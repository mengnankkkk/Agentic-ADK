import asyncio
from unittest.mock import MagicMock

import pytest
from mcp import types as mcp_types

from ali_agentic_adk_python.extension.mcp.connection import McpSessionManager
from ali_agentic_adk_python.extension.mcp.tool import McpTool, McpToolDescriptor


class _StubConnection:
    def __init__(self, namespace: str | None = None, result: mcp_types.CallToolResult | None = None):
        self._namespace = namespace
        self._result = result
        self.called_with = None

    @property
    def namespace(self):
        return self._namespace

    async def call_tool(self, tool_name: str, args: dict[str, object]):
        self.called_with = (tool_name, args)
        return self._result


class _FakeConnection:
    def __init__(self, namespace: str | None, tools: dict[str, mcp_types.Tool], server: str):
        self._namespace = namespace
        self._tools = tools
        self.server_info = mcp_types.Implementation(name=server, version="1.0.0")

    @property
    def namespace(self):
        return self._namespace

    @property
    def tools(self):
        return self._tools


def test_mcp_tool_run_async_returns_serialized_payload():
    call_result = mcp_types.CallToolResult(
        content=[mcp_types.TextContent(type="text", text="pong")],
        structuredContent={"status": "ok"},
        isError=False,
    )
    connection = _StubConnection(namespace="demo", result=call_result)
    descriptor = McpToolDescriptor(
        exposed_name="demo.ping",
        original_name="ping",
        connection=connection,
        tool=mcp_types.Tool(name="ping", inputSchema={"type": "object", "properties": {}}),
        server_info=mcp_types.Implementation(name="stub", version="0.1.0"),
    )

    tool = McpTool(descriptor)

    async def _invoke():
        return await tool.run_async(args={"echo": True}, tool_context=MagicMock())

    payload = asyncio.run(_invoke())

    assert payload["structured_content"] == {"status": "ok"}
    assert payload["text"] == "pong"
    assert payload["metadata"]["namespace"] == "demo"
    assert connection.called_with == ("ping", {"echo": True})


def test_mcp_session_manager_deduplicates_tool_names():
    manager = McpSessionManager([])
    tool = mcp_types.Tool(name="echo", inputSchema={"type": "object", "properties": {}})

    manager._connections = [
        _FakeConnection(None, {"echo": tool}, "server-a"),
        _FakeConnection(None, {"echo": tool}, "server-b"),
    ]

    manager._rebuild_registry()
    registry = manager.descriptors

    assert "echo" in registry
    assert "echo#1" in registry


def test_mcp_session_manager_applies_namespace_prefix():
    manager = McpSessionManager([])
    tool = mcp_types.Tool(name="search", inputSchema={"type": "object", "properties": {}})

    manager._connections = [
        _FakeConnection("alpha", {"search": tool}, "server-alpha"),
    ]

    manager._rebuild_registry()
    registry = manager.descriptors

    assert list(registry.keys()) == ["alpha.search"]
