import unittest
from unittest.mock import MagicMock, patch

from ali_agentic_adk_python.core.common.exceptions import EmbeddingProviderError
from ali_agentic_adk_python.core.embedding.azure_openai_embedding import AzureOpenAIEmbedding


class AzureOpenAIEmbeddingTestCase(unittest.TestCase):
    def test_missing_api_key_raises(self):
        with self.assertRaises(ValueError):
            AzureOpenAIEmbedding(api_key="", azure_endpoint="https://example.com", deployment="dep")

    def test_none_api_key_raises(self):
        with self.assertRaises(ValueError):
            AzureOpenAIEmbedding(api_key=None, azure_endpoint="https://example.com", deployment="dep")

    def test_missing_endpoint_raises(self):
        with self.assertRaises(ValueError):
            AzureOpenAIEmbedding(api_key="key", azure_endpoint="", deployment="dep")

    def test_none_endpoint_raises(self):
        with self.assertRaises(ValueError):
            AzureOpenAIEmbedding(api_key="key", azure_endpoint=None, deployment="dep")

    def test_missing_deployment_raises(self):
        with self.assertRaises(ValueError):
            AzureOpenAIEmbedding(api_key="key", azure_endpoint="https://example.com", deployment="")

    def test_none_deployment_raises(self):
        with self.assertRaises(ValueError):
            AzureOpenAIEmbedding(api_key="key", azure_endpoint="https://example.com", deployment=None)

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_embed_documents_returns_vectors(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[
                MagicMock(embedding=[0.1, 0.2]),
                MagicMock(embedding=[0.3, 0.4]),
            ]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="embedding-deployment",
        )
        vectors = embedding.embed_documents(["hello", "world"])

        self.assertEqual(vectors, [[0.1, 0.2], [0.3, 0.4]])
        client_mock.embeddings.create.assert_called_once_with(
            model="embedding-deployment",
            input=["hello", "world"],
        )
        azure_cls.assert_called_once_with(
            api_key="key",
            azure_endpoint="https://example.com",
            api_version="2024-02-01",
        )

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_request_options_and_client_options_are_forwarded(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.5, 0.6])]
        )

        request_options = {"encoding_format": "float"}
        client_options = {"azure_ad_token": "token123"}

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="embedding-deployment",
            dimensions=1024,
            user="alice",
            request_options=request_options,
            client_options=client_options,
        )
        embedding.embed_documents(["hi"])

        client_mock.embeddings.create.assert_called_once_with(
            model="embedding-deployment",
            input=["hi"],
            dimensions=1024,
            user="alice",
            encoding_format="float",
        )
        azure_cls.assert_called_once_with(
            api_key="key",
            azure_endpoint="https://example.com",
            api_version="2024-02-01",
            azure_ad_token="token123",
        )
        self.assertEqual(request_options, {"encoding_format": "float"})

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_missing_vectors_raise(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[])],
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="embedding-deployment",
        )

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_embed_query_returns_single_vector(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.9, 0.8, 0.7])]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="embedding-deployment",
        )
        vector = embedding.embed_query("query")

        self.assertEqual(vector, [0.9, 0.8, 0.7])

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_client_error_is_wrapped(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.side_effect = RuntimeError("boom")

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="embedding-deployment",
        )

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_empty_input_returns_empty_list(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="embedding-deployment",
        )
        vectors = embedding.embed_documents([])

        self.assertEqual(vectors, [])
        client_mock.embeddings.create.assert_not_called()

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_default_deployment(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.1, 0.2])]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
        )
        embedding.embed_documents(["test"])

        call_kwargs = client_mock.embeddings.create.call_args.kwargs
        self.assertEqual(call_kwargs["model"], "text-embedding-3-large")

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_custom_deployment(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.1, 0.2])]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="text-embedding-ada-002",
        )
        embedding.embed_documents(["test"])

        call_kwargs = client_mock.embeddings.create.call_args.kwargs
        self.assertEqual(call_kwargs["model"], "text-embedding-ada-002")

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_default_api_version(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.1])]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
        )

        azure_cls.assert_called_once_with(
            api_key="key",
            azure_endpoint="https://example.com",
            api_version="2024-02-01",
        )

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_custom_api_version(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.1])]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
            api_version="2023-05-15",
        )

        azure_cls.assert_called_once_with(
            api_key="key",
            azure_endpoint="https://example.com",
            api_version="2023-05-15",
        )

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_dimensions_parameter(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.1, 0.2])]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
            dimensions=512,
        )
        embedding.embed_documents(["test"])

        call_kwargs = client_mock.embeddings.create.call_args.kwargs
        self.assertEqual(call_kwargs["dimensions"], 512)

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_none_dimensions_not_included(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.1])]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
            dimensions=None,
        )
        embedding.embed_documents(["test"])

        call_kwargs = client_mock.embeddings.create.call_args.kwargs
        self.assertNotIn("dimensions", call_kwargs)

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_user_parameter(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.1])]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
            user="user123",
        )
        embedding.embed_documents(["test"])

        call_kwargs = client_mock.embeddings.create.call_args.kwargs
        self.assertEqual(call_kwargs["user"], "user123")

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_none_user_not_included(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.1])]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
            user=None,
        )
        embedding.embed_documents(["test"])

        call_kwargs = client_mock.embeddings.create.call_args.kwargs
        self.assertNotIn("user", call_kwargs)

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_request_options_copied_not_mutated(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.1])]
        )

        original_options = {"param1": "value1"}
        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
            request_options=original_options,
        )

        original_options["param2"] = "value2"

        embedding.embed_documents(["test"])

        call_kwargs = client_mock.embeddings.create.call_args.kwargs
        self.assertIn("param1", call_kwargs)
        self.assertNotIn("param2", call_kwargs)

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_client_options_forwarded(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.1])]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
            client_options={
                "timeout": 30.0,
                "max_retries": 3,
                "default_headers": {"X-Custom": "header"},
            },
        )

        call_kwargs = azure_cls.call_args.kwargs
        self.assertEqual(call_kwargs["timeout"], 30.0)
        self.assertEqual(call_kwargs["max_retries"], 3)
        self.assertEqual(call_kwargs["default_headers"], {"X-Custom": "header"})

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_none_embedding_attribute_raises(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        
        data_item = MagicMock()
        del data_item.embedding
        
        client_mock.embeddings.create.return_value = MagicMock(data=[data_item])

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
        )

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_multiple_documents_processing(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[
                MagicMock(embedding=[1.0, 2.0]),
                MagicMock(embedding=[3.0, 4.0]),
                MagicMock(embedding=[5.0, 6.0]),
            ]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
        )
        vectors = embedding.embed_documents(["doc1", "doc2", "doc3"])

        self.assertEqual(len(vectors), 3)
        self.assertEqual(vectors[0], [1.0, 2.0])
        self.assertEqual(vectors[1], [3.0, 4.0])
        self.assertEqual(vectors[2], [5.0, 6.0])

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_large_batch_of_documents(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        
        data = [MagicMock(embedding=[float(i), float(i + 1)]) for i in range(50)]
        client_mock.embeddings.create.return_value = MagicMock(data=data)

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
        )
        texts = [f"text_{i}" for i in range(50)]
        vectors = embedding.embed_documents(texts)

        self.assertEqual(len(vectors), 50)
        for i, vector in enumerate(vectors):
            self.assertEqual(vector, [float(i), float(i + 1)])

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_varying_vector_dimensions(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[
                MagicMock(embedding=[1.0, 2.0]),
                MagicMock(embedding=[3.0, 4.0, 5.0, 6.0]),
            ]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
        )
        vectors = embedding.embed_documents(["short", "long"])

        self.assertEqual(len(vectors[0]), 2)
        self.assertEqual(len(vectors[1]), 4)

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_single_document_processing(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.5, 0.6, 0.7])]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
        )
        vectors = embedding.embed_documents(["single"])

        self.assertEqual(len(vectors), 1)
        self.assertEqual(vectors[0], [0.5, 0.6, 0.7])

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_model_attribute_accessible(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="my-deployment",
        )

        self.assertEqual(embedding.model, "my-deployment")

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_encoding_format_request_option(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.1])]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
            request_options={"encoding_format": "base64"},
        )
        embedding.embed_documents(["test"])

        call_kwargs = client_mock.embeddings.create.call_args.kwargs
        self.assertEqual(call_kwargs["encoding_format"], "base64")

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_multiple_request_options(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.1])]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
            request_options={
                "encoding_format": "float",
                "extra_param": "value",
            },
        )
        embedding.embed_documents(["test"])

        call_kwargs = client_mock.embeddings.create.call_args.kwargs
        self.assertEqual(call_kwargs["encoding_format"], "float")
        self.assertEqual(call_kwargs["extra_param"], "value")

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_azure_ad_token_in_client_options(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.1])]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
            client_options={"azure_ad_token": "ad-token-123"},
        )

        call_kwargs = azure_cls.call_args.kwargs
        self.assertEqual(call_kwargs["azure_ad_token"], "ad-token-123")

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_organization_in_client_options(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.1])]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
            client_options={"organization": "org-123"},
        )

        call_kwargs = azure_cls.call_args.kwargs
        self.assertEqual(call_kwargs["organization"], "org-123")

    def test_missing_openai_dependency_raises(self):
        with patch(
            "ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI",
            None,
        ):
            with self.assertRaises(ImportError):
                AzureOpenAIEmbedding(
                    api_key="key",
                    azure_endpoint="https://example.com",
                    deployment="dep",
                )

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_exception_with_original_exception(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        original_error = RuntimeError("API error")
        client_mock.embeddings.create.side_effect = original_error

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.com",
            deployment="dep",
        )

        with self.assertRaises(EmbeddingProviderError) as context:
            embedding.embed_documents(["test"])

        self.assertIs(context.exception.__cause__, original_error)

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_endpoint_with_trailing_slash(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.1])]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="key",
            azure_endpoint="https://example.openai.azure.com/",
            deployment="dep",
        )

        call_kwargs = azure_cls.call_args.kwargs
        self.assertEqual(call_kwargs["azure_endpoint"], "https://example.openai.azure.com/")

    @patch("ali_agentic_adk_python.core.embedding.azure_openai_embedding.AzureOpenAI")
    def test_all_parameters_combined(self, azure_cls):
        client_mock = MagicMock()
        azure_cls.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.1, 0.2])]
        )

        embedding = AzureOpenAIEmbedding(
            api_key="my-api-key",
            azure_endpoint="https://my-resource.openai.azure.com",
            deployment="text-embedding-3-small",
            api_version="2024-06-01",
            dimensions=256,
            user="user-456",
            client_options={"timeout": 60.0},
            request_options={"encoding_format": "float"},
        )
        embedding.embed_documents(["test"])

        azure_call_kwargs = azure_cls.call_args.kwargs
        self.assertEqual(azure_call_kwargs["api_key"], "my-api-key")
        self.assertEqual(azure_call_kwargs["azure_endpoint"], "https://my-resource.openai.azure.com")
        self.assertEqual(azure_call_kwargs["api_version"], "2024-06-01")
        self.assertEqual(azure_call_kwargs["timeout"], 60.0)

        create_call_kwargs = client_mock.embeddings.create.call_args.kwargs
        self.assertEqual(create_call_kwargs["model"], "text-embedding-3-small")
        self.assertEqual(create_call_kwargs["dimensions"], 256)
        self.assertEqual(create_call_kwargs["user"], "user-456")
        self.assertEqual(create_call_kwargs["encoding_format"], "float")


if __name__ == "__main__":
    unittest.main()
