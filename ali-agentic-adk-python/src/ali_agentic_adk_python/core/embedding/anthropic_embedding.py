from __future__ import annotations

import logging
from typing import Any, Dict, Iterable, List, Sequence

try:  
    import anthropic
except ImportError as import_error:  
    anthropic = None 
    _IMPORT_ERROR = import_error
else:
    _IMPORT_ERROR = None

from ..common.exceptions import EmbeddingProviderError
from .basic_embedding import BasicEmbedding

logger = logging.getLogger(__name__)


class AnthropicEmbedding(BasicEmbedding):
    """Embedding provider backed by Anthropic's embeddings API."""

    def __init__(
        self,
        api_key: str,
        model: str = "claude-embed-v1",
        *,
        base_url: str | None = None,
        timeout: float | tuple[float, float] | None = None,
        client_options: Dict[str, Any] | None = None,
        request_options: Dict[str, Any] | None = None,
    ) -> None:
        if not api_key:
            raise ValueError("api_key is required to use AnthropicEmbedding")
        if anthropic is None:  # pragma: no cover - import guard
            raise ImportError("anthropic is required to use AnthropicEmbedding") from _IMPORT_ERROR

        super().__init__(model=model)

        client_kwargs: Dict[str, Any] = {"api_key": api_key}
        if base_url:
            client_kwargs["base_url"] = base_url
        if timeout is not None:
            client_kwargs["timeout"] = timeout
        if client_options:
            client_kwargs.update(client_options)

        self._client = anthropic.Anthropic(**client_kwargs)
        self._request_options = request_options.copy() if request_options else {}

    def embed_documents(self, texts: Sequence[str]) -> List[List[float]]:
        normalized_inputs = self._normalize_inputs(texts)
        if not normalized_inputs:
            return []

        payload: Dict[str, Any] = {
            "model": self.model,
            "input": normalized_inputs,
        }
        if self._request_options:
            payload.update(self._request_options)

        try:
            response = self._client.embeddings.create(**payload)
        except Exception as exc:  
            message = "Failed to retrieve embeddings from Anthropic provider"
            logger.exception(message)
            raise EmbeddingProviderError(message, original_exception=exc) from exc

        vectors = self._extract_embeddings(response)
        if not vectors:
            raise EmbeddingProviderError("Anthropic response did not contain embedding vectors")

        return [self._coerce_vector(vector) for vector in vectors]

    @staticmethod
    def _extract_embeddings(response: Any) -> Iterable[Sequence[Any]] | None:
        if response is None:
            return None

        if isinstance(response, dict):
            data = response.get("data") or response.get("embeddings")
            if isinstance(data, list) and data:
                return AnthropicEmbedding._collect_from_iterable(data)
            return None

        data = getattr(response, "data", None)
        if isinstance(data, list) and data:
            return AnthropicEmbedding._collect_from_iterable(data)

        return None

    @staticmethod
    def _collect_from_iterable(data: Iterable[Any]) -> Iterable[Sequence[Any]] | None:
        collected: List[Sequence[Any]] = []
        for item in data:
            if hasattr(item, "embedding"):
                vector = getattr(item, "embedding")
            elif isinstance(item, dict):
                vector = item.get("embedding") or item.get("vector")
            else:
                vector = item

            if vector is None:
                return None
            collected.append(vector)
        return collected

    @staticmethod
    def _coerce_vector(vector: Sequence[Any]) -> List[float]:
        try:
            return [float(component) for component in vector]
        except (TypeError, ValueError) as exc: 
            raise EmbeddingProviderError(
                "Anthropic embedding vector contained non-numeric values",
                original_exception=exc,
            ) from exc


__all__ = ["AnthropicEmbedding"]
