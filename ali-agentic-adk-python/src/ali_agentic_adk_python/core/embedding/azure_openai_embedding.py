from __future__ import annotations

import logging
from typing import Any, Dict, List, Sequence

try:
    from openai import AzureOpenAI
except ImportError as import_error:
    AzureOpenAI = None
    _IMPORT_ERROR = import_error
else:
    _IMPORT_ERROR = None

from ..common.exceptions import EmbeddingProviderError
from .basic_embedding import BasicEmbedding

logger = logging.getLogger(__name__)


class AzureOpenAIEmbedding(BasicEmbedding):
    """Embedding provider backed by the Azure OpenAI embeddings API."""

    def __init__(
        self,
        api_key: str,
        *,
        azure_endpoint: str,
        deployment: str = "text-embedding-3-large",
        api_version: str = "2024-02-01",
        dimensions: int | None = None,
        user: str | None = None,
        client_options: Dict[str, Any] | None = None,
        request_options: Dict[str, Any] | None = None,
    ) -> None:
        if not api_key:
            raise ValueError("api_key is required to use AzureOpenAIEmbedding")
        if not azure_endpoint:
            raise ValueError("azure_endpoint is required to use AzureOpenAIEmbedding")
        if not deployment:
            raise ValueError("deployment is required to use AzureOpenAIEmbedding")
        if AzureOpenAI is None:
            raise ImportError("openai is required to use AzureOpenAIEmbedding") from _IMPORT_ERROR

        super().__init__(model=deployment)

        client_kwargs: Dict[str, Any] = {
            "api_key": api_key,
            "azure_endpoint": azure_endpoint,
            "api_version": api_version,
        }
        if client_options:
            client_kwargs.update(client_options)

        self._client = AzureOpenAI(**client_kwargs)
        self._dimensions = dimensions
        self._user = user
        self._request_options = request_options.copy() if request_options else {}

    def embed_documents(self, texts: Sequence[str]) -> List[List[float]]:
        normalized_inputs = self._normalize_inputs(texts)
        if not normalized_inputs:
            return []

        payload: Dict[str, Any] = {
            "model": self.model,
            "input": normalized_inputs,
        }
        if self._dimensions is not None:
            payload["dimensions"] = self._dimensions
        if self._user is not None:
            payload["user"] = self._user
        payload.update(self._request_options)

        try:
            response = self._client.embeddings.create(**payload)
        except Exception as exc:
            message = "Failed to retrieve embeddings from Azure OpenAI provider"
            logger.exception(message)
            raise EmbeddingProviderError(message, original_exception=exc) from exc

        embeddings: List[List[float]] = []
        for item in response.data:
            vector = list(getattr(item, "embedding", []) or [])
            if not vector:
                raise EmbeddingProviderError("Azure OpenAI response did not contain embedding vectors")
            embeddings.append(vector)

        return embeddings


__all__ = ["AzureOpenAIEmbedding"]
