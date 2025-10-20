import unittest
from unittest.mock import MagicMock, patch

from ali_agentic_adk_python.core.common.exceptions import EmbeddingProviderError
from ali_agentic_adk_python.core.embedding.jina_embedding import JinaEmbedding


class JinaEmbeddingTestCase(unittest.TestCase):
    def test_missing_api_key_raises(self):
        with self.assertRaises(ValueError):
            JinaEmbedding(api_key="")

    def test_none_api_key_raises(self):
        with self.assertRaises(ValueError):
            JinaEmbedding(api_key=None)

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_embed_documents_returns_vectors(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [
                {"embedding": [0.11, 0.22]},
                {"embedding": [0.33, 0.44]},
            ]
        }
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token", model="jina-embeddings-v3")
        vectors = embedding.embed_documents(["hello", "world"])

        self.assertEqual(vectors, [[0.11, 0.22], [0.33, 0.44]])
        requests_module.post.assert_called_once()
        args, kwargs = requests_module.post.call_args
        self.assertEqual(args[0], "https://api.jina.ai/v1/embeddings")
        self.assertEqual(kwargs["json"], {"model": "jina-embeddings-v3", "input": ["hello", "world"]})
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer token")

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_request_options_are_forwarded(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.5, 0.6]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(
            api_key="token",
            model="text-multilingual",
            endpoint="https://example.com/embeddings",
            timeout=3.5,
            headers={"X-Test": "value"},
            request_options={"dimensions": 1024},
        )

        embedding.embed_documents(["sample"])

        requests_module.post.assert_called_once()
        args, kwargs = requests_module.post.call_args
        self.assertEqual(args[0], "https://example.com/embeddings")
        self.assertEqual(
            kwargs["json"],
            {"model": "text-multilingual", "input": ["sample"], "dimensions": 1024},
        )
        self.assertEqual(kwargs["timeout"], 3.5)
        self.assertEqual(kwargs["headers"]["X-Test"], "value")

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_missing_vectors_raise(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": []}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_request_exception_is_wrapped(self, requests_module):
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = RuntimeError
        requests_module.post.side_effect = requests_module.exceptions.RequestException("boom")

        embedding = JinaEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_embed_query_returns_single_vector(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [
                {"embedding": [0.9, 0.8, 0.7]},
            ]
        }
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")
        vector = embedding.embed_query("query")

        self.assertEqual(vector, [0.9, 0.8, 0.7])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_empty_input_returns_empty_list(self, requests_module):
        embedding = JinaEmbedding(api_key="token")
        vectors = embedding.embed_documents([])

        self.assertEqual(vectors, [])
        requests_module.post.assert_not_called()

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_json_parse_error_is_wrapped(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.side_effect = ValueError("invalid json")
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_http_error_is_wrapped(self, requests_module):
        response_mock = MagicMock()
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception
        response_mock.raise_for_status.side_effect = Exception("HTTP 500")
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_default_endpoint(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1, 0.2]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(args[0], "https://api.jina.ai/v1/embeddings")

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_custom_endpoint(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1, 0.2]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(
            api_key="token", endpoint="https://custom.jina.ai/v2/embeddings"
        )
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(args[0], "https://custom.jina.ai/v2/embeddings")

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_default_model_parameter(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1, 0.2]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(kwargs["json"]["model"], "jina-embeddings-v3")

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_timeout_parameter(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token", timeout=12.0)
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(kwargs["timeout"], 12.0)

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_tuple_timeout_parameter(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token", timeout=(5.0, 20.0))
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(kwargs["timeout"], (5.0, 20.0))

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_none_timeout_parameter(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token", timeout=None)
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertIsNone(kwargs["timeout"])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_custom_headers_merge_with_defaults(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(
            api_key="token", headers={"X-Custom": "value", "X-Another": "header"}
        )
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer token")
        self.assertEqual(kwargs["headers"]["Accept"], "application/json")
        self.assertEqual(kwargs["headers"]["X-Custom"], "value")
        self.assertEqual(kwargs["headers"]["X-Another"], "header")

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_request_options_copied_not_mutated(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        original_options = {"param1": "value1", "param2": "value2"}
        embedding = JinaEmbedding(api_key="token", request_options=original_options)

        original_options["param3"] = "value3"

        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertNotIn("param3", kwargs["json"])
        self.assertIn("param1", kwargs["json"])
        self.assertIn("param2", kwargs["json"])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_response_with_embeddings_field(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "embeddings": [
                {"embedding": [1.0, 2.0]},
                {"embedding": [3.0, 4.0]},
            ]
        }
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")
        vectors = embedding.embed_documents(["test1", "test2"])

        self.assertEqual(vectors, [[1.0, 2.0], [3.0, 4.0]])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_response_with_vector_field(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [
                {"vector": [5.0, 6.0]},
                {"vector": [7.0, 8.0]},
            ]
        }
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")
        vectors = embedding.embed_documents(["test1", "test2"])

        self.assertEqual(vectors, [[5.0, 6.0], [7.0, 8.0]])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_response_as_list_format(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = [
            [1.0, 2.0, 3.0],
            [4.0, 5.0, 6.0],
        ]
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")
        vectors = embedding.embed_documents(["test1", "test2"])

        self.assertEqual(vectors, [[1.0, 2.0, 3.0], [4.0, 5.0, 6.0]])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_none_payload_raises(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = None
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_incomplete_data_raises(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [
                {"embedding": [1.0, 2.0]},
                {"wrong_key": [3.0, 4.0]},
            ]
        }
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test1", "test2"])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_non_numeric_vector_raises(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": ["invalid", "data"]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_integer_vector_values_coerced_to_float(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [1, 2, 3]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")
        vectors = embedding.embed_documents(["test"])

        self.assertEqual(vectors, [[1.0, 2.0, 3.0]])
        self.assertIsInstance(vectors[0][0], float)
        self.assertIsInstance(vectors[0][1], float)
        self.assertIsInstance(vectors[0][2], float)

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_mixed_int_float_vector_values(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [1, 2.5, 3, 4.8]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")
        vectors = embedding.embed_documents(["test"])

        self.assertEqual(vectors, [[1.0, 2.5, 3.0, 4.8]])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_response_with_tuple_vectors(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = [(1.0, 2.0), (3.0, 4.0)]
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")
        vectors = embedding.embed_documents(["test1", "test2"])

        self.assertEqual(vectors, [[1.0, 2.0], [3.0, 4.0]])
        self.assertIsInstance(vectors[0], list)
        self.assertIsInstance(vectors[1], list)

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_invalid_item_type_in_data_raises(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [{"embedding": [1.0, 2.0]}, "invalid_string_item"]
        }
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test1", "test2"])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_large_batch_of_documents(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_data = {
            "data": [{"embedding": [float(i), float(i + 1)]} for i in range(100)]
        }
        response_mock.json.return_value = response_data
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")
        texts = [f"text_{i}" for i in range(100)]
        vectors = embedding.embed_documents(texts)

        self.assertEqual(len(vectors), 100)
        for i, vector in enumerate(vectors):
            self.assertEqual(vector, [float(i), float(i + 1)])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_varying_vector_dimensions(self, requests_module):
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

        embedding = JinaEmbedding(api_key="token")
        vectors = embedding.embed_documents(["t1", "t2", "t3"])

        self.assertEqual(len(vectors), 3)
        self.assertEqual(len(vectors[0]), 2)
        self.assertEqual(len(vectors[1]), 4)
        self.assertEqual(len(vectors[2]), 1)

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_single_document_processing(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.5, 0.6, 0.7]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")
        vectors = embedding.embed_documents(["single text"])

        self.assertEqual(len(vectors), 1)
        self.assertEqual(vectors[0], [0.5, 0.6, 0.7])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_model_attribute_accessible(self, requests_module):
        embedding = JinaEmbedding(api_key="token", model="jina-embeddings-v2")

        self.assertEqual(embedding.model, "jina-embeddings-v2")

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_no_request_options_by_default(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        payload = kwargs["json"]
        self.assertEqual(set(payload.keys()), {"model", "input"})

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_accept_header(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(kwargs["headers"]["Accept"], "application/json")

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_authorization_header_format(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="jina-api-key-123")
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer jina-api-key-123")

    def test_missing_requests_dependency_raises(self):
        with patch(
            "ali_agentic_adk_python.core.embedding.jina_embedding.requests", None
        ):
            with self.assertRaises(ImportError):
                JinaEmbedding(api_key="token")

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_multiple_request_options(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(
            api_key="token",
            request_options={
                "task": "text-matching",
                "dimensions": 768,
                "late_chunking": True,
            },
        )
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        payload = kwargs["json"]
        self.assertEqual(payload["task"], "text-matching")
        self.assertEqual(payload["dimensions"], 768)
        self.assertEqual(payload["late_chunking"], True)

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_custom_headers_do_not_remove_defaults(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token", headers={"X-Custom": "header"})
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        headers = kwargs["headers"]
        self.assertIn("Authorization", headers)
        self.assertIn("Accept", headers)
        self.assertIn("X-Custom", headers)

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_empty_data_field_raises(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"other_field": "value"}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_request_with_encoding_format_option(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1, 0.2]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(
            api_key="token", request_options={"encoding_format": "float"}
        )
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(kwargs["json"]["encoding_format"], "float")

    @patch("ali_agentic_adk_python.core.embedding.jina_embedding.requests")
    def test_custom_model_name(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = JinaEmbedding(
            api_key="token", model="jina-embeddings-v2-base-zh"
        )
        embedding.embed_documents(["test"])

        args, kwargs = requests_module.post.call_args
        self.assertEqual(kwargs["json"]["model"], "jina-embeddings-v2-base-zh")


if __name__ == "__main__":
    unittest.main()
