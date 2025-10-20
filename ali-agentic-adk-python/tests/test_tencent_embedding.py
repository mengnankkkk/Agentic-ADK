import unittest
from types import SimpleNamespace
from unittest.mock import MagicMock, patch

from ali_agentic_adk_python.core.common.exceptions import EmbeddingProviderError
from ali_agentic_adk_python.core.embedding.tencent_embedding import TencentEmbedding


class TencentEmbeddingTestCase(unittest.TestCase):
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_embed_documents_returns_vectors(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_instance = MagicMock()
        credential_cls.return_value = credential_instance

        http_profile_instance = MagicMock()
        http_profile_cls.return_value = http_profile_instance

        client_profile_instance = MagicMock()
        client_profile_cls.return_value = client_profile_instance

        request_instance = MagicMock()
        models_module.GetEmbeddingRequest.return_value = request_instance

        client_mock = MagicMock()
        hunyuan_module.HunyuanClient.return_value = client_mock

        first_response = SimpleNamespace(Data=[SimpleNamespace(Embedding=[0.1, 0.2])])
        second_response = SimpleNamespace(Data=[SimpleNamespace(Embedding=[0.3, 0.4])])
        client_mock.GetEmbedding.side_effect = [first_response, second_response]

        embedding = TencentEmbedding(
            secret_id="example-id",
            secret_key="example-key",
            region="ap-shanghai",
            endpoint="hunyuan.tencentcloudapi.com",
        )

        vectors = embedding.embed_documents(["hello", "world"])

        self.assertEqual(vectors, [[0.1, 0.2], [0.3, 0.4]])
        self.assertEqual(client_mock.GetEmbedding.call_count, 2)
        models_module.GetEmbeddingRequest.assert_called()
        self.assertEqual(http_profile_instance.endpoint, "hunyuan.tencentcloudapi.com")
        self.assertIs(client_profile_instance.httpProfile, http_profile_instance)

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_missing_credentials_raise(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_cls.return_value = MagicMock()
        http_profile_cls.return_value = MagicMock()
        client_profile_cls.return_value = MagicMock()
        models_module.GetEmbeddingRequest.side_effect = AssertionError("Should not be called")
        hunyuan_module.HunyuanClient.return_value = MagicMock()

        with self.assertRaises(ValueError):
            TencentEmbedding(secret_id=None, secret_key="key")

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_missing_secret_key_raise(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_cls.return_value = MagicMock()
        http_profile_cls.return_value = MagicMock()
        client_profile_cls.return_value = MagicMock()
        models_module.GetEmbeddingRequest.side_effect = AssertionError("Should not be called")
        hunyuan_module.HunyuanClient.return_value = MagicMock()

        with self.assertRaises(ValueError):
            TencentEmbedding(secret_id="id", secret_key=None)

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    @patch(
        "ali_agentic_adk_python.core.embedding.tencent_embedding.TencentCloudSDKException",
        new=Exception,
    )
    def test_sdk_error_wrapped(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_cls.return_value = MagicMock()
        http_profile_cls.return_value = MagicMock()
        client_profile_cls.return_value = MagicMock()
        models_module.GetEmbeddingRequest.return_value = MagicMock()

        client_mock = MagicMock()
        client_mock.GetEmbedding.side_effect = Exception("boom")
        hunyuan_module.HunyuanClient.return_value = client_mock

        embedding = TencentEmbedding(secret_id="id", secret_key="key")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["demo"])

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_missing_vectors_raise(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_cls.return_value = MagicMock()
        http_profile_cls.return_value = MagicMock()
        client_profile_cls.return_value = MagicMock()
        models_module.GetEmbeddingRequest.return_value = MagicMock()

        client_mock = MagicMock()
        client_mock.GetEmbedding.return_value = SimpleNamespace(Data=[])
        hunyuan_module.HunyuanClient.return_value = client_mock

        embedding = TencentEmbedding(secret_id="id", secret_key="key")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["sample"])

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_embed_documents_with_empty_input(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_cls.return_value = MagicMock()
        http_profile_cls.return_value = MagicMock()
        client_profile_cls.return_value = MagicMock()
        models_module.GetEmbeddingRequest.return_value = MagicMock()

        client_mock = MagicMock()
        hunyuan_module.HunyuanClient.return_value = client_mock

        embedding = TencentEmbedding(secret_id="id", secret_key="key")
        vectors = embedding.embed_documents([])

        self.assertEqual(vectors, [])
        client_mock.GetEmbedding.assert_not_called()

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_custom_client_parameter(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        custom_client = MagicMock()
        models_module.GetEmbeddingRequest.return_value = MagicMock()
        
        response = SimpleNamespace(Data=[SimpleNamespace(Embedding=[0.5, 0.6])])
        custom_client.GetEmbedding.return_value = response

        embedding = TencentEmbedding(client=custom_client)
        vectors = embedding.embed_documents(["test"])

        self.assertEqual(vectors, [[0.5, 0.6]])
        custom_client.GetEmbedding.assert_called_once()
        hunyuan_module.HunyuanClient.assert_not_called()

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_session_token_parameter(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_instance = MagicMock()
        credential_cls.return_value = credential_instance
        http_profile_cls.return_value = MagicMock()
        client_profile_cls.return_value = MagicMock()
        models_module.GetEmbeddingRequest.return_value = MagicMock()
        hunyuan_module.HunyuanClient.return_value = MagicMock()

        embedding = TencentEmbedding(
            secret_id="id",
            secret_key="key",
            session_token="token123",
        )

        credential_cls.assert_called_once_with("id", "key", "token123")

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_custom_credential_parameter(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        custom_credential = MagicMock()
        http_profile_cls.return_value = MagicMock()
        client_profile_cls.return_value = MagicMock()
        models_module.GetEmbeddingRequest.return_value = MagicMock()
        hunyuan_module.HunyuanClient.return_value = MagicMock()

        embedding = TencentEmbedding(credential=custom_credential)

        credential_cls.assert_not_called()
        hunyuan_module.HunyuanClient.assert_called_once()
        args = hunyuan_module.HunyuanClient.call_args[0]
        self.assertIs(args[0], custom_credential)

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_custom_http_profile_parameter(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_cls.return_value = MagicMock()
        custom_http_profile = MagicMock()
        client_profile_instance = MagicMock()
        client_profile_cls.return_value = client_profile_instance
        models_module.GetEmbeddingRequest.return_value = MagicMock()
        hunyuan_module.HunyuanClient.return_value = MagicMock()

        embedding = TencentEmbedding(
            secret_id="id",
            secret_key="key",
            http_profile=custom_http_profile,
        )

        http_profile_cls.assert_not_called()
        self.assertIs(client_profile_instance.httpProfile, custom_http_profile)

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_custom_client_profile_parameter(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_cls.return_value = MagicMock()
        http_profile_instance = MagicMock()
        http_profile_cls.return_value = http_profile_instance
        custom_client_profile = MagicMock()
        models_module.GetEmbeddingRequest.return_value = MagicMock()
        hunyuan_module.HunyuanClient.return_value = MagicMock()

        embedding = TencentEmbedding(
            secret_id="id",
            secret_key="key",
            client_profile=custom_client_profile,
        )

        client_profile_cls.assert_not_called()
        self.assertIs(custom_client_profile.httpProfile, http_profile_instance)

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_default_region(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_cls.return_value = MagicMock()
        http_profile_cls.return_value = MagicMock()
        client_profile_cls.return_value = MagicMock()
        models_module.GetEmbeddingRequest.return_value = MagicMock()
        hunyuan_module.HunyuanClient.return_value = MagicMock()

        embedding = TencentEmbedding(secret_id="id", secret_key="key")

        args = hunyuan_module.HunyuanClient.call_args[0]
        self.assertEqual(args[1], "ap-guangzhou")

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_custom_model_parameter(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_cls.return_value = MagicMock()
        http_profile_cls.return_value = MagicMock()
        client_profile_cls.return_value = MagicMock()
        models_module.GetEmbeddingRequest.return_value = MagicMock()
        hunyuan_module.HunyuanClient.return_value = MagicMock()

        embedding = TencentEmbedding(
            secret_id="id",
            secret_key="key",
            model="custom-embedding-model",
        )

        self.assertEqual(embedding._model, "custom-embedding-model")

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_embed_query(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_cls.return_value = MagicMock()
        http_profile_cls.return_value = MagicMock()
        client_profile_cls.return_value = MagicMock()
        models_module.GetEmbeddingRequest.return_value = MagicMock()

        client_mock = MagicMock()
        response = SimpleNamespace(Data=[SimpleNamespace(Embedding=[0.7, 0.8, 0.9])])
        client_mock.GetEmbedding.return_value = response
        hunyuan_module.HunyuanClient.return_value = client_mock

        embedding = TencentEmbedding(secret_id="id", secret_key="key")
        vector = embedding.embed_query("test query")

        self.assertEqual(vector, [0.7, 0.8, 0.9])
        client_mock.GetEmbedding.assert_called_once()

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_response_with_dict_format(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_cls.return_value = MagicMock()
        http_profile_cls.return_value = MagicMock()
        client_profile_cls.return_value = MagicMock()
        models_module.GetEmbeddingRequest.return_value = MagicMock()

        client_mock = MagicMock()
        response = SimpleNamespace(Data=[{"Embedding": [1.0, 2.0, 3.0]}])
        client_mock.GetEmbedding.return_value = response
        hunyuan_module.HunyuanClient.return_value = client_mock

        embedding = TencentEmbedding(secret_id="id", secret_key="key")
        vectors = embedding.embed_documents(["test"])

        self.assertEqual(vectors, [[1.0, 2.0, 3.0]])

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_response_with_lowercase_embedding_key(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_cls.return_value = MagicMock()
        http_profile_cls.return_value = MagicMock()
        client_profile_cls.return_value = MagicMock()
        models_module.GetEmbeddingRequest.return_value = MagicMock()

        client_mock = MagicMock()
        response = SimpleNamespace(Data=[{"embedding": [4.0, 5.0]}])
        client_mock.GetEmbedding.return_value = response
        hunyuan_module.HunyuanClient.return_value = client_mock

        embedding = TencentEmbedding(secret_id="id", secret_key="key")
        vectors = embedding.embed_documents(["test"])

        self.assertEqual(vectors, [[4.0, 5.0]])

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_non_numeric_vector_raises(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_cls.return_value = MagicMock()
        http_profile_cls.return_value = MagicMock()
        client_profile_cls.return_value = MagicMock()
        models_module.GetEmbeddingRequest.return_value = MagicMock()

        client_mock = MagicMock()
        response = SimpleNamespace(Data=[SimpleNamespace(Embedding=["invalid", "data"])])
        client_mock.GetEmbedding.return_value = response
        hunyuan_module.HunyuanClient.return_value = client_mock

        embedding = TencentEmbedding(secret_id="id", secret_key="key")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_response_with_no_data_attribute(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_cls.return_value = MagicMock()
        http_profile_cls.return_value = MagicMock()
        client_profile_cls.return_value = MagicMock()
        models_module.GetEmbeddingRequest.return_value = MagicMock()

        client_mock = MagicMock()
        response = SimpleNamespace()
        client_mock.GetEmbedding.return_value = response
        hunyuan_module.HunyuanClient.return_value = client_mock

        embedding = TencentEmbedding(secret_id="id", secret_key="key")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_vector_type_coercion(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_cls.return_value = MagicMock()
        http_profile_cls.return_value = MagicMock()
        client_profile_cls.return_value = MagicMock()
        models_module.GetEmbeddingRequest.return_value = MagicMock()

        client_mock = MagicMock()
        response = SimpleNamespace(Data=[SimpleNamespace(Embedding=[1, 2, 3])])
        client_mock.GetEmbedding.return_value = response
        hunyuan_module.HunyuanClient.return_value = client_mock

        embedding = TencentEmbedding(secret_id="id", secret_key="key")
        vectors = embedding.embed_documents(["test"])

        self.assertEqual(vectors, [[1.0, 2.0, 3.0]])
        self.assertIsInstance(vectors[0][0], float)
        self.assertIsInstance(vectors[0][1], float)
        self.assertIsInstance(vectors[0][2], float)

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_multiple_documents_with_different_lengths(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_cls.return_value = MagicMock()
        http_profile_cls.return_value = MagicMock()
        client_profile_cls.return_value = MagicMock()
        models_module.GetEmbeddingRequest.return_value = MagicMock()

        client_mock = MagicMock()
        response1 = SimpleNamespace(Data=[SimpleNamespace(Embedding=[0.1, 0.2])])
        response2 = SimpleNamespace(Data=[SimpleNamespace(Embedding=[0.3, 0.4, 0.5])])
        response3 = SimpleNamespace(Data=[SimpleNamespace(Embedding=[0.6])])
        client_mock.GetEmbedding.side_effect = [response1, response2, response3]
        hunyuan_module.HunyuanClient.return_value = client_mock

        embedding = TencentEmbedding(secret_id="id", secret_key="key")
        vectors = embedding.embed_documents(["short", "medium text", "x"])

        self.assertEqual(len(vectors), 3)
        self.assertEqual(vectors[0], [0.1, 0.2])
        self.assertEqual(vectors[1], [0.3, 0.4, 0.5])
        self.assertEqual(vectors[2], [0.6])

    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.models")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.hunyuan_client")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.ClientProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.HttpProfile")
    @patch("ali_agentic_adk_python.core.embedding.tencent_embedding.Credential")
    def test_single_document_embedding(
        self,
        credential_cls,
        http_profile_cls,
        client_profile_cls,
        hunyuan_module,
        models_module,
    ):
        credential_cls.return_value = MagicMock()
        http_profile_cls.return_value = MagicMock()
        client_profile_cls.return_value = MagicMock()
        
        request_instance = MagicMock()
        models_module.GetEmbeddingRequest.return_value = request_instance

        client_mock = MagicMock()
        response = SimpleNamespace(Data=[SimpleNamespace(Embedding=[0.1, 0.2])])
        client_mock.GetEmbedding.return_value = response
        hunyuan_module.HunyuanClient.return_value = client_mock

        embedding = TencentEmbedding(secret_id="id", secret_key="key")
        vectors = embedding.embed_documents(["single document"])

        self.assertEqual(vectors, [[0.1, 0.2]])
        client_mock.GetEmbedding.assert_called_once()


if __name__ == "__main__":
    unittest.main()
