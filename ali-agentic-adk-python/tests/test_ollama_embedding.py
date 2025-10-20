import unittest
from types import SimpleNamespace
from unittest.mock import MagicMock, call, patch

from ali_agentic_adk_python.core.common.exceptions import EmbeddingProviderError
from ali_agentic_adk_python.core.embedding.ollama_embedding import OllamaEmbedding


class OllamaEmbeddingTestCase(unittest.TestCase):
    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_embed_documents_returns_vectors(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "embeddings": [
                [0.1, 0.2],
                [0.3, 0.4],
            ]
        }
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding(
            model="all-minilm",
            base_url="http://localhost:11434/",
            request_options={"keep_alive": "5m"},
        )
        vectors = embedding.embed_documents(["hello", "world"])

        self.assertEqual(vectors, [[0.1, 0.2], [0.3, 0.4]])
        requests_module.post.assert_called_once_with(
            "http://localhost:11434/api/embed",
            headers={"Content-Type": "application/json"},
            json={"model": "all-minilm", "input": ["hello", "world"], "keep_alive": "5m"},
            timeout=None,
        )

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_new_endpoint_404_fallbacks_to_legacy(self, requests_module):
        new_response = MagicMock()
        new_response.status_code = 404

        legacy_response_one = MagicMock()
        legacy_response_one.status_code = 200
        legacy_response_one.raise_for_status.return_value = None
        legacy_response_one.json.return_value = {"embedding": [0.5, 0.6]}

        legacy_response_two = MagicMock()
        legacy_response_two.status_code = 200
        legacy_response_two.raise_for_status.return_value = None
        legacy_response_two.json.return_value = {"embedding": [0.7, 0.8]}

        requests_module.post.side_effect = [
            new_response,
            legacy_response_one,
            legacy_response_two,
        ]

        embedding = OllamaEmbedding(model="all-minilm")
        vectors = embedding.embed_documents(["doc-1", "doc-2"])

        self.assertEqual(vectors, [[0.5, 0.6], [0.7, 0.8]])
        self.assertEqual(requests_module.post.call_args_list, [
            call(
                "http://localhost:11434/api/embed",
                headers={"Content-Type": "application/json"},
                json={"model": "all-minilm", "input": ["doc-1", "doc-2"]},
                timeout=None,
            ),
            call(
                "http://localhost:11434/api/embeddings",
                headers={"Content-Type": "application/json"},
                json={"model": "all-minilm", "prompt": "doc-1"},
                timeout=None,
            ),
            call(
                "http://localhost:11434/api/embeddings",
                headers={"Content-Type": "application/json"},
                json={"model": "all-minilm", "prompt": "doc-2"},
                timeout=None,
            ),
        ])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_prefer_legacy_endpoint_calls_embeddings_api(self, requests_module):
        requests_module.post.side_effect = [
            SimpleNamespace(
                status_code=200,
                raise_for_status=MagicMock(return_value=None),
                json=MagicMock(return_value={"embedding": [1, 2]}),
            ),
            SimpleNamespace(
                status_code=200,
                raise_for_status=MagicMock(return_value=None),
                json=MagicMock(return_value={"embedding": [3, 4]}),
            ),
        ]

        embedding = OllamaEmbedding(model="all-minilm", prefer_legacy_endpoint=True)
        vectors = embedding.embed_documents(["first", "second"])

        self.assertEqual(vectors, [[1.0, 2.0], [3.0, 4.0]])
        self.assertEqual(requests_module.post.call_args_list, [
            call(
                "http://localhost:11434/api/embeddings",
                headers={"Content-Type": "application/json"},
                json={"model": "all-minilm", "prompt": "first"},
                timeout=None,
            ),
            call(
                "http://localhost:11434/api/embeddings",
                headers={"Content-Type": "application/json"},
                json={"model": "all-minilm", "prompt": "second"},
                timeout=None,
            ),
        ])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_missing_vectors_raise_error(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": []}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_request_exception_is_wrapped(self, requests_module):
        requests_module.exceptions = SimpleNamespace(RequestException=RuntimeError)
        requests_module.post.side_effect = RuntimeError("boom")

        embedding = OllamaEmbedding()

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["text"])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_embed_query_returns_single_vector(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [[9.0, 8.0, 7.0]]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()
        vector = embedding.embed_query("query")

        self.assertEqual(vector, [9.0, 8.0, 7.0])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_empty_input_returns_empty_list(self, requests_module):
        embedding = OllamaEmbedding()
        vectors = embedding.embed_documents([])

        self.assertEqual(vectors, [])
        requests_module.post.assert_not_called()

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_default_model(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [[0.1, 0.2]]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()
        embedding.embed_documents(["test"])

        call_args = requests_module.post.call_args
        self.assertEqual(call_args.kwargs["json"]["model"], "nomic-embed-text")

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_custom_model(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [[0.1, 0.2]]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding(model="llama2")
        embedding.embed_documents(["test"])

        call_args = requests_module.post.call_args
        self.assertEqual(call_args.kwargs["json"]["model"], "llama2")

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_default_base_url(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [[0.1]]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()
        embedding.embed_documents(["test"])

        call_args = requests_module.post.call_args
        self.assertEqual(call_args.args[0], "http://localhost:11434/api/embed")

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_custom_base_url(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [[0.1]]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding(base_url="http://192.168.1.100:11434")
        embedding.embed_documents(["test"])

        call_args = requests_module.post.call_args
        self.assertEqual(call_args.args[0], "http://192.168.1.100:11434/api/embed")

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_base_url_strips_trailing_slash(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [[0.1]]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding(base_url="http://localhost:11434/")
        embedding.embed_documents(["test"])

        call_args = requests_module.post.call_args
        self.assertEqual(call_args.args[0], "http://localhost:11434/api/embed")

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_timeout_parameter(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [[0.1]]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding(timeout=30.0)
        embedding.embed_documents(["test"])

        call_args = requests_module.post.call_args
        self.assertEqual(call_args.kwargs["timeout"], 30.0)

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_tuple_timeout_parameter(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [[0.1]]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding(timeout=(5.0, 30.0))
        embedding.embed_documents(["test"])

        call_args = requests_module.post.call_args
        self.assertEqual(call_args.kwargs["timeout"], (5.0, 30.0))

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_custom_headers_merge_with_defaults(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [[0.1]]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding(headers={"X-Custom": "value"})
        embedding.embed_documents(["test"])

        call_args = requests_module.post.call_args
        headers = call_args.kwargs["headers"]
        self.assertEqual(headers["Content-Type"], "application/json")
        self.assertEqual(headers["X-Custom"], "value")

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_request_options_copied_not_mutated(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [[0.1]]}
        requests_module.post.return_value = response_mock

        original_options = {"option1": "value1"}
        embedding = OllamaEmbedding(request_options=original_options)

        original_options["option2"] = "value2"

        embedding.embed_documents(["test"])

        call_args = requests_module.post.call_args
        payload = call_args.kwargs["json"]
        self.assertIn("option1", payload)
        self.assertNotIn("option2", payload)

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_http_error_is_wrapped(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 500
        requests_module.exceptions = SimpleNamespace(RequestException=Exception)
        response_mock.raise_for_status.side_effect = Exception("HTTP 500")
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_json_parse_error_is_wrapped(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.side_effect = ValueError("invalid json")
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_response_with_data_field(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "data": [
                {"embedding": [1.0, 2.0]},
                {"embedding": [3.0, 4.0]},
            ]
        }
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()
        vectors = embedding.embed_documents(["test1", "test2"])

        self.assertEqual(vectors, [[1.0, 2.0], [3.0, 4.0]])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_embeddings_with_nested_embedding_field(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "embeddings": [
                {"embedding": [5.0, 6.0]},
                {"embedding": [7.0, 8.0]},
            ]
        }
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()
        vectors = embedding.embed_documents(["test1", "test2"])

        self.assertEqual(vectors, [[5.0, 6.0], [7.0, 8.0]])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_non_dict_payload_raises(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = "invalid string response"
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_non_list_embeddings_raises(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": "not a list"}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_invalid_embedding_item_type_raises(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "embeddings": [
                [1.0, 2.0],
                "invalid_string",
            ]
        }
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test1", "test2"])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_non_numeric_vector_values_raise(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [["invalid", "data"]]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_integer_vector_values_coerced_to_float(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [[1, 2, 3]]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()
        vectors = embedding.embed_documents(["test"])

        self.assertEqual(vectors, [[1.0, 2.0, 3.0]])
        self.assertIsInstance(vectors[0][0], float)

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_mixed_int_float_vector_values(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [[1, 2.5, 3, 4.8]]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()
        vectors = embedding.embed_documents(["test"])

        self.assertEqual(vectors, [[1.0, 2.5, 3.0, 4.8]])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_legacy_endpoint_missing_embedding_raises(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"wrong_field": [1.0, 2.0]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding(prefer_legacy_endpoint=True)

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_legacy_endpoint_tuple_embedding(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embedding": (1.0, 2.0, 3.0)}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding(prefer_legacy_endpoint=True)
        vectors = embedding.embed_documents(["test"])

        self.assertEqual(vectors, [[1.0, 2.0, 3.0]])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_large_batch_of_documents(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        
        embeddings = [[float(i), float(i + 1)] for i in range(50)]
        response_mock.json.return_value = {"embeddings": embeddings}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()
        texts = [f"text_{i}" for i in range(50)]
        vectors = embedding.embed_documents(texts)

        self.assertEqual(len(vectors), 50)
        for i, vector in enumerate(vectors):
            self.assertEqual(vector, [float(i), float(i + 1)])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_varying_vector_dimensions(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {
            "embeddings": [
                [1.0, 2.0],
                [3.0, 4.0, 5.0, 6.0],
                [7.0],
            ]
        }
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()
        vectors = embedding.embed_documents(["t1", "t2", "t3"])

        self.assertEqual(len(vectors), 3)
        self.assertEqual(len(vectors[0]), 2)
        self.assertEqual(len(vectors[1]), 4)
        self.assertEqual(len(vectors[2]), 1)

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_single_document_processing(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [[0.5, 0.6, 0.7]]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()
        vectors = embedding.embed_documents(["single"])

        self.assertEqual(len(vectors), 1)
        self.assertEqual(vectors[0], [0.5, 0.6, 0.7])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_model_attribute_accessible(self, requests_module):
        embedding = OllamaEmbedding(model="custom-model")

        self.assertEqual(embedding.model, "custom-model")

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_keep_alive_request_option(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [[0.1]]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding(request_options={"keep_alive": "10m"})
        embedding.embed_documents(["test"])

        call_args = requests_module.post.call_args
        self.assertEqual(call_args.kwargs["json"]["keep_alive"], "10m")

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_truncate_request_option(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [[0.1]]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding(request_options={"truncate": True})
        embedding.embed_documents(["test"])

        call_args = requests_module.post.call_args
        self.assertEqual(call_args.kwargs["json"]["truncate"], True)

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_multiple_request_options(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [[0.1]]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding(
            request_options={
                "keep_alive": "5m",
                "truncate": False,
                "options": {"temperature": 0.8},
            }
        )
        embedding.embed_documents(["test"])

        call_args = requests_module.post.call_args
        payload = call_args.kwargs["json"]
        self.assertEqual(payload["keep_alive"], "5m")
        self.assertEqual(payload["truncate"], False)
        self.assertEqual(payload["options"], {"temperature": 0.8})

    def test_missing_requests_dependency_raises(self):
        with patch(
            "ali_agentic_adk_python.core.embedding.ollama_embedding.requests", None
        ):
            with self.assertRaises(ImportError):
                OllamaEmbedding()

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_404_fallback_with_request_options(self, requests_module):
        new_response = MagicMock()
        new_response.status_code = 404

        legacy_response = MagicMock()
        legacy_response.status_code = 200
        legacy_response.raise_for_status.return_value = None
        legacy_response.json.return_value = {"embedding": [0.1, 0.2]}

        requests_module.post.side_effect = [new_response, legacy_response]

        embedding = OllamaEmbedding(request_options={"keep_alive": "5m"})
        vectors = embedding.embed_documents(["test"])

        self.assertEqual(vectors, [[0.1, 0.2]])
        
        legacy_call = requests_module.post.call_args_list[1]
        legacy_payload = legacy_call.kwargs["json"]
        self.assertEqual(legacy_payload["keep_alive"], "5m")

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_legacy_endpoint_error_propagated(self, requests_module):
        requests_module.exceptions = SimpleNamespace(RequestException=RuntimeError)
        requests_module.post.side_effect = RuntimeError("connection error")

        embedding = OllamaEmbedding(prefer_legacy_endpoint=True)

        with self.assertRaises(EmbeddingProviderError):
            embedding.embed_documents(["test"])

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_content_type_header_always_set(self, requests_module):
        response_mock = MagicMock()
        response_mock.status_code = 200
        response_mock.raise_for_status.return_value = None
        response_mock.json.return_value = {"embeddings": [[0.1]]}
        requests_module.post.return_value = response_mock

        embedding = OllamaEmbedding()
        embedding.embed_documents(["test"])

        call_args = requests_module.post.call_args
        self.assertEqual(call_args.kwargs["headers"]["Content-Type"], "application/json")

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_legacy_endpoint_with_empty_input_returns_empty(self, requests_module):
        embedding = OllamaEmbedding(prefer_legacy_endpoint=True)
        vectors = embedding.embed_documents([])

        self.assertEqual(vectors, [])
        requests_module.post.assert_not_called()

    @patch("ali_agentic_adk_python.core.embedding.ollama_embedding.requests")
    def test_exception_with_original_exception(self, requests_module):
        requests_module.exceptions = SimpleNamespace(RequestException=RuntimeError)
        original_error = RuntimeError("connection failed")
        requests_module.post.side_effect = original_error

        embedding = OllamaEmbedding()

        with self.assertRaises(EmbeddingProviderError) as context:
            embedding.embed_documents(["test"])

        self.assertIs(context.exception.__cause__, original_error)


if __name__ == "__main__":
    unittest.main()
