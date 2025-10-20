import unittest
from unittest.mock import MagicMock, patch

from ali_agentic_adk_python.core.common.exceptions import EmbeddingProviderError
from ali_agentic_adk_python.core.embedding.sensenova_embedding import SenseNovaEmbedding


class SenseNovaEmbeddingTestCase(unittest.TestCase):
    def test_missing_access_key_id_raises(self):
        with self.assertRaises(ValueError):
            SenseNovaEmbedding(access_key_id="", secret_access_key="secret")

    def test_missing_secret_access_key_raises(self):
        with self.assertRaises(ValueError):
            SenseNovaEmbedding(access_key_id="access", secret_access_key="")

    def test_none_access_key_id_raises(self):
        with self.assertRaises(ValueError):
            SenseNovaEmbedding(access_key_id=None, secret_access_key="secret")

    def test_none_secret_access_key_raises(self):
        with self.assertRaises(ValueError):
            SenseNovaEmbedding(access_key_id="access", secret_access_key=None)

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_embed_documents_returns_vectors(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "encoded-token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [
                {"embedding": [0.1, 0.2]},
                {"embedding": [0.3, 0.4]},
            ]
        }
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(
            access_key_id="access",
            secret_access_key="secret",
            model="nova-embedding-lite",
        )
        vectors = embedding.embed_documents(["文本1", "文本2"])

        self.assertEqual(vectors, [[0.1, 0.2], [0.3, 0.4]])
        requests_module.post.assert_called_once()
        args, kwargs = requests_module.post.call_args
        self.assertEqual(args[0], "https://api.sensenova.cn/v1/llm/embeddings")
        self.assertEqual(
            kwargs["json"], {"model": "nova-embedding-lite", "input": ["文本1", "文本2"]}
        )
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer encoded-token")

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_empty_input_returns_empty_list(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")
        vectors = embedding.embed_documents([])

        self.assertEqual(vectors, [])
        requests_module.post.assert_not_called()

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_request_options_forwarded(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [1.0, 2.0]}]}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        request_options = {"truncate": "start", "encoding_format": "float16"}
        embedding = SenseNovaEmbedding(
            access_key_id="ak",
            secret_access_key="sk",
            model="nova-embedding-v1",
            request_options=request_options,
        )

        request_options["new"] = "value"

        embedding.embed_documents(["示例文本"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(
            kwargs["json"],
            {
                "model": "nova-embedding-v1",
                "input": ["示例文本"],
                "truncate": "start",
                "encoding_format": "float16",
            },
        )
        self.assertNotIn("new", kwargs["json"])

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_custom_headers_merge_and_copy(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.5]}]}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        custom_headers = {"X-Test": "value"}
        embedding = SenseNovaEmbedding(
            access_key_id="ak",
            secret_access_key="sk",
            headers=custom_headers,
        )

        custom_headers["X-New"] = "should-not-appear"
        embedding.embed_documents(["text"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(kwargs["headers"]["X-Test"], "value")
        self.assertNotIn("X-New", kwargs["headers"])
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer token")

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_request_exception_is_wrapped(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = RuntimeError
        requests_module.post.side_effect = RuntimeError("network error")

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_json_parse_error_is_wrapped(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.side_effect = ValueError("invalid json")
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_missing_vectors_raise(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": []}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_non_numeric_vectors_raise(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": ["oops"]}]}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_jwt_encode_bytes_are_decoded(self, requests_module, jwt_module):
        jwt_module.encode.return_value = b"byte-token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")
        embedding.embed_documents(["text"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer byte-token")

    def test_missing_requests_dependency_raises(self):
        with patch(
            "ali_agentic_adk_python.core.embedding.sensenova_embedding.requests", None
        ):
            with self.assertRaises(ImportError):
                SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")

    def test_missing_jwt_dependency_raises(self):
        with patch(
            "ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt", None
        ):
            with self.assertRaises(ImportError):
                SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_custom_endpoint(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1, 0.2]}]}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(
            access_key_id="ak",
            secret_access_key="sk",
            endpoint="https://custom.api.com/embeddings",
        )
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(args[0], "https://custom.api.com/embeddings")

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_default_endpoint(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(args[0], "https://api.sensenova.cn/v1/llm/embeddings")

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_timeout_parameter(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(
            access_key_id="ak", secret_access_key="sk", timeout=10.5
        )
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(kwargs["timeout"], 10.5)

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_tuple_timeout_parameter(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(
            access_key_id="ak", secret_access_key="sk", timeout=(5.0, 15.0)
        )
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(kwargs["timeout"], (5.0, 15.0))

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_default_model_parameter(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(kwargs["json"]["model"], "nova-embedding-v1")

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_response_with_embeddings_field(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "embeddings": [
                {"embedding": [1.0, 2.0]},
                {"embedding": [3.0, 4.0]},
            ]
        }
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")
        vectors = embedding.embed_documents(["test1", "test2"])

        self.assertEqual(vectors, [[1.0, 2.0], [3.0, 4.0]])

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_response_with_vector_field(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [
                {"vector": [5.0, 6.0]},
                {"vector": [7.0, 8.0]},
            ]
        }
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")
        vectors = embedding.embed_documents(["test1", "test2"])

        self.assertEqual(vectors, [[5.0, 6.0], [7.0, 8.0]])

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_response_as_list_format(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = [
            [1.0, 2.0, 3.0],
            [4.0, 5.0, 6.0],
        ]
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")
        vectors = embedding.embed_documents(["test1", "test2"])

        self.assertEqual(vectors, [[1.0, 2.0, 3.0], [4.0, 5.0, 6.0]])

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_http_error_is_wrapped(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception
        response_mock.raise_for_status.side_effect = Exception("HTTP 500")
        requests_module.post.return_value = response_mock

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_integer_vector_values_coerced_to_float(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [1, 2, 3]}]}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")
        vectors = embedding.embed_documents(["test"])

        self.assertEqual(vectors, [[1.0, 2.0, 3.0]])
        self.assertIsInstance(vectors[0][0], float)
        self.assertIsInstance(vectors[0][1], float)
        self.assertIsInstance(vectors[0][2], float)

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_mixed_int_float_vector_values(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [1, 2.5, 3, 4.8]}]}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")
        vectors = embedding.embed_documents(["test"])

        self.assertEqual(vectors, [[1.0, 2.5, 3.0, 4.8]])

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_none_payload_raises(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = None
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_incomplete_data_raises(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [
                {"embedding": [1.0, 2.0]},
                {"wrong_key": [3.0, 4.0]},
            ]
        }
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test1", "test2"])

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_response_with_tuple_vectors(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = [(1.0, 2.0), (3.0, 4.0)]
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")
        vectors = embedding.embed_documents(["test1", "test2"])

        self.assertEqual(vectors, [[1.0, 2.0], [3.0, 4.0]])
        self.assertIsInstance(vectors[0], list)
        self.assertIsInstance(vectors[1], list)

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_invalid_item_type_in_data_raises(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [{"embedding": [1.0, 2.0]}, "invalid_string_item"]
        }
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test1", "test2"])

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.time")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_jwt_token_generation(self, requests_module, jwt_module, time_module):
        time_module.time.return_value = 1000000
        jwt_module.encode.return_value = "generated-token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(
            access_key_id="my-access-key", secret_access_key="my-secret-key"
        )
        embedding.embed_documents(["test"])

        jwt_module.encode.assert_called_once()
        call_args = jwt_module.encode.call_args
        payload = call_args[0][0]
        secret = call_args[0][1]
        algorithm = call_args[1]["algorithm"]

        self.assertEqual(payload["iss"], "my-access-key")
        self.assertEqual(payload["exp"], 1000000 + 1800)
        self.assertEqual(payload["nbf"], 1000000 - 5)
        self.assertEqual(secret, "my-secret-key")
        self.assertEqual(algorithm, "HS256")

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_content_type_and_accept_headers(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(kwargs["headers"]["Content-Type"], "application/json")
        self.assertEqual(kwargs["headers"]["Accept"], "application/json")

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_embed_query(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.9, 0.8, 0.7]}]}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")
        vector = embedding.embed_query("query text")

        self.assertEqual(vector, [0.9, 0.8, 0.7])

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_large_batch_of_documents(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_data = {
            "data": [{"embedding": [float(i), float(i + 1)]} for i in range(50)]
        }
        response_mock.json.return_value = response_data
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")
        texts = [f"text_{i}" for i in range(50)]
        vectors = embedding.embed_documents(texts)

        self.assertEqual(len(vectors), 50)
        for i, vector in enumerate(vectors):
            self.assertEqual(vector, [float(i), float(i + 1)])

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_varying_vector_dimensions(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [
                {"embedding": [1.0, 2.0]},
                {"embedding": [3.0, 4.0, 5.0, 6.0]},
                {"embedding": [7.0]},
            ]
        }
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")
        vectors = embedding.embed_documents(["t1", "t2", "t3"])

        self.assertEqual(len(vectors), 3)
        self.assertEqual(len(vectors[0]), 2)
        self.assertEqual(len(vectors[1]), 4)
        self.assertEqual(len(vectors[2]), 1)

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_single_document_processing(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.5, 0.6, 0.7]}]}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")
        vectors = embedding.embed_documents(["single text"])

        self.assertEqual(len(vectors), 1)
        self.assertEqual(vectors[0], [0.5, 0.6, 0.7])

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_model_attribute_accessible(self, requests_module, jwt_module):
        embedding = SenseNovaEmbedding(
            access_key_id="ak", secret_access_key="sk", model="custom-model"
        )

        self.assertEqual(embedding.model, "custom-model")

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_no_request_options_by_default(self, requests_module, jwt_module):
        jwt_module.encode.return_value = "token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(access_key_id="ak", secret_access_key="sk")
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        payload = kwargs["json"]
        self.assertEqual(set(payload.keys()), {"model", "input"})

    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.jwt")
    @patch("ali_agentic_adk_python.core.embedding.sensenova_embedding.requests")
    def test_custom_headers_do_not_override_authorization(
        self, requests_module, jwt_module
    ):
        jwt_module.encode.return_value = "correct-token"
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception

        embedding = SenseNovaEmbedding(
            access_key_id="ak",
            secret_access_key="sk",
            headers={"Authorization": "Bearer should-be-overridden"},
        )
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer correct-token")


if __name__ == "__main__":
    unittest.main()
