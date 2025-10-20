import unittest
from unittest.mock import MagicMock, patch

from ali_agentic_adk_python.core.common.exceptions import EmbeddingProviderError
from ali_agentic_adk_python.core.embedding.qianfan_embedding import QianfanEmbedding


class QianfanEmbeddingTestCase(unittest.TestCase):
    def test_missing_credentials_raise(self):
        with self.assertRaises(ValueError):
            QianfanEmbedding()

    @patch("ali_agentic_adk_python.core.embedding.qianfan_embedding.requests")
    def test_embed_documents_with_auto_token(self, requests_module):
        token_response = MagicMock()
        token_response.raise_for_status.return_value = None
        token_response.json.return_value = {"access_token": "token123", "expires_in": 3600}

        embedding_response = MagicMock()
        embedding_response.raise_for_status.return_value = None
        embedding_response.json.return_value = {
            "data": [
                {"embedding": [0.1, 0.2]},
                {"embedding": [0.3, 0.4]},
            ]
        }

        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = RuntimeError
        requests_module.post.side_effect = [token_response, embedding_response]

        embedding = QianfanEmbedding(api_key="ak", secret_key="sk", model="embedding-v1")
        vectors = embedding.embed_documents(["你好", "世界"])

        self.assertEqual(vectors, [[0.1, 0.2], [0.3, 0.4]])
        self.assertEqual(requests_module.post.call_count, 2)

        token_call = requests_module.post.call_args_list[0]
        self.assertEqual(token_call.kwargs["params"], {
            "grant_type": "client_credentials",
            "client_id": "ak",
            "client_secret": "sk",
        })

        embed_call = requests_module.post.call_args_list[1]
        self.assertIn("access_token=token123", embed_call.args[0])
        self.assertEqual(embed_call.kwargs["json"], {"input": ["你好", "世界"]})

    @patch("ali_agentic_adk_python.core.embedding.qianfan_embedding.requests")
    def test_request_options_and_headers_forwarded(self, requests_module):
        token_response = MagicMock()
        token_response.raise_for_status.return_value = None
        token_response.json.return_value = {"access_token": "token123", "expires_in": 3600}

        embedding_response = MagicMock()
        embedding_response.raise_for_status.return_value = None
        embedding_response.json.return_value = {"data": [{"embedding": [0.5, 0.6]}]}

        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = RuntimeError
        requests_module.post.side_effect = [token_response, embedding_response]

        embedding = QianfanEmbedding(
            api_key="ak",
            secret_key="sk",
            headers={"X-Test": "value"},
            timeout=5.5,
            request_options={"user_id": "alice"},
        )
        embedding.embed_documents(["示例"])

        embed_call = requests_module.post.call_args_list[1]
        self.assertEqual(
            embed_call.kwargs["json"],
            {"input": ["示例"], "user_id": "alice"},
        )
        self.assertEqual(embed_call.kwargs["timeout"], 5.5)
        self.assertEqual(embed_call.kwargs["headers"]["X-Test"], "value")

    @patch("ali_agentic_adk_python.core.embedding.qianfan_embedding.requests")
    def test_missing_vectors_raise(self, requests_module):
        token_response = MagicMock()
        token_response.raise_for_status.return_value = None
        token_response.json.return_value = {"access_token": "token123"}

        embedding_response = MagicMock()
        embedding_response.raise_for_status.return_value = None
        embedding_response.json.return_value = {"data": []}

        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = RuntimeError
        requests_module.post.side_effect = [token_response, embedding_response]

        embedding = QianfanEmbedding(api_key="ak", secret_key="sk")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["文本"])

    @patch("ali_agentic_adk_python.core.embedding.qianfan_embedding.requests")
    def test_token_refresh_on_error_code(self, requests_module):
        token_response_1 = MagicMock()
        token_response_1.raise_for_status.return_value = None
        token_response_1.json.return_value = {"access_token": "token123", "expires_in": 3600}

        failed_embedding = MagicMock()
        failed_embedding.raise_for_status.return_value = None
        failed_embedding.json.return_value = {"error_code": 110}

        token_response_2 = MagicMock()
        token_response_2.raise_for_status.return_value = None
        token_response_2.json.return_value = {"access_token": "token456", "expires_in": 3600}

        success_embedding = MagicMock()
        success_embedding.raise_for_status.return_value = None
        success_embedding.json.return_value = {"data": [{"embedding": [0.7, 0.8, 0.9]}]}

        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = RuntimeError
        requests_module.post.side_effect = [
            token_response_1,
            failed_embedding,
            token_response_2,
            success_embedding,
        ]

        embedding = QianfanEmbedding(api_key="ak", secret_key="sk")
        vector = embedding.embed_query("查询")

        self.assertEqual(vector, [0.7, 0.8, 0.9])
        self.assertEqual(requests_module.post.call_args_list[2].kwargs["params"]["client_id"], "ak")
        self.assertIn("access_token=token456", requests_module.post.call_args_list[3].args[0])

    @patch("ali_agentic_adk_python.core.embedding.qianfan_embedding.requests")
    def test_access_token_only_usage(self, requests_module):
        embedding_response = MagicMock()
        embedding_response.raise_for_status.return_value = None
        embedding_response.json.return_value = {"data": [{"embedding": [0.2, 0.4]}]}

        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = RuntimeError
        requests_module.post.return_value = embedding_response

        embedding = QianfanEmbedding(access_token="static-token", model="embedding-v1")
        self.assertEqual(embedding.embed_query("hi"), [0.2, 0.4])
        requests_module.post.assert_called_once()
        self.assertIn("access_token=static-token", requests_module.post.call_args.args[0])

    @patch("ali_agentic_adk_python.core.embedding.qianfan_embedding.requests")
    def test_request_exception_wrapped(self, requests_module):
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = RuntimeError
        requests_module.post.side_effect = RuntimeError("boom")

        embedding = QianfanEmbedding(access_token="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])


if __name__ == "__main__":
    unittest.main()
