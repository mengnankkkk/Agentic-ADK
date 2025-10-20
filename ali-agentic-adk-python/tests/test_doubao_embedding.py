import unittest
from unittest.mock import MagicMock, patch

from ali_agentic_adk_python.core.common.exceptions import EmbeddingProviderError
from ali_agentic_adk_python.core.embedding.doubao_embedding import DoubaoEmbedding


class DoubaoEmbeddingTestCase(unittest.TestCase):
    def test_missing_api_key_raises(self):
        with self.assertRaises(ValueError):
            DoubaoEmbedding(api_key="")

    @patch("ali_agentic_adk_python.core.embedding.doubao_embedding.requests")
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

        embedding = DoubaoEmbedding(api_key="token", model="doubao-embedding-v1")
        vectors = embedding.embed_documents(["你好", "世界"])

        self.assertEqual(vectors, [[0.1, 0.2], [0.3, 0.4]])
        requests_module.post.assert_called_once()
        args, kwargs = requests_module.post.call_args
        self.assertEqual(args[0], "https://ark.cn-beijing.volces.com/api/v3/embeddings")
        self.assertEqual(kwargs["json"], {"model": "doubao-embedding-v1", "input": ["你好", "世界"]})
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer token")

    @patch("ali_agentic_adk_python.core.embedding.doubao_embedding.requests")
    def test_request_options_are_forwarded(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.5, 0.6]}]}
        requests_module.post.return_value = response_mock

        embedding = DoubaoEmbedding(
            api_key="token",
            model="custom-model",
            endpoint="https://example.com/v3/embeddings",
            timeout=3.8,
            headers={"X-Test": "value"},
            request_options={"encoding_format": "float"},
        )

        embedding.embed_documents(["示例"])

        requests_module.post.assert_called_once()
        args, kwargs = requests_module.post.call_args
        self.assertEqual(args[0], "https://example.com/v3/embeddings")
        self.assertEqual(
            kwargs["json"],
            {"model": "custom-model", "input": ["示例"], "encoding_format": "float"},
        )
        self.assertEqual(kwargs["timeout"], 3.8)
        self.assertEqual(kwargs["headers"]["X-Test"], "value")

    @patch("ali_agentic_adk_python.core.embedding.doubao_embedding.requests")
    def test_missing_vectors_raise(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": []}
        requests_module.post.return_value = response_mock

        embedding = DoubaoEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["文本"])

    @patch("ali_agentic_adk_python.core.embedding.doubao_embedding.requests")
    def test_request_exception_is_wrapped(self, requests_module):
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = RuntimeError
        requests_module.post.side_effect = RuntimeError("boom")

        embedding = DoubaoEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.doubao_embedding.requests")
    def test_embed_query_returns_single_vector(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [
                {"embedding": [0.9, 0.8, 0.7]},
            ]
        }
        requests_module.post.return_value = response_mock

        embedding = DoubaoEmbedding(api_key="token")
        vector = embedding.embed_query("查询")

        self.assertEqual(vector, [0.9, 0.8, 0.7])


if __name__ == "__main__":
    unittest.main()
