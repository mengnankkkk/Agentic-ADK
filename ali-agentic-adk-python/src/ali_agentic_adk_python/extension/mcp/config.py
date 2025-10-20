# Copyright (C) 2025 AIDC-AI
# Apache License, Version 2.0
#
"""Configuration models for MCP server connections."""

from __future__ import annotations

from enum import Enum
from typing import Annotated, Literal, Union

from pydantic import BaseModel, ConfigDict, Field, HttpUrl


class McpServerTransport(str, Enum):
    """supported transport types for connecting to an MCP server."""
    STDIO = "stdio"
    SSE = "sse"


class _BaseServerConfig(BaseModel):
    """shared options for an MCP server connection."""
    namespace: str | None = Field(
        default=None,
        description=(
            "Optional namespace that will be prepended to tool names published "
            "by this server. If omitted, the server-reported name will be used "
            "to avoid cross-server collisions."
        ),
    )

    model_config = ConfigDict(extra="forbid")


class McpStdioServerConfig(_BaseServerConfig):
    """configuration required to spawn a server via stdio."""

    transport: Literal[McpServerTransport.STDIO] = McpServerTransport.STDIO
    command: str = Field(
        default="uvx",
        description=(
            "Executable used to launch the MCP server. Defaults to 'uvx', "
            "which can resolve uv-based MCP servers."
        ),
    )
    args: list[str] = Field(
        default_factory=list,
        description="Command-line arguments passed to the executable.",
    )
    env: dict[str, str] | None = Field(
        default=None,
        description="Optional environment variables for the spawned process.",
    )
    cwd: str | None = Field(
        default=None,
        description="Optional working directory for the spawned process",
    )
    encoding: str = Field(
        default="utf-8",
        description="Text encoding used for stdio communicaion",
    )
    encoding_error_handler: Literal["strict", "ignore", "replace"] = Field(
        default="strict",
        description="Error handling strategy applied while decoding stdio",
    )


class McpSseServerConfig(_BaseServerConfig):
    """Configuration required to connect to a server via SSE."""
    transport: Literal[McpServerTransport.SSE] = McpServerTransport.SSE
    url: HttpUrl = Field(description="SSE endpoint exposed by the MCP server.")
    headers: dict[str, str] | None = Field(
        default=None,
        description="Optional additional headers sent with SSE requests.",
    )
    timeout: float = Field(
        default=5.0,
        ge=0.1,
        description="HTTP timeout (seconds) for standard requests.",
    )
    sse_read_timeout: float = Field(
        default=300.0,
        ge=1.0,
        description=(
            "Maximum time (seconds) to wait for SSE messages before disconnecting."
        ),
    )


McpServerConfig = Annotated[
    Union[McpStdioServerConfig, McpSseServerConfig],
    Field(discriminator="transport"),
]
