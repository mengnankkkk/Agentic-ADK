import unittest
from unittest.mock import MagicMock, patch

from ali_agentic_adk_python.core.common.exceptions import EmbeddingProviderError
from ali_agentic_adk_python.core.embedding.minimax_embedding import MiniMaxEmbedding


class MiniMaxEmbeddingTestCase(unittest.TestCase):
    def test_missing_api_key_raises(self):
        with self.assertRaises(ValueError):
            MiniMaxEmbedding(api_key="")

    @patch("ali_agentic_adk_python.core.embedding.minimax_embedding.requests")
    def test_embed_documents_returns_vectors(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [
                {"embedding": [0.1, 0.2]},
                {"embedding": [0.3, 0.4]},
            ]
        }
        requests_module.post.return_value = response_mock

        embedding = MiniMaxEmbedding(api_key="token", model="text-embedding-01")
        vectors = embedding.embed_documents(["你好", "世界"])

        self.assertEqual(vectors, [[0.1, 0.2], [0.3, 0.4]])
        requests_module.post.assert_called_once()
        args, kwargs = requests_module.post.call_args
        self.assertEqual(args[0], "https://api.minimaxi.com/v1/embeddings")
        self.assertEqual(kwargs["json"], {"model": "text-embedding-01", "input": ["你好", "世界"]})
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer token")

    @patch("ali_agentic_adk_python.core.embedding.minimax_embedding.requests")
    def test_request_options_are_forwarded(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.5, 0.6]}]}
        requests_module.post.return_value = response_mock

        embedding = MiniMaxEmbedding(
            api_key="token",
            model="text-embedding-01",
            endpoint="https://example.com/embeddings",
            timeout=3.25,
            headers={"X-Test": "value"},
            request_options={"user_id": "alice"},
        )

        embedding.embed_documents(["示例"])

        requests_module.post.assert_called_once()
        args, kwargs = requests_module.post.call_args
        self.assertEqual(args[0], "https://example.com/embeddings")
        self.assertEqual(
            kwargs["json"],
            {"model": "text-embedding-01", "input": ["示例"], "user_id": "alice"},
        )
        self.assertEqual(kwargs["timeout"], 3.25)
        self.assertEqual(kwargs["headers"]["X-Test"], "value")

    @patch("ali_agentic_adk_python.core.embedding.minimax_embedding.requests")
    def test_missing_vectors_raise(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": []}
        requests_module.post.return_value = response_mock

        embedding = MiniMaxEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["文本"])

    @patch("ali_agentic_adk_python.core.embedding.minimax_embedding.requests")
    def test_request_exception_is_wrapped(self, requests_module):
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = RuntimeError
        requests_module.post.side_effect = requests_module.exceptions.RequestException("boom")

        embedding = MiniMaxEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["文本"])

    @patch("ali_agentic_adk_python.core.embedding.minimax_embedding.requests")
    def test_embed_query_returns_single_vector(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [
                {"embedding": [0.9, 0.8, 0.7]},
            ]
        }
        requests_module.post.return_value = response_mock

        embedding = MiniMaxEmbedding(api_key="token")
        vector = embedding.embed_query("查询")

        self.assertEqual(vector, [0.9, 0.8, 0.7])


if __name__ == "__main__":
    unittest.main()
