from __future__ import annotations

import logging
from typing import Any, Dict, Iterable, List, Sequence

try:  
    import requests
except ImportError as import_error:  
    requests = None 
    _IMPORT_ERROR = import_error
else:
    _IMPORT_ERROR = None

from ..common.exceptions import EmbeddingProviderError
from .basic_embedding import BasicEmbedding

logger = logging.getLogger(__name__)


class DeepSeekEmbedding(BasicEmbedding):
    """Embedding provider backed by DeepSeek's embeddings API."""

    _DEFAULT_ENDPOINT = "https://api.deepseek.com/v1/embeddings"

    def __init__(
        self,
        api_key: str,
        model: str = "deepseek-embedding",
        *,
        endpoint: str | None = None,
        timeout: float | tuple[float, float] | None = None,
        headers: Dict[str, str] | None = None,
        request_options: Dict[str, Any] | None = None,
    ) -> None:
        if not api_key:
            raise ValueError("api_key is required to use DeepSeekEmbedding")
        if requests is None:  
            raise ImportError("requests is required to use DeepSeekEmbedding") from _IMPORT_ERROR

        super().__init__(model=model)

        self._endpoint = endpoint or self._DEFAULT_ENDPOINT
        self._timeout = timeout
        self._request_options = request_options.copy() if request_options else {}

        default_headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        }
        if headers:
            default_headers.update(headers)

        self._headers = default_headers

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
            response = requests.post(
                self._endpoint,
                headers=self._headers,
                json=payload,
                timeout=self._timeout,
            )
            response.raise_for_status()
        except requests.exceptions.RequestException as exc:  
            message = "Failed to retrieve embeddings from DeepSeek provider"
            logger.exception(message)
            raise EmbeddingProviderError(message, original_exception=exc) from exc

        try:
            content = response.json()
        except ValueError as exc:
            message = "Failed to parse DeepSeek embedding response body"
            logger.exception(message)
            raise EmbeddingProviderError(message, original_exception=exc) from exc

        vectors = self._extract_embeddings(content)
        if not vectors:
            raise EmbeddingProviderError("DeepSeek response did not contain embedding vectors")

        return [self._coerce_vector(vector) for vector in vectors]

    @staticmethod
    def _extract_embeddings(payload: Any) -> Iterable[Sequence[Any]] | None:
        if payload is None:
            return None

        data_field = None
        if isinstance(payload, dict):
            data_field = payload.get("data") or payload.get("embeddings")
        elif isinstance(payload, list):
            data_field = payload

        if isinstance(data_field, list) and data_field:
            embeddings: List[Sequence[Any]] = []
            for item in data_field:
                if isinstance(item, dict):
                    candidate = item.get("embedding") or item.get("vector")
                    if candidate is None:
                        return None
                    embeddings.append(candidate)
                elif isinstance(item, (list, tuple)):
                    embeddings.append(item)
                else:
                    return None
            return embeddings

        return None

    @staticmethod
    def _coerce_vector(vector: Sequence[Any]) -> List[float]:
        try:
            return [float(component) for component in vector]
        except (TypeError, ValueError) as exc: 
            raise EmbeddingProviderError(
                "DeepSeek embedding vector contained non-numeric values",
                original_exception=exc,
            ) from exc


__all__ = ["DeepSeekEmbedding"]
