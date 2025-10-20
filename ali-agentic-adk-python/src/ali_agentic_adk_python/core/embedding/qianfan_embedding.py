from __future__ import annotations

import logging
import time
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


class QianfanEmbedding(BasicEmbedding):
    """Embedding provider backed by Baidu Qianfan (ERNIE) embeddings API."""

    _DEFAULT_ENDPOINT_TEMPLATE = (
        "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/embeddings/{model}"
    )
    _TOKEN_ENDPOINT = "https://aip.baidubce.com/oauth/2.0/token"
    _TOKEN_ERROR_CODES = {110, 111, 110010, 110011, 110021}

    def __init__(
        self,
        *,
        model: str = "embedding-v1",
        api_key: str | None = None,
        secret_key: str | None = None,
        access_token: str | None = None,
        endpoint_template: str | None = None,
        timeout: float | tuple[float, float] | None = None,
        token_timeout: float | tuple[float, float] | None = None,
        headers: Dict[str, str] | None = None,
        request_options: Dict[str, Any] | None = None,
    ) -> None:
        if requests is None:  
            raise ImportError("requests is required to use QianfanEmbedding") from _IMPORT_ERROR
        if not access_token and not (api_key and secret_key):
            raise ValueError("QianfanEmbedding requires either an access_token or api_key/secret_key pair")

        super().__init__(model=model)
        self._api_key = api_key
        self._secret_key = secret_key
        self._access_token = access_token
        self._access_token_expiry: float | None = None
        self._endpoint_template = (endpoint_template or self._DEFAULT_ENDPOINT_TEMPLATE).rstrip("/")
        self._timeout = timeout
        self._token_timeout = token_timeout
        self._request_options = request_options.copy() if request_options else {}

        base_headers = {"Content-Type": "application/json", "Accept": "application/json"}
        if headers:
            base_headers.update(headers)
        self._headers = base_headers

    def embed_documents(self, texts: Sequence[str]) -> List[List[float]]:
        normalized_inputs = self._normalize_inputs(texts)
        if not normalized_inputs:
            return []

        payload: Dict[str, Any] = {"input": normalized_inputs}
        if self._request_options:
            payload.update(self._request_options)

        for attempt in range(2):
            token = self._ensure_access_token(force_refresh=attempt == 1)
            url = f"{self._endpoint_template.format(model=self.model)}?access_token={token}"
            try:
                response = requests.post(
                    url,
                    headers=self._headers,
                    json=payload,
                    timeout=self._timeout,
                )
                response.raise_for_status()
            except requests.exceptions.RequestException as exc:  # pragma: no cover - propagated
                message = "Failed to retrieve embeddings from Qianfan provider"
                logger.exception(message)
                raise EmbeddingProviderError(message, original_exception=exc) from exc

            try:
                content = response.json()
            except ValueError as exc:
                message = "Failed to parse Qianfan embedding response body"
                logger.exception(message)
                raise EmbeddingProviderError(message, original_exception=exc) from exc

            if self._should_refresh_token(content) and attempt == 0:
                self._invalidate_access_token()
                continue

            vectors = self._extract_embeddings(content)
            if not vectors:
                raise EmbeddingProviderError("Qianfan response did not contain embedding vectors")

            return [self._coerce_vector(vector) for vector in vectors]

        raise EmbeddingProviderError("Qianfan response still invalid after refreshing access token")

    def _ensure_access_token(self, *, force_refresh: bool = False) -> str:
        if self._access_token and not force_refresh:
            if self._access_token_expiry is None or self._access_token_expiry > time.time():
                return self._access_token

        if not (self._api_key and self._secret_key):
            if self._access_token:
                return self._access_token
            raise EmbeddingProviderError("Qianfan access token is missing and cannot be refreshed automatically")

        params = {
            "grant_type": "client_credentials",
            "client_id": self._api_key,
            "client_secret": self._secret_key,
        }
        try:
            response = requests.post(
                self._TOKEN_ENDPOINT,
                params=params,
                timeout=self._token_timeout,
            )
            response.raise_for_status()
        except requests.exceptions.RequestException as exc:  
            message = "Failed to refresh Qianfan access token"
            logger.exception(message)
            raise EmbeddingProviderError(message, original_exception=exc) from exc

        try:
            token_payload = response.json()
        except ValueError as exc:
            message = "Failed to parse Qianfan access token response"
            logger.exception(message)
            raise EmbeddingProviderError(message, original_exception=exc) from exc

        access_token = token_payload.get("access_token")
        expires_in = token_payload.get("expires_in")
        if not access_token:
            raise EmbeddingProviderError("Qianfan token response did not contain access_token")

        self._access_token = access_token
        if isinstance(expires_in, (int, float)):
            self._access_token_expiry = time.time() + float(expires_in) - 60 
        else:
            self._access_token_expiry = None
        return self._access_token

    def _invalidate_access_token(self) -> None:
        self._access_token = None
        self._access_token_expiry = None

    @classmethod
    def _should_refresh_token(cls, payload: Any) -> bool:
        if isinstance(payload, dict):
            error_code = payload.get("error_code")
            return isinstance(error_code, int) and error_code in cls._TOKEN_ERROR_CODES
        return False

    @staticmethod
    def _extract_embeddings(payload: Any) -> Iterable[Sequence[Any]] | None:
        if payload is None:
            return None

        data_field = payload.get("data") if isinstance(payload, dict) else None
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
                "Qianfan embedding vector contained non-numeric values",
                original_exception=exc,
            ) from exc


__all__ = ["QianfanEmbedding"]
