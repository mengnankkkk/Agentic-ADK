from __future__ import annotations

import logging
from typing import Any, List, Sequence

try:  # pragma: no cover - exercised through dependency injection in tests
    from tencentcloud.common.credential import Credential
    from tencentcloud.common.exception.tencent_cloud_sdk_exception import (
        TencentCloudSDKException,
    )
    from tencentcloud.common.profile.client_profile import ClientProfile
    from tencentcloud.common.profile.http_profile import HttpProfile
    from tencentcloud.hunyuan.v20230901 import hunyuan_client, models
except ImportError as import_error:  # pragma: no cover - captured at runtime
    Credential = None  # type: ignore[assignment]
    ClientProfile = None  # type: ignore[assignment]
    HttpProfile = None  # type: ignore[assignment]
    TencentCloudSDKException = None  # type: ignore[assignment]
    hunyuan_client = None  # type: ignore[assignment]
    models = None  # type: ignore[assignment]
    _IMPORT_ERROR = import_error
else:  # pragma: no cover - validated via unit tests
    _IMPORT_ERROR = None

from ..common.exceptions import EmbeddingProviderError
from .basic_embedding import BasicEmbedding

logger = logging.getLogger(__name__)


class TencentEmbedding(BasicEmbedding):
    """Embedding provider backed by Tencent Cloud Hunyuan embeddings API."""

    _DEFAULT_REGION = "ap-guangzhou"

    def __init__(
        self,
        secret_id: str | None = None,
        secret_key: str | None = None,
        *,
        model: str = "text-embedding",
        region: str | None = None,
        session_token: str | None = None,
        endpoint: str | None = None,
        credential: Credential | None = None,  # type: ignore[type-arg]
        http_profile: HttpProfile | None = None,  # type: ignore[type-arg]
        client_profile: ClientProfile | None = None,  # type: ignore[type-arg]
        client: Any | None = None,
    ) -> None:
        if (
            hunyuan_client is None
            or models is None
            or Credential is None
            or ClientProfile is None
            or HttpProfile is None
        ):
            raise ImportError(
                "tencentcloud-sdk-python is required to use TencentEmbedding"
            ) from _IMPORT_ERROR

        super().__init__(model=model)

        if client is not None:
            self._client = client
        else:
            self._client = self._build_client(
                secret_id=secret_id,
                secret_key=secret_key,
                region=region,
                session_token=session_token,
                endpoint=endpoint,
                credential=credential,
                http_profile=http_profile,
                client_profile=client_profile,
            )

    def embed_documents(self, texts: Sequence[str]) -> List[List[float]]:
        normalized_inputs = self._normalize_inputs(texts)
        if not normalized_inputs:
            return []

        embeddings: List[List[float]] = []
        for text in normalized_inputs:
            request = models.GetEmbeddingRequest()  # type: ignore[call-arg]
            request.Input = text  # type: ignore[attr-defined]

            try:
                response = self._client.GetEmbedding(request)
            except TencentCloudSDKException as exc:  # type: ignore[misc]
                message = "Failed to retrieve embeddings from Tencent Hunyuan provider"
                logger.exception(message)
                raise EmbeddingProviderError(message, original_exception=exc) from exc
            except Exception as exc:  # pragma: no cover - defensive fallback
                message = "Unexpected error while invoking Tencent Hunyuan provider"
                logger.exception(message)
                raise EmbeddingProviderError(message, original_exception=exc) from exc

            vector = self._extract_embedding_vector(response)
            if vector is None:
                raise EmbeddingProviderError(
                    "Tencent Hunyuan response did not contain embedding vectors"
                )

            embeddings.append(self._coerce_vector(vector))

        return embeddings

    @staticmethod
    def _extract_embedding_vector(payload: Any) -> Sequence[Any] | None:
        data_entries = getattr(payload, "Data", None)
        if isinstance(data_entries, list) and data_entries:
            first_entry = data_entries[0]
            if isinstance(first_entry, dict):
                candidate = first_entry.get("Embedding") or first_entry.get("embedding")
                if candidate:
                    return candidate
            else:
                candidate = getattr(first_entry, "Embedding", None)
                if candidate:
                    return candidate

        return None

    @staticmethod
    def _coerce_vector(vector: Sequence[Any]) -> List[float]:
        try:
            return [float(component) for component in vector]
        except (TypeError, ValueError) as exc:
            raise EmbeddingProviderError(
                "Tencent Hunyuan embedding vector contained non-numeric values",
                original_exception=exc,
            ) from exc

    def _build_client(
        self,
        *,
        secret_id: str | None,
        secret_key: str | None,
        region: str | None,
        session_token: str | None,
        endpoint: str | None,
        credential: Credential | None,  # type: ignore[type-arg]
        http_profile: HttpProfile | None,  # type: ignore[type-arg]
        client_profile: ClientProfile | None,  # type: ignore[type-arg]
    ) -> Any:
        if credential is None:
            if not secret_id or not secret_key:
                raise ValueError(
                    "secret_id and secret_key are required to use TencentEmbedding"
                )
            credential = Credential(secret_id, secret_key, session_token)

        region_name = region or self._DEFAULT_REGION

        http_profile_instance = http_profile or HttpProfile()
        if endpoint:
            http_profile_instance.endpoint = endpoint

        profile = client_profile or ClientProfile()
        profile.httpProfile = http_profile_instance

        return hunyuan_client.HunyuanClient(credential, region_name, profile)


__all__ = ["TencentEmbedding"]
