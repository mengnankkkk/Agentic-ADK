import unittest
from unittest.mock import MagicMock, patch

from ali_agentic_adk_python.core.common.exceptions import EmbeddingProviderError
from ali_agentic_adk_python.core.embedding.siliconflow_embedding import SiliconFlowEmbedding


class SiliconFlowEmbeddingTestCase(unittest.TestCase):
    def test_missing_api_key_raises(self):
        with self.assertRaises(ValueError):
            SiliconFlowEmbedding(api_key="")

    def test_none_api_key_raises(self):
        with self.assertRaises(ValueError):
            SiliconFlowEmbedding(api_key=None)

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
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

        embedding = SiliconFlowEmbedding(api_key="token", model="my-embedding-model")
        vectors = embedding.embed_documents(["hello", "world"])

        self.assertEqual(vectors, [[0.1, 0.2], [0.3, 0.4]])
        requests_module.post.assert_called_once()
        kwargs = requests_module.post.call_args.kwargs
        self.assertEqual(
            kwargs["json"],
            {"model": "my-embedding-model", "input": ["hello", "world"]},
        )
        self.assertIn("Authorization", kwargs["headers"])
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer token")

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_request_options_are_forwarded(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.5, 0.6]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(
            api_key="token",
            model="internlm2.5-embedding",
            base_url="https://example.com/api",
            timeout=3.5,
            headers={"X-Test": "value"},
            request_options={"user": "tester"},
        )

        embedding.embed_documents(["sample"])

        requests_module.post.assert_called_once()
        args = requests_module.post.call_args
        self.assertEqual(args.args[0], "https://example.com/api/embeddings")
        kwargs = args.kwargs
        self.assertEqual(
            kwargs["json"],
            {"model": "internlm2.5-embedding", "input": ["sample"], "user": "tester"},
        )
        self.assertEqual(kwargs["timeout"], 3.5)
        self.assertEqual(kwargs["headers"]["X-Test"], "value")
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer token")

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_missing_vectors_raise(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": []}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_request_exception_is_wrapped(self, requests_module):
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = RuntimeError
        requests_module.post.side_effect = requests_module.exceptions.RequestException("boom")

        embedding = SiliconFlowEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_empty_input_returns_empty_list(self, requests_module):
        embedding = SiliconFlowEmbedding(api_key="token")
        vectors = embedding.embed_documents([])

        self.assertEqual(vectors, [])
        requests_module.post.assert_not_called()

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_json_parse_error_is_wrapped(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.side_effect = ValueError("invalid json")
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_http_error_is_wrapped(self, requests_module):
        response_mock = MagicMock()
        requests_module.exceptions = MagicMock()
        requests_module.exceptions.RequestException = Exception
        response_mock.raise_for_status.side_effect = Exception("HTTP 500")
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_default_base_url(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1, 0.2]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")
        embedding.embed_documents(["test"])

        args = requests_module.post.call_args
        self.assertEqual(args.args[0], "https://api.siliconflow.cn/v1/embeddings")

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_custom_base_url_strips_trailing_slash(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1, 0.2]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(
            api_key="token", base_url="https://custom.api.com/v2/"
        )
        embedding.embed_documents(["test"])

        args = requests_module.post.call_args
        self.assertEqual(args.args[0], "https://custom.api.com/v2/embeddings")

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_default_model_parameter(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1, 0.2]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")
        embedding.embed_documents(["test"])

        kwargs = requests_module.post.call_args.kwargs
        self.assertEqual(kwargs["json"]["model"], "internlm2.5-embedding")

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_timeout_parameter(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token", timeout=15.0)
        embedding.embed_documents(["test"])

        kwargs = requests_module.post.call_args.kwargs
        self.assertEqual(kwargs["timeout"], 15.0)

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_tuple_timeout_parameter(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token", timeout=(3.0, 10.0))
        embedding.embed_documents(["test"])

        kwargs = requests_module.post.call_args.kwargs
        self.assertEqual(kwargs["timeout"], (3.0, 10.0))

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_none_timeout_parameter(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token", timeout=None)
        embedding.embed_documents(["test"])

        kwargs = requests_module.post.call_args.kwargs
        self.assertIsNone(kwargs["timeout"])

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_custom_headers_merge_with_defaults(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(
            api_key="token", headers={"X-Custom": "value", "X-Another": "header"}
        )
        embedding.embed_documents(["test"])

        kwargs = requests_module.post.call_args.kwargs
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer token")
        self.assertEqual(kwargs["headers"]["Content-Type"], "application/json")
        self.assertEqual(kwargs["headers"]["X-Custom"], "value")
        self.assertEqual(kwargs["headers"]["X-Another"], "header")

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_request_options_copied_not_mutated(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        original_options = {"param1": "value1", "param2": "value2"}
        embedding = SiliconFlowEmbedding(
            api_key="token", request_options=original_options
        )

        original_options["param3"] = "value3"

        embedding.embed_documents(["test"])

        kwargs = requests_module.post.call_args.kwargs
        self.assertNotIn("param3", kwargs["json"])
        self.assertIn("param1", kwargs["json"])
        self.assertIn("param2", kwargs["json"])

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_response_with_vector_field(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [
                {"vector": [1.0, 2.0]},
                {"vector": [3.0, 4.0]},
            ]
        }
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")
        vectors = embedding.embed_documents(["test1", "test2"])

        self.assertEqual(vectors, [[1.0, 2.0], [3.0, 4.0]])

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_non_dict_payload_raises(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = "invalid string response"
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_non_list_data_field_raises(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": "invalid"}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_non_dict_item_in_data_raises(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [{"embedding": [1.0, 2.0]}, "invalid_item"]
        }
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test1", "test2"])

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_missing_embedding_and_vector_fields_raises(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [
                {"embedding": [1.0, 2.0]},
                {"wrong_field": [3.0, 4.0]},
            ]
        }
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test1", "test2"])

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_non_numeric_vector_values_raise(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": ["invalid", "data"]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_integer_vector_values_coerced_to_float(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [1, 2, 3]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")
        vectors = embedding.embed_documents(["test"])

        self.assertEqual(vectors, [[1.0, 2.0, 3.0]])
        self.assertIsInstance(vectors[0][0], float)
        self.assertIsInstance(vectors[0][1], float)
        self.assertIsInstance(vectors[0][2], float)

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_mixed_int_float_vector_values(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [1, 2.5, 3, 4.8]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")
        vectors = embedding.embed_documents(["test"])

        self.assertEqual(vectors, [[1.0, 2.5, 3.0, 4.8]])

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_embed_query(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [{"embedding": [0.9, 0.8, 0.7, 0.6]}]
        }
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")
        vector = embedding.embed_query("query text")

        self.assertEqual(vector, [0.9, 0.8, 0.7, 0.6])

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_large_batch_of_documents(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_data = {
            "data": [{"embedding": [float(i), float(i + 1)]} for i in range(100)]
        }
        response_mock.json.return_value = response_data
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")
        texts = [f"text_{i}" for i in range(100)]
        vectors = embedding.embed_documents(texts)

        self.assertEqual(len(vectors), 100)
        for i, vector in enumerate(vectors):
            self.assertEqual(vector, [float(i), float(i + 1)])

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
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

        embedding = SiliconFlowEmbedding(api_key="token")
        vectors = embedding.embed_documents(["t1", "t2", "t3"])

        self.assertEqual(len(vectors), 3)
        self.assertEqual(len(vectors[0]), 2)
        self.assertEqual(len(vectors[1]), 4)
        self.assertEqual(len(vectors[2]), 1)

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_single_document_processing(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.5, 0.6, 0.7]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")
        vectors = embedding.embed_documents(["single text"])

        self.assertEqual(len(vectors), 1)
        self.assertEqual(vectors[0], [0.5, 0.6, 0.7])

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_model_attribute_accessible(self, requests_module):
        embedding = SiliconFlowEmbedding(api_key="token", model="custom-model-v2")

        self.assertEqual(embedding.model, "custom-model-v2")

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_no_request_options_by_default(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")
        embedding.embed_documents(["test"])

        kwargs = requests_module.post.call_args.kwargs
        payload = kwargs["json"]
        self.assertEqual(set(payload.keys()), {"model", "input"})

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_content_type_header(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")
        embedding.embed_documents(["test"])

        kwargs = requests_module.post.call_args.kwargs
        self.assertEqual(kwargs["headers"]["Content-Type"], "application/json")

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_authorization_header_format(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="my-api-key-123")
        embedding.embed_documents(["test"])

        kwargs = requests_module.post.call_args.kwargs
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer my-api-key-123")

    def test_missing_requests_dependency_raises(self):
        with patch(
            "ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests", None
        ):
            with self.assertRaises(ImportError):
                SiliconFlowEmbedding(api_key="token")

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_missing_data_field_raises(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"results": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(api_key="token")

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_base_url_without_trailing_slash(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(
            api_key="token", base_url="https://custom.api.com"
        )
        embedding.embed_documents(["test"])

        args = requests_module.post.call_args
        self.assertEqual(args.args[0], "https://custom.api.com/embeddings")

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_multiple_request_options(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(
            api_key="token",
            request_options={
                "encoding_format": "float",
                "dimensions": 512,
                "user": "test_user",
            },
        )
        embedding.embed_documents(["test"])

        kwargs = requests_module.post.call_args.kwargs
        payload = kwargs["json"]
        self.assertEqual(payload["encoding_format"], "float")
        self.assertEqual(payload["dimensions"], 512)
        self.assertEqual(payload["user"], "test_user")

    @patch("ali_agentic_adk_python.core.embedding.siliconflow_embedding.requests")
    def test_custom_headers_do_not_remove_defaults(self, requests_module):
        response_mock = MagicMock()
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"data": [{"embedding": [0.1]}]}
        requests_module.post.return_value = response_mock

        embedding = SiliconFlowEmbedding(
            api_key="token", headers={"X-Custom": "header"}
        )
        embedding.embed_documents(["test"])

        kwargs = requests_module.post.call_args.kwargs
        headers = kwargs["headers"]
        self.assertIn("Authorization", headers)
        self.assertIn("Content-Type", headers)
        self.assertIn("X-Custom", headers)


if __name__ == "__main__":
    unittest.main()
