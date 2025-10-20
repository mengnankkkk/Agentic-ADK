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


class OllamaEmbedding(BasicEmbedding):
    """Embedding provider backed by the Ollama embeddings REST API."""

    _DEFAULT_BASE_URL = "http://localhost:11434"
    _NEW_ENDPOINT_PATH = "/api/embed"
    _LEGACY_ENDPOINT_PATH = "/api/embeddings"

    def __init__(
        self,
        model: str = "nomic-embed-text",
        *,
        base_url: str | None = None,
        timeout: float | tuple[float, float] | None = None,
        headers: Dict[str, str] | None = None,
        request_options: Dict[str, Any] | None = None,
        prefer_legacy_endpoint: bool = False,
    ) -> None:
        if requests is None:
            raise ImportError("requests is required to use OllamaEmbedding") from _IMPORT_ERROR

        super().__init__(model=model)
        self._base_url = (base_url or self._DEFAULT_BASE_URL).rstrip("/")
        self._timeout = timeout
        self._request_options = request_options.copy() if request_options else {}
        self._prefer_legacy = prefer_legacy_endpoint

        default_headers: Dict[str, str] = {"Content-Type": "application/json"}
        if headers:
            default_headers.update(headers)
        self._headers = default_headers

    def embed_documents(self, texts: Sequence[str]) -> List[List[float]]:
        normalized_inputs = self._normalize_inputs(texts)
        if not normalized_inputs:
            return []

        if self._prefer_legacy:
            return self._embed_via_legacy(normalized_inputs)

        payload = self._build_payload(normalized_inputs)
        response = self._post(self._resolve_endpoint(self._NEW_ENDPOINT_PATH), payload)

        if response.status_code == 404:
            logger.info("Ollama embed endpoint not found, retrying legacy embeddings endpoint")
            return self._embed_via_legacy(normalized_inputs)

        content = self._parse_response(response)
        vectors = self._extract_embeddings(content)
        if not vectors:
            raise EmbeddingProviderError("Ollama response did not contain embedding vectors")

        return [self._coerce_vector(vector) for vector in vectors]

    def _embed_via_legacy(self, texts: Sequence[str]) -> List[List[float]]:
        embeddings: List[List[float]] = []
        for text in texts:
            payload = self._build_payload([text], legacy=True)
            response = self._post(self._resolve_endpoint(self._LEGACY_ENDPOINT_PATH), payload)
            content = self._parse_response(response)
            vector = self._extract_legacy_embedding(content)
            if vector is None:
                raise EmbeddingProviderError("Ollama legacy response did not contain embedding vectors")
            embeddings.append(self._coerce_vector(vector))
        return embeddings

    def _post(self, endpoint: str, payload: Dict[str, Any]) -> "requests.Response":
        try:
            response = requests.post(
                endpoint,
                headers=self._headers,
                json=payload,
                timeout=self._timeout,
            )
        except requests.exceptions.RequestException as exc:
            message = "Failed to retrieve embeddings from Ollama provider"
            logger.exception(message)
            raise EmbeddingProviderError(message, original_exception=exc) from exc

        return response

    def _parse_response(self, response: "requests.Response") -> Any:
        try:
            response.raise_for_status()
        except requests.exceptions.RequestException as exc:
            message = "Ollama embedding request returned an error response"
            logger.exception(message)
            raise EmbeddingProviderError(message, original_exception=exc) from exc

        try:
            return response.json()
        except ValueError as exc:
            message = "Failed to parse Ollama embedding response body"
            logger.exception(message)
            raise EmbeddingProviderError(message, original_exception=exc) from exc

    def _extract_embeddings(self, payload: Any) -> List[Iterable[float]] | None:
        if not isinstance(payload, dict):
            return None

        embeddings = payload.get("embeddings")
        if embeddings is None and isinstance(payload.get("data"), list):
            embeddings = []
            for item in payload["data"]:
                if isinstance(item, dict) and "embedding" in item:
                    embeddings.append(item["embedding"])
        if embeddings is None:
            return None

        if not isinstance(embeddings, list):
            return None

        coerced: List[Iterable[float]] = []
        for item in embeddings:
            if isinstance(item, dict) and "embedding" in item:
                item = item["embedding"]
            if not isinstance(item, (list, tuple)):
                return None
            coerced.append(item)
        return coerced if coerced else None

    @staticmethod
    def _extract_legacy_embedding(payload: Any) -> Iterable[float] | None:
        if isinstance(payload, dict):
            embedding = payload.get("embedding")
            if isinstance(embedding, (list, tuple)):
                return embedding
        return None

    @staticmethod
    def _coerce_vector(vector: Iterable[float]) -> List[float]:
        try:
            return [float(component) for component in vector]
        except (TypeError, ValueError) as exc:
            raise EmbeddingProviderError(
                "Ollama embedding vector contained non-numeric values",
                original_exception=exc,
            ) from exc

    def _build_payload(self, texts: Sequence[str], legacy: bool = False) -> Dict[str, Any]:
        payload: Dict[str, Any] = {"model": self.model}
        if legacy:
            payload["prompt"] = texts[0] if texts else ""
        else:
            payload["input"] = list(texts)

        if self._request_options:
            payload.update(self._request_options)
        return payload

    def _resolve_endpoint(self, path: str) -> str:
        return f"{self._base_url}{path}"


__all__ = ["OllamaEmbedding"]
