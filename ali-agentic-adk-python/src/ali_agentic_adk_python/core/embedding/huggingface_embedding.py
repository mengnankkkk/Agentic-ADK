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


class HuggingFaceEmbedding(BasicEmbedding):
    """Embedding provider backed by Hugging Face Inference API."""

    _DEFAULT_BASE_URL = "https://api-inference.huggingface.co/models"

    def __init__(
        self,
        api_key: str,
        model: str = "sentence-transformers/all-MiniLM-L6-v2",
        *,
        base_url: str | None = None,
        timeout: float | tuple[float, float] | None = None,
        headers: Dict[str, str] | None = None,
        request_options: Dict[str, Any] | None = None,
    ) -> None:
        if not api_key:
            raise ValueError("api_key is required to use HuggingFaceEmbedding")
        if requests is None:  
            raise ImportError("requests is required to use HuggingFaceEmbedding") from _IMPORT_ERROR

        super().__init__(model=model)

        self._base_url = (base_url or self._DEFAULT_BASE_URL).rstrip("/")
        self._timeout = timeout
        self._request_options = request_options.copy() if request_options else {}

        auth_header = {"Authorization": f"Bearer {api_key}", "Accept": "application/json"}
        self._headers: Dict[str, str] = auth_header
        if headers:
            self._headers.update(headers)

    def embed_documents(self, texts: Sequence[str]) -> List[List[float]]:
        normalized_inputs = self._normalize_inputs(texts)
        if not normalized_inputs:
            return []

        payload: Dict[str, Any] = {"inputs": normalized_inputs}
        if self._request_options:
            payload.update(self._request_options)

        url = f"{self._base_url}/{self.model}"
        try:
            response = requests.post(
                url,
                headers=self._headers,
                json=payload,
                timeout=self._timeout,
            )
            response.raise_for_status()
        except requests.exceptions.RequestException as exc:  
            message = "Failed to retrieve embeddings from Hugging Face provider"
            logger.exception(message)
            raise EmbeddingProviderError(message, original_exception=exc) from exc

        try:
            content = response.json()
        except ValueError as exc:
            message = "Failed to parse Hugging Face embedding response body"
            logger.exception(message)
            raise EmbeddingProviderError(message, original_exception=exc) from exc

        vectors = self._extract_embeddings(content)
        if not vectors:
            raise EmbeddingProviderError("Hugging Face response did not contain embedding vectors")

        return [self._coerce_vector(vector) for vector in vectors]

    @staticmethod
    def _extract_embeddings(payload: Any) -> Iterable[Sequence[Any]] | None:
        if payload is None:
            return None

        if isinstance(payload, list):
            if not payload:
                return None
            first_item = payload[0]
            if isinstance(first_item, (list, tuple)):
                # already a batch of embedding vectors
                return payload  
            if isinstance(first_item, dict):
                embeddings = []
                for item in payload:
                    if isinstance(item, dict):
                        candidate = item.get("embedding") or item.get("vector")
                        if candidate is None:
                            return None
                        embeddings.append(candidate)
                    else:
                        return None
                return embeddings
            if isinstance(first_item, (int, float)):
                return [payload]

        if isinstance(payload, dict):
            vectors = payload.get("embeddings") or payload.get("data")
            if isinstance(vectors, list) and vectors:
                return vectors

        return None

    @staticmethod
    def _coerce_vector(vector: Sequence[Any]) -> List[float]:
        try:
            return [float(component) for component in vector]
        except (TypeError, ValueError) as exc:  
            raise EmbeddingProviderError(
                "Hugging Face embedding vector contained non-numeric values",
                original_exception=exc,
            ) from exc


__all__ = ["HuggingFaceEmbedding"]
