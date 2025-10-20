from __future__ import annotations

import logging
from typing import Any, Dict, List, Sequence

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


class SiliconFlowEmbedding(BasicEmbedding):
    """Embedding provider backed by SiliconFlow embeddings REST API."""

    _DEFAULT_BASE_URL = "https://api.siliconflow.cn/v1"

    def __init__(
        self,
        api_key: str,
        model: str = "internlm2.5-embedding",
        *,
        base_url: str | None = None,
        timeout: float | tuple[float, float] | None = None,
        headers: Dict[str, str] | None = None,
        request_options: Dict[str, Any] | None = None,
    ) -> None:
        if not api_key:
            raise ValueError("api_key is required to use SiliconFlowEmbedding")
        if requests is None:
            raise ImportError("requests is required to use SiliconFlowEmbedding") from _IMPORT_ERROR

        super().__init__(model=model)

        self._base_url = (base_url or self._DEFAULT_BASE_URL).rstrip("/")
        self._timeout = timeout
        self._request_options = request_options.copy() if request_options else {}

        default_headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        }
        if headers:
            default_headers.update(headers)
        self._headers = default_headers

    def embed_documents(self, texts: Sequence[str]) -> List[List[float]]:
        normalized_inputs = self._normalize_inputs(texts)
        if not normalized_inputs:
            return []

        payload: Dict[str, Any] = {"model": self.model, "input": normalized_inputs}
        if self._request_options:
            payload.update(self._request_options)

        url = f"{self._base_url}/embeddings"
        try:
            response = requests.post(
                url,
                headers=self._headers,
                json=payload,
                timeout=self._timeout,
            )
            response.raise_for_status()
        except requests.exceptions.RequestException as exc:
            message = "Failed to retrieve embeddings from SiliconFlow provider"
            logger.exception(message)
            raise EmbeddingProviderError(message, original_exception=exc) from exc

        try:
            content = response.json()
        except ValueError as exc:
            message = "Failed to parse SiliconFlow embedding response body"
            logger.exception(message)
            raise EmbeddingProviderError(message, original_exception=exc) from exc

        vectors = self._extract_vectors(content)
        if not vectors:
            raise EmbeddingProviderError("SiliconFlow response did not contain embedding vectors")

        return [self._coerce_vector(vector) for vector in vectors]

    @staticmethod
    def _extract_vectors(payload: Any) -> Sequence[Sequence[Any]] | None:
        if not isinstance(payload, dict):
            return None

        data = payload.get("data")
        if not isinstance(data, list):
            return None

        vectors: List[Sequence[Any]] = []
        for item in data:
            if not isinstance(item, dict):
                return None
            vector = item.get("embedding") or item.get("vector")
            if vector is None:
                return None
            vectors.append(vector)

        return vectors

    @staticmethod
    def _coerce_vector(vector: Sequence[Any]) -> List[float]:
        try:
            return [float(component) for component in vector]
        except (TypeError, ValueError) as exc:
            raise EmbeddingProviderError(
                "SiliconFlow embedding vector contained non-numeric values",
                original_exception=exc,
            ) from exc


__all__ = ["SiliconFlowEmbedding"]
