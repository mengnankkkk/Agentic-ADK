# Copyright (C) 2025 AIDC-AI
# Apache License, Version 2.0
#
"""MCP tool adapter for google-adk."""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from typing import Any, Dict, Optional, TYPE_CHECKING

import mcp.types as mcp_types
from google.adk.tools import BaseTool, ToolContext
from google.genai import types as genai_types
from pydantic import BaseModel

if TYPE_CHECKING: 
    from .connection import McpConnection

logger = logging.getLogger(__name__)


@dataclass(slots=True)
class McpToolDescriptor:
    """Metadata describing how to invoke an MCP tool."""
    exposed_name: str
    original_name: str
    connection: "McpConnection"
    tool: mcp_types.Tool
    server_info: mcp_types.Implementation | None = None
    extra_metadata: Dict[str, Any] = field(default_factory=dict)


class McpTool(BaseTool):
    """google-adk compatible wrapper around an MCP tool."""
    def __init__(self, descriptor: McpToolDescriptor) -> None:
        description = descriptor.tool.description or (
            descriptor.tool.annotations.title if descriptor.tool.annotations else ""
        )
        super().__init__(
            name=descriptor.exposed_name,
            description=description or f"MCP tool {descriptor.original_name}",
        )
        self._descriptor = descriptor
        self.custom_metadata = self._build_metadata()

    def _get_declaration(self) -> Optional[genai_types.FunctionDeclaration]:
        tool = self._descriptor.tool
        return genai_types.FunctionDeclaration(
            name=self.name,
            description=tool.description,
            parameters_json_schema=tool.inputSchema,
            response_json_schema=tool.outputSchema,
        )

    async def run_async(
        self,
        *,
        args: dict[str, Any],
        tool_context: ToolContext,
    ) -> dict[str, Any]:
        try:
            result = await self._descriptor.connection.call_tool(self._descriptor.original_name, args)
        except Exception:
            logger.exception(
                "Failed to call MCP tool '%s' (namespace=%s)",
                self._descriptor.original_name,
                self.custom_metadata.get("namespace"),
            )
            raise

        return _serialize_call_tool_result(result, self._descriptor)

    def _build_metadata(self) -> dict[str, Any]:
        metadata: dict[str, Any] = {
            "tool_name": self._descriptor.original_name,
        }

        namespace = self._descriptor.connection.namespace
        if namespace:
            metadata["namespace"] = namespace

        if self._descriptor.server_info:
            metadata["server_name"] = self._descriptor.server_info.name
            metadata["server_version"] = self._descriptor.server_info.version
            if self._descriptor.server_info.title:
                metadata["server_title"] = self._descriptor.server_info.title
            if self._descriptor.server_info.websiteUrl:
                metadata["server_website"] = str(self._descriptor.server_info.websiteUrl)

        if self._descriptor.extra_metadata:
            metadata.update(self._descriptor.extra_metadata)

        return metadata


def _serialize_call_tool_result(
    result: mcp_types.CallToolResult,
    descriptor: McpToolDescriptor,
) -> dict[str, Any]:
    payload: dict[str, Any] = {
        "is_error": result.isError,
        "metadata": {
            "tool_name": descriptor.original_name,
            "exposed_name": descriptor.exposed_name,
            "namespace": descriptor.connection.namespace,
        },
    }

    if descriptor.server_info:
        payload["metadata"].update(
            {
                "server_name": descriptor.server_info.name,
                "server_version": descriptor.server_info.version,
            }
        )
        if descriptor.server_info.title:
            payload["metadata"]["server_title"] = descriptor.server_info.title

    if result.structuredContent is not None:
        payload["structured_content"] = result.structuredContent

    if result.content:
        payload["content"] = [_dump_pydantic_model(block) for block in result.content]
        # for convenience: surface the first text entry for ease of use.
        first_text = next(
            (block for block in result.content if isinstance(block, mcp_types.TextContent)),
            None,
        )
        if first_text:
            payload["text"] = first_text.text

    return payload


def _dump_pydantic_model(model: mcp_types.ContentBlock | BaseModel | dict[str, Any]) -> dict[str, Any]:
    if isinstance(model, BaseModel):
        return model.model_dump(mode="json", exclude_none=True)
    if isinstance(model, dict):
        return model
    raise TypeError(f"Unexpected content block type: {type(model)!r}")
