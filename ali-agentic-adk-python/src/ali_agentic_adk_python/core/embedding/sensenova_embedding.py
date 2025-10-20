from __future__ import annotations

import logging
import time
from typing import Any, Dict, Iterable, List, Sequence

try:
    import requests
except ImportError as import_error:
    requests = None
    _REQUESTS_IMPORT_ERROR = import_error
else:
    _REQUESTS_IMPORT_ERROR = None

try:
    import jwt
except ImportError as import_error:
    jwt = None
    _JWT_IMPORT_ERROR = import_error
else:
    _JWT_IMPORT_ERROR = None

from ..common.exceptions import EmbeddingProviderError
from .basic_embedding import BasicEmbedding

logger = logging.getLogger(__name__)


class SenseNovaEmbedding(BasicEmbedding):
    """Embedding provider backed by SenseTime SenseNova embeddings API."""

    _DEFAULT_ENDPOINT = "https://api.sensenova.cn/v1/llm/embeddings"
    _TOKEN_TTL_SECONDS = 1800
    _TOKEN_NOT_BEFORE_SKEW = 5

    def __init__(
        self,
        access_key_id: str,
        secret_access_key: str,
        model: str = "nova-embedding-v1",
        *,
        endpoint: str | None = None,
        timeout: float | tuple[float, float] | None = None,
        headers: Dict[str, str] | None = None,
        request_options: Dict[str, Any] | None = None,
    ) -> None:
        if not access_key_id:
            raise ValueError("access_key_id is required to use SenseNovaEmbedding")
        if not secret_access_key:
            raise ValueError("secret_access_key is required to use SenseNovaEmbedding")
        if requests is None:
            raise ImportError("requests is required to use SenseNovaEmbedding") from _REQUESTS_IMPORT_ERROR
        if jwt is None:
            raise ImportError("PyJWT is required to use SenseNovaEmbedding") from _JWT_IMPORT_ERROR

        super().__init__(model=model)
        self._access_key_id = access_key_id
        self._secret_access_key = secret_access_key
        self._endpoint = endpoint or self._DEFAULT_ENDPOINT
        self._timeout = timeout
        self._request_options = request_options.copy() if request_options else {}
        self._extra_headers = headers.copy() if headers else {}

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
                headers=self._compose_headers(),
                json=payload,
                timeout=self._timeout,
            )
            response.raise_for_status()
        except requests.exceptions.RequestException as exc:
            message = "Failed to retrieve embeddings from SenseNova provider"
            logger.exception(message)
            raise EmbeddingProviderError(message, original_exception=exc) from exc

        try:
            content = response.json()
        except ValueError as exc:
            message = "Failed to parse SenseNova embedding response body"
            logger.exception(message)
            raise EmbeddingProviderError(message, original_exception=exc) from exc

        vectors = self._extract_embeddings(content)
        if not vectors:
            raise EmbeddingProviderError(
                "SenseNova response did not contain embedding vectors"
            )

        return [self._coerce_vector(vector) for vector in vectors]

    def _compose_headers(self) -> Dict[str, str]:
        headers: Dict[str, str] = {
            "Content-Type": "application/json",
            "Accept": "application/json",
        }

        if self._extra_headers:
            headers.update(self._extra_headers)

        headers["Authorization"] = self._build_authorization_header()
        return headers

    def _build_authorization_header(self) -> str:
        now = int(time.time())
        payload = {
            "iss": self._access_key_id,
            "exp": now + self._TOKEN_TTL_SECONDS,
            "nbf": now - self._TOKEN_NOT_BEFORE_SKEW,
        }
        token = jwt.encode(
            payload,
            self._secret_access_key,
            algorithm="HS256",
        )
        if isinstance(token, bytes):
            token = token.decode("utf-8")
        return f"Bearer {token}"

    @staticmethod
    def _extract_embeddings(payload: Any) -> Iterable[Sequence[Any]] | None:
        if payload is None:
            return None

        data_entries = None
        if isinstance(payload, dict):
            data_entries = payload.get("data") or payload.get("embeddings")
        elif isinstance(payload, list):
            data_entries = payload

        if isinstance(data_entries, list) and data_entries:
            embeddings: List[Sequence[Any]] = []
            for item in data_entries:
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
                "SenseNova embedding vector contained non-numeric values",
                original_exception=exc,
            ) from exc


__all__ = ["SenseNovaEmbedding"]
