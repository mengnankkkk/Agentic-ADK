from __future__ import annotations

from typing import AsyncGenerator, Any

from google.adk.models import LlmResponse
from google.genai import types
from google.genai.types import FunctionCall


class CerebrasUtils:
    @staticmethod
    def to_llm_response(result: Any) -> LlmResponse:
        """Convert a Cerebras ChatCompletion (non-stream) to LlmResponse.

        The `result` is expected to be an instance of `ChatCompletionResponse`
        (from cerebras.cloud.sdk.types.chat.chat_completion), but we avoid direct
        typing/imports here to keep this file import-light.
        """
        llm_response = LlmResponse()
        if result is None:
            return llm_response

        # id, usage
        if getattr(result, "id", None):
            llm_response.id = getattr(result, "id")
        usage = getattr(result, "usage", None)
        if usage is not None:
            prompt_tokens = getattr(usage, "prompt_tokens", None)
            completion_tokens = getattr(usage, "completion_tokens", None)
            total_tokens = getattr(usage, "total_tokens", None)
            if any(v is not None for v in (prompt_tokens, completion_tokens, total_tokens)):
                llm_response.usage_metadata = types.GenerateContentResponseUsageMetadata(
                    prompt_token_count=prompt_tokens or 0,
                    candidates_token_count=completion_tokens or 0,
                    total_token_count=total_tokens or 0,
                )

        # choices[0]
        choices = getattr(result, "choices", None) or []
        if choices:
            choice0 = choices[0]
            message = getattr(choice0, "message", None)
            finish_reason = getattr(choice0, "finish_reason", None)
            # text
            if message is not None and getattr(message, "content", None):
                llm_response.content = types.Content(
                    role=getattr(message, "role", "assistant"),
                    parts=[types.Part(text=getattr(message, "content"))],
                )
            # tools
            tool_calls = getattr(message, "tool_calls", None)
            if tool_calls:
                parts = []
                for tc in tool_calls:
                    fn = getattr(tc, "function", None)
                    if fn is None:
                        continue
                    name = getattr(fn, "name", "")
                    args_str = getattr(fn, "arguments", "{}")
                    try:
                        import json
                        args_val = json.loads(args_str) if isinstance(args_str, str) else args_str
                    except Exception:
                        args_val = {}
                    parts.append(types.Part(function_call=FunctionCall(name=name, args=args_val, id=getattr(tc, "id", None))))
                if parts:
                    llm_response.content = types.Content(role="assistant", parts=parts)

            # partial or final
            if finish_reason in ("stop", "tool_calls"):
                llm_response.partial = False
            else:
                llm_response.partial = True

        return llm_response

    @staticmethod
    async def to_llm_response_stream(stream: Any) -> AsyncGenerator[LlmResponse, None]:
        """Convert a Cerebras streaming iterator to async generator of LlmResponse.

        We expect `stream` to be an iterator over ChatChunkResponse; we yield text/tool
        deltas as partial chunks and emit a final LlmResponse with usage if present.
        """
        import json

        acc_text = ""
        fn_calls = {}
        usage_meta = None
        final_emitted = False

        for chunk in stream:
            if chunk is None:
                continue
            choices = getattr(chunk, "choices", None) or []
            if choices:
                ch0 = choices[0]
                delta = getattr(ch0, "delta", None)
                finish = getattr(ch0, "finish_reason", None)

                # text delta
                if delta is not None and getattr(delta, "content", None):
                    text_piece = getattr(delta, "content")
                    acc_text += text_piece
                    yield LlmResponse(
                        content=types.Content(role="assistant", parts=[types.Part(text=text_piece)]),
                        partial=True,
                    )

                # tool delta
                tool_calls = getattr(delta, "tool_calls", None)
                if tool_calls:
                    for tc in tool_calls:
                        idx = getattr(tc, "index", 0) or 0
                        fn = getattr(tc, "function", None)
                        if fn is None:
                            continue
                        name = getattr(fn, "name", None)
                        args_piece = getattr(fn, "arguments", None)
                        if idx not in fn_calls:
                            fn_calls[idx] = {"name": "", "args": "", "id": getattr(tc, "id", None)}
                        if name:
                            fn_calls[idx]["name"] = name
                        if args_piece:
                            fn_calls[idx]["args"] += args_piece

                # usage only in final chunk generally
                usage = getattr(chunk, "usage", None)
                if usage is not None:
                    usage_meta = types.GenerateContentResponseUsageMetadata(
                        prompt_token_count=getattr(usage, "prompt_tokens", 0) or 0,
                        candidates_token_count=getattr(usage, "completion_tokens", 0) or 0,
                        total_token_count=getattr(usage, "total_tokens", 0) or 0,
                    )

                # finalize when finish reason present
                if finish in ("tool_calls", "stop"):
                    if fn_calls:
                        parts = []
                        for i in sorted(fn_calls.keys()):
                            call = fn_calls[i]
                            args_val = {}
                            try:
                                args_val = json.loads(call["args"]) if call["args"] else {}
                            except Exception:
                                args_val = {}
                            parts.append(types.Part(function_call=FunctionCall(name=call.get("name", ""), args=args_val, id=call.get("id"))))
                        yield LlmResponse(content=types.Content(role="assistant", parts=parts), partial=False, usage_metadata=usage_meta)
                        final_emitted = True
                        fn_calls.clear()
                        acc_text = ""
                    elif acc_text:
                        yield LlmResponse(content=types.Content(role="assistant", parts=[types.Part(text=acc_text)]), partial=False, usage_metadata=usage_meta)
                        final_emitted = True
                        acc_text = ""

        if not final_emitted and (acc_text or usage_meta is not None):
            yield LlmResponse(content=types.Content(role="assistant", parts=[types.Part(text=acc_text)]) if acc_text else None, partial=False, usage_metadata=usage_meta)

