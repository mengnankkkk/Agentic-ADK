from __future__ import annotations

from typing import AsyncGenerator

from google.adk.models import LlmRequest, LlmResponse, BaseLlm

from ali_agentic_adk_python.core.utils.cerebras_utils import CerebrasUtils
from ali_agentic_adk_python.core.utils.dashscope_message_convert_utils import DashscopeMessageConverter


class CerebrasLLM(BaseLlm):
    """Cerebras Cloud adapter for Google ADK BaseLlm.

    This wrapper prefers using the official cerebras_cloud_sdk if available. It
    does not add the dependency to project requirements by default to avoid
    conflicts. If the SDK is not installed, an ImportError will be raised when
    invoking.
    """

    def __init__(self, api_key: str | None = None, *, base_url: str | None = None, model: str = "llama3.1-8b"):
        super().__init__(model=model)
        self._api_key = api_key
        self._base_url = base_url
        self._client = None

    @classmethod
    def from_env(cls, *, model: str = "llama3.1-8b") -> "CerebrasLLM":
        import os
        return cls(api_key=os.environ.get("CEREBRAS_API_KEY"), base_url=os.environ.get("CEREBRAS_BASE_URL"), model=model)

    def _ensure_client(self):
        if self._client is not None:
            return
        try:
            from cerebras.cloud.sdk import Cerebras
        except Exception as e:
            raise ImportError(
                "cerebras_cloud_sdk is required for CerebrasLLM. Install with `pip install cerebras_cloud_sdk`"
            ) from e
        self._client = Cerebras(api_key=self._api_key, base_url=self._base_url) if (self._api_key or self._base_url) else Cerebras()

    async def generate_content_async(self, llm_request: LlmRequest, stream: bool = False) -> AsyncGenerator[LlmResponse, None]:
        async for r in self._invoke(llm_request, stream):
            yield r

    async def _invoke(self, request: LlmRequest, stream: bool) -> AsyncGenerator[LlmResponse, None]:
        self._ensure_client()
        # map request to Cerebras chat params
        messages = DashscopeMessageConverter.to_qwen_messages(request)
        tools = DashscopeMessageConverter.to_qwen_tools(request)

        if stream:
            stream_obj = self._client.chat.completions.create(
                messages=messages,
                model=request.model,
                tools=tools,
                stream=True,
            )
            async for resp in CerebrasUtils.to_llm_response_stream(stream_obj):
                yield resp
        else:
            result = self._client.chat.completions.create(
                messages=messages,
                model=request.model,
                tools=tools,
                stream=False,
            )
            yield CerebrasUtils.to_llm_response(result)

    def get_api_key(self):
        return self._api_key

