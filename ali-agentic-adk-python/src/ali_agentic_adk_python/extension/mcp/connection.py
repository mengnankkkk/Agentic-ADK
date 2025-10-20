# Copyright (C) 2025 AIDC-AI
# Apache License, Version 2.0
#
"""Connection and session management for MCP servers."""

from __future__ import annotations

import asyncio
import logging
from collections.abc import Iterable, Sequence
from contextlib import AsyncExitStack
from typing import Dict

from mcp import types as mcp_types
from mcp.client.session import ClientSession
from mcp.client.sse import sse_client
from mcp.client.stdio import StdioServerParameters, stdio_client

from .config import (
    McpServerConfig,
    McpSseServerConfig,
    McpStdioServerConfig,
)
from .tool import McpToolDescriptor

logger = logging.getLogger(__name__)


class McpConnection:
    """manage the lifecycle of a single MCP server connection."""
    def __init__(self, config: McpServerConfig) -> None:
        self._config = config
        self._stack = AsyncExitStack()
        self._stack_entered = False
        self._session: ClientSession | None = None
        self._server_info: mcp_types.Implementation | None = None
        self._tools: Dict[str, mcp_types.Tool] = {}
        self._call_lock = asyncio.Lock()
        self._connected = False

    @property
    def config(self) -> McpServerConfig:
        return self._config

    @property
    def server_info(self) -> mcp_types.Implementation | None:
        return self._server_info

    @property
    def namespace(self) -> str | None:
        if self._config.namespace:
            return self._config.namespace
        if self._server_info:
            return self._server_info.name
        return None

    @property
    def is_connected(self) -> bool:
        return self._connected

    @property
    def tools(self) -> Dict[str, mcp_types.Tool]:
        """returns a copy of the currently known tools."""
        return dict(self._tools)

    async def connect(self) -> None:
        """Establish the connection to the MCP server if not already connected."""
        if self._connected:
            return

        try:
            if not self._stack_entered:
                await self._stack.__aenter__()
                self._stack_entered = True

            read_stream, write_stream = await self._open_transport()
            session = ClientSession(read_stream, write_stream)
            self._session = await self._stack.enter_async_context(session)
            init_result = await self._session.initialize()
            self._server_info = init_result.serverInfo
            await self.refresh_tools()
            self._connected = True
            logger.debug(
                "Connected to MCP server %s (version=%s)",
                self.namespace or "<anonymous>",
                self._server_info.version if self._server_info else "unknown",
            )
        except Exception:
            await self.close()
            raise

    async def close(self) -> None:
        """Tear down the connection (if active)."""
        if self._stack_entered:
            await self._stack.aclose()
            self._stack_entered = False
        self._session = None
        self._server_info = None
        self._tools = {}
        self._connected = False

    async def refresh_tools(self) -> Dict[str, mcp_types.Tool]:
        """Fetch the latest tool metadata from the server"""
        session = await self._require_session()
        async with self._call_lock:
            response = await session.list_tools()
        self._tools = {tool.name: tool for tool in response.tools}
        return self.tools

    async def call_tool(self, tool_name: str, args: dict[str, object]) -> mcp_types.CallToolResult:
        """Invoke a tool exposed by this server"""
        session = await self._require_session()
        async with self._call_lock:
            return await session.call_tool(tool_name, args)

    async def _open_transport(self):
        """Open the configured transport and return read/write streams."""
        if isinstance(self._config, McpStdioServerConfig):
            params = StdioServerParameters(
                command=self._config.command,
                args=self._config.args,
                env=self._config.env,
                cwd=self._config.cwd,
                encoding=self._config.encoding,
                encoding_error_handler=self._config.encoding_error_handler,
            )
            return await self._stack.enter_async_context(stdio_client(params))

        if isinstance(self._config, McpSseServerConfig):
            return await self._stack.enter_async_context(
                sse_client(
                    url=str(self._config.url),
                    headers=self._config.headers,
                    timeout=self._config.timeout,
                    sse_read_timeout=self._config.sse_read_timeout,
                )
            )

        raise ValueError(f"Unsupported MCP transport: {self._config.transport!r}")

    async def _require_session(self) -> ClientSession:
        await self.connect()
        if self._session is None:
            raise RuntimeError("MCP session is not available after attempting to connect.")
        return self._session


class McpSessionManager:
    """Aggregates multiple MCP connections and exposes their tools."""
    def __init__(
        self,
        servers: Sequence[McpServerConfig] | None = None,
        *,
        name_separator: str = ".",
        auto_connect: bool = False,
    ) -> None:
        self._connections = [McpConnection(cfg) for cfg in (servers or ())]
        self._name_separator = name_separator
        self._tool_registry: Dict[str, McpToolDescriptor] = {}
        self._lock = asyncio.Lock()
        self._started = False

        if auto_connect:
            asyncio.get_event_loop().create_task(self.start())

    async def start(self) -> None:
        async with self._lock:
            if self._started:
                return

            try:
                for connection in self._connections:
                    await connection.connect()
                self._rebuild_registry()
                self._started = True
            except Exception:
                await self._shutdown_connections()
                raise

    async def stop(self) -> None:
        async with self._lock:
            await self._shutdown_connections()
            self._tool_registry.clear()
            self._started = False

    async def ensure_started(self) -> None:
        if not self._started:
            await self.start()

    @property
    def descriptors(self) -> Dict[str, McpToolDescriptor]:
        return dict(self._tool_registry)

    async def refresh(self) -> None:
        """Refresh tool metadata across all connected servers."""
        async with self._lock:
            for connection in self._connections:
                await connection.refresh_tools()
            self._rebuild_registry()

    def add_server(self, config: McpServerConfig) -> None:
        """Register a new MCP server (connection established on next start/refresh)."""
        self._connections.append(McpConnection(config))
        self._started = False

    def build_google_adk_tools(self) -> list["McpTool"]:
        from .tool import McpTool

        return [McpTool(descriptor) for descriptor in self._tool_registry.values()]

    def _rebuild_registry(self) -> None:
        registry: Dict[str, McpToolDescriptor] = {}
        taken_names: set[str] = set()

        for connection in self._connections:
            namespace = connection.namespace
            for original_name, tool in connection.tools.items():
                base_name = self._compose_name(namespace, original_name)
                unique_name = self._dedupe_name(base_name, taken_names)
                descriptor = McpToolDescriptor(
                    exposed_name=unique_name,
                    original_name=original_name,
                    connection=connection,
                    tool=tool,
                    server_info=connection.server_info,
                )
                registry[unique_name] = descriptor
                taken_names.add(unique_name)

        self._tool_registry = registry

    def _compose_name(self, namespace: str | None, tool_name: str) -> str:
        if namespace:
            return f"{namespace}{self._name_separator}{tool_name}"
        return tool_name

    def _dedupe_name(self, candidate: str, taken: set[str]) -> str:
        if candidate not in taken:
            return candidate

        suffix = 1
        while True:
            updated = f"{candidate}#{suffix}"
            if updated not in taken:
                return updated
            suffix += 1

    async def _shutdown_connections(self) -> None:
        for connection in self._connections:
            try:
                await connection.close()
            except Exception:
                logger.exception("Failed to close MCP connection cleanly.")


# avoid circular import 
from typing import TYPE_CHECKING

if TYPE_CHECKING: 
    from .tool import McpTool
