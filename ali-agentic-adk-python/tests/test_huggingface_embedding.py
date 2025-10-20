import unittest
from unittest.mock import MagicMock, patch

from ali_agentic_adk_python.core.common.exceptions import EmbeddingProviderError
from ali_agentic_adk_python.core.embedding.huggingface_embedding import HuggingFaceEmbedding


class HuggingFaceEmbeddingTestCase(unittest.TestCase):
    def test_missing_api_key_raises(self):
        with self.assertRaises(ValueError):
            HuggingFaceEmbedding(api_key="")

    @patch("ali_agentic_adk_python.core.embedding.huggingface_embedding.requests")
    def test_embed_documents_returns_vectors(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = [[0.1, 0.2], [0.3, 0.4]]
        requests_module.post.return_value = response_mock

        embedding = HuggingFaceEmbedding(api_key="token", model="my-model")
        vectors = embedding.embed_documents(["hello", "world"])

        self.assertEqual(vectors, [[0.1, 0.2], [0.3, 0.4]])
        requests_module.post.assert_called_once()
        kwargs = requests_module.post.call_args.kwargs
        self.assertEqual(kwargs["json"], {"inputs": ["hello", "world"]})
        self.assertIn("Authorization", kwargs["headers"])
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer token")

    @patch("ali_agentic_adk_python.core.embedding.huggingface_embedding.requests")
    def test_request_options_are_forwarded(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = [[0.5, 0.6]]
        requests_module.post.return_value = response_mock

        embedding = HuggingFaceEmbedding(
            api_key="token",
            model="sentence-transformers/all-MiniLM-L6-v2",
            base_url="https://example.com/api/models",
            timeout=5.0,
            headers={"X-Test": "value"},
            request_options={"parameters": {"truncate": True}},
        )

        embedding.embed_documents(["sample"])

        requests_module.post.assert_called_once()
        kwargs = requests_module.post.call_args.kwargs
        self.assertEqual(kwargs["json"], {"inputs": ["sample"], "parameters": {"truncate": True}})
        self.assertEqual(kwargs["timeout"], 5.0)
        self.assertEqual(kwargs["headers"]["X-Test"], "value")
        self.assertEqual(
            kwargs["headers"]["Authorization"],
            "Bearer token",
        )
        self.assertEqual(
            requests_module.post.call_args.args[0],
            "https://example.com/api/models/sentence-transformers/all-MiniLM-L6-v2",
        )

    @patch("ali_agentic_adk_python.core.embedding.huggingface_embedding.requests")
    def test_missing_vectors_raise(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"error": "Model loading"}
        requests_module.post.return_value = response_mock

        embedding = HuggingFaceEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.huggingface_embedding.requests")
    def test_request_exception_is_wrapped(self, requests_module):
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = RuntimeError
        requests_module.post.side_effect = requests_module.exceptions.RequestException("boom")

        embedding = HuggingFaceEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.huggingface_embedding.requests")
    def test_embed_query_returns_single_vector(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = [0.9, 0.8, 0.7]
        requests_module.post.return_value = response_mock

        embedding = HuggingFaceEmbedding(api_key="token", model="test-model")
        vector = embedding.embed_query("query text")

        self.assertEqual(vector, [0.9, 0.8, 0.7])


if __name__ == "__main__":
    unittest.main()
