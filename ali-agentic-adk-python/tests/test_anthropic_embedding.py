import unittest
from unittest.mock import MagicMock, patch

from ali_agentic_adk_python.core.common.exceptions import EmbeddingProviderError
from ali_agentic_adk_python.core.embedding.anthropic_embedding import AnthropicEmbedding


class AnthropicEmbeddingTestCase(unittest.TestCase):
    def test_missing_api_key_raises(self):
        with self.assertRaises(ValueError):
            AnthropicEmbedding(api_key="")

    @patch("ali_agentic_adk_python.core.embedding.anthropic_embedding.anthropic")
    def test_embed_documents_returns_vectors(self, anthropic_module):
        client_mock = MagicMock()
        anthropic_module.Anthropic.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.1, 0.2]), MagicMock(embedding=[0.3, 0.4])]
        )

        embedding = AnthropicEmbedding(api_key="token", model="claude-embed-v1")
        vectors = embedding.embed_documents(["hello", "world"])

        self.assertEqual(vectors, [[0.1, 0.2], [0.3, 0.4]])
        client_mock.embeddings.create.assert_called_once_with(
            model="claude-embed-v1",
            input=["hello", "world"],
        )

    @patch("ali_agentic_adk_python.core.embedding.anthropic_embedding.anthropic")
    def test_request_options_are_forwarded(self, anthropic_module):
        client_mock = MagicMock()
        anthropic_module.Anthropic.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.5, 0.6])]
        )

        embedding = AnthropicEmbedding(
            api_key="token",
            model="custom-model",
            base_url="https://example.com",
            timeout=5.0,
            client_options={"max_retries": 2},
            request_options={"metadata": {"source": "unit-test"}},
        )

        embedding.embed_documents(["sample"])

        anthropic_module.Anthropic.assert_called_once_with(
            api_key="token",
            base_url="https://example.com",
            timeout=5.0,
            max_retries=2,
        )
        client_mock.embeddings.create.assert_called_once_with(
            model="custom-model",
            input=["sample"],
            metadata={"source": "unit-test"},
        )

    @patch("ali_agentic_adk_python.core.embedding.anthropic_embedding.anthropic")
    def test_missing_vectors_raise(self, anthropic_module):
        client_mock = MagicMock()
        anthropic_module.Anthropic.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(data=[])

        embedding = AnthropicEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.anthropic_embedding.anthropic")
    def test_embed_query_returns_single_vector(self, anthropic_module):
        client_mock = MagicMock()
        anthropic_module.Anthropic.return_value = client_mock
        client_mock.embeddings.create.return_value = MagicMock(
            data=[MagicMock(embedding=[0.9, 0.8, 0.7])]
        )

        embedding = AnthropicEmbedding(api_key="token")
        vector = embedding.embed_query("query")

        self.assertEqual(vector, [0.9, 0.8, 0.7])

    @patch("ali_agentic_adk_python.core.embedding.anthropic_embedding.anthropic")
    def test_request_exception_is_wrapped(self, anthropic_module):
        client_mock = MagicMock()
        anthropic_module.Anthropic.return_value = client_mock
        client_mock.embeddings.create.side_effect = RuntimeError("boom")

        embedding = AnthropicEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])


if __name__ == "__main__":
    unittest.main()
