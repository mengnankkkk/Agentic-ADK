# Copyright (C) 2025 AIDC-AI
# Apache License, Version 2.0
#
"""

MCP integration for Agentic ADK.

This extension exposes helper classes that make it straightforward to
connect to MCP-compatible servers and surface their tools through
google-adk's ``BaseTool`` abstraction.

Typical usage::

    from ali_agentic_adk_python.extension.mcp import (
        McpSessionManager,
        McpStdioServerConfig,
    )

    manager = McpSessionManager(
        servers=[
            McpStdioServerConfig(
                namespace="filesystem",
                command="uvx",
                args=["modelcontextprotocol/server-filesystem"],
            )
        ]
    )

    await manager.start()
    mcp_tools = manager.build_google_adk_tools()

The resulting ``mcp_tools`` can then be registered on an Agent just like
any other google-adk tool.
"""

from .config import (
    McpServerConfig,
    McpServerTransport,
    McpSseServerConfig,
    McpStdioServerConfig,
)
from .connection import McpSessionManager
from .tool import McpTool

__all__ = [
    "McpServerConfig",
    "McpServerTransport",
    "McpSseServerConfig",
    "McpStdioServerConfig",
    "McpSessionManager",
    "McpTool",
]
