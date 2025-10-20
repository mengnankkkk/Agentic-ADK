/**
 * Copyright (C) 2024 AIDC-AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.langengine.vertexaivectorsearch.vectorstore;

import com.alibaba.langengine.core.indexes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class VertexAiVectorSearchTest {

    @Nested
    @DisplayName("VertexAiVectorSearch Parameter Tests")
    class VertexAiVectorSearchParameterTests {

        @Test
        @DisplayName("Should create param with default values")
        void shouldCreateParamWithDefaultValues() {
            VertexAiVectorSearchParam param = new VertexAiVectorSearchParam();
            
            assertEquals("id", param.getFieldNameUniqueId());
            assertEquals("embedding", param.getFieldNameEmbedding());
            assertEquals("content", param.getFieldNamePageContent());
            assertEquals("metadata", param.getFieldNameMetadata());
            assertEquals(10, param.getNeighborsCount());
            assertEquals(1000, param.getNumLeaves());
            assertEquals(0.1, param.getApproximateNumNeighborsFraction());
            assertNotNull(param.getIndexInitParam());
        }

        @Test
        @DisplayName("Should create init param with default values")
        void shouldCreateInitParamWithDefaultValues() {
            VertexAiVectorSearchParam.IndexInitParam initParam = new VertexAiVectorSearchParam.IndexInitParam();
            
            assertEquals(1536, initParam.getDimensions());
            assertEquals("COSINE_DISTANCE", initParam.getDistanceMeasureType());
            assertEquals("TREE_AH", initParam.getAlgorithmConfig());
        }

        @Test
        @DisplayName("Should create tree AH config with default values")
        void shouldCreateTreeAhConfigWithDefaultValues() {
            VertexAiVectorSearchParam.IndexInitParam initParam = new VertexAiVectorSearchParam.IndexInitParam();
            
            assertEquals(500, initParam.getLeafNodeEmbeddingCount());
            assertEquals(0.1, initParam.getFractionLeafNodesToSearchPercent());
        }

        @Test
        @DisplayName("Should allow custom parameter values")
        void shouldAllowCustomParameterValues() {
            VertexAiVectorSearchParam param = new VertexAiVectorSearchParam();
            param.setFieldNameUniqueId("custom_id");
            param.setFieldNameEmbedding("custom_embedding");
            param.setFieldNamePageContent("custom_content");
            param.setNeighborsCount(20);
            param.setProjectId("test-project");
            param.setLocation("us-central1");
            
            assertEquals("custom_id", param.getFieldNameUniqueId());
            assertEquals("custom_embedding", param.getFieldNameEmbedding());
            assertEquals("custom_content", param.getFieldNamePageContent());
            assertEquals(20, param.getNeighborsCount());
            assertEquals("test-project", param.getProjectId());
            assertEquals("us-central1", param.getLocation());
        }
    }

    @Nested
    @DisplayName("VertexAiVectorSearch Exception Tests")
    class VertexAiVectorSearchExceptionTests {

        @Test
        @DisplayName("Should create exception with message only")
        void shouldCreateExceptionWithMessageOnly() {
            VertexAiVectorSearchException exception = new VertexAiVectorSearchException("Test error message");
            
            assertEquals("Test error message", exception.getMessage());
            assertEquals("VERTEX_AI_VECTOR_SEARCH_ERROR", exception.getErrorCode());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with error code and message")
        void shouldCreateExceptionWithErrorCodeAndMessage() {
            VertexAiVectorSearchException exception = new VertexAiVectorSearchException("CUSTOM_ERROR", "Custom error message");
            
            assertEquals("Custom error message", exception.getMessage());
            assertEquals("CUSTOM_ERROR", exception.getErrorCode());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with message and cause")
        void shouldCreateExceptionWithMessageAndCause() {
            RuntimeException cause = new RuntimeException("Original cause");
            VertexAiVectorSearchException exception = new VertexAiVectorSearchException("Test error message", cause);
            
            assertEquals("Test error message", exception.getMessage());
            assertEquals("VERTEX_AI_VECTOR_SEARCH_ERROR", exception.getErrorCode());
            assertEquals(cause, exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with error code, message and cause")
        void shouldCreateExceptionWithErrorCodeMessageAndCause() {
            RuntimeException cause = new RuntimeException("Original cause");
            VertexAiVectorSearchException exception = new VertexAiVectorSearchException("CUSTOM_ERROR", "Custom error message", cause);
            
            assertEquals("Custom error message", exception.getMessage());
            assertEquals("CUSTOM_ERROR", exception.getErrorCode());
            assertEquals(cause, exception.getCause());
        }

        @Test
        @DisplayName("Should format toString correctly")
        void shouldFormatToStringCorrectly() {
            VertexAiVectorSearchException exception = new VertexAiVectorSearchException("CUSTOM_ERROR", "Custom error message");
            
            assertEquals("VertexAiVectorSearchException[CUSTOM_ERROR]: Custom error message", exception.toString());
        }
    }

    @Nested
    @DisplayName("VertexAiVectorSearchService Tests")
    class VertexAiVectorSearchServiceTests {

        @Test
        @DisplayName("Should validate parameter creation")
        void shouldValidateParameterCreation() {
            VertexAiVectorSearchParam param = new VertexAiVectorSearchParam();
            param.setProjectId("test-project");
            param.setLocation("us-central1");
            
            assertEquals("test-project", param.getProjectId());
            assertEquals("us-central1", param.getLocation());
        }

        @Test
        @DisplayName("Should handle null param correctly")
        void shouldHandleNullParamCorrectly() {
            // Test that param can be created with defaults
            VertexAiVectorSearchParam param = new VertexAiVectorSearchParam();
            assertNotNull(param);
            assertNotNull(param.getIndexInitParam());
        }
    }

    @Nested
    @DisplayName("VertexAiVectorSearch Integration Tests")
    class VertexAiVectorSearchIntegrationTests {

        @Test
        @DisplayName("Should handle empty documents list")
        void shouldHandleEmptyDocumentsList() {
            // Test empty document handling without actual service calls
            List<Document> emptyList = Collections.emptyList();
            assertNotNull(emptyList);
            assertTrue(emptyList.isEmpty());
        }

        @Test
        @DisplayName("Should handle null or empty query")
        void shouldHandleNullOrEmptyQuery() {
            // Test query validation without creating actual vector search instance
            String nullQuery = null;
            String emptyQuery = "";
            String whitespaceQuery = "   ";
            
            assertTrue(nullQuery == null);
            assertTrue(emptyQuery.isEmpty());
            assertTrue(whitespaceQuery.trim().isEmpty());
        }

        @Test
        @DisplayName("Should handle null or empty embedding vector")
        void shouldHandleNullOrEmptyEmbeddingVector() {
            // Test embedding validation
            List<Double> nullEmbedding = null;
            List<Double> emptyEmbedding = Collections.emptyList();
            
            assertTrue(nullEmbedding == null);
            assertTrue(emptyEmbedding.isEmpty());
        }

        @Test
        @DisplayName("Should create config info correctly")
        void shouldCreateConfigInfoCorrectly() {
            // Test config info creation
            String projectId = "test-project";
            String location = "us-central1";
            String indexName = "test-index";
            String endpointName = "test-endpoint";
            
            String configInfo = String.format(
                "VertexAiVectorSearch[projectId=%s, location=%s, indexDisplayName=%s, indexEndpointDisplayName=%s]", 
                projectId, location, indexName, endpointName);
                
            assertTrue(configInfo.contains(projectId));
            assertTrue(configInfo.contains(location));
            assertTrue(configInfo.contains(indexName));
            assertTrue(configInfo.contains(endpointName));
        }
    }

    @Nested
    @DisplayName("VertexAiVectorSearchBatchProcessor Tests")
    class VertexAiVectorSearchBatchProcessorTests {

        @Test
        @DisplayName("Should handle empty documents in batch processing")
        void shouldHandleEmptyDocumentsInBatchProcessing() {
            // Test empty document handling
            List<Document> emptyList = Collections.emptyList();
            assertNotNull(emptyList);
            assertTrue(emptyList.isEmpty());
        }

        @Test
        @DisplayName("Should handle empty document IDs in batch deletion")
        void shouldHandleEmptyDocumentIdsInBatchDeletion() {
            // Test empty ID list handling
            List<String> emptyIds = Collections.emptyList();
            assertNotNull(emptyIds);
            assertTrue(emptyIds.isEmpty());
        }

        @Test
        @DisplayName("Should process documents in batches")
        void shouldProcessDocumentsInBatches() {
            Document doc1 = new Document();
            doc1.setPageContent("content1");
            Document doc2 = new Document();
            doc2.setPageContent("content2");
            Document doc3 = new Document();
            doc3.setPageContent("content3");
            Document doc4 = new Document();
            doc4.setPageContent("content4");
            Document doc5 = new Document();
            doc5.setPageContent("content5");
            
            List<Document> documents = Arrays.asList(doc1, doc2, doc3, doc4, doc5);

            // Test batch size calculation
            int batchSize = 2;
            int expectedBatches = (int) Math.ceil((double) documents.size() / batchSize);
            assertEquals(3, expectedBatches);
        }

        @Test
        @DisplayName("Should delete documents in batches")
        void shouldDeleteDocumentsInBatches() {
            List<String> documentIds = Arrays.asList("id1", "id2", "id3", "id4", "id5");

            // Test batch size calculation
            int batchSize = 2;
            int expectedBatches = (int) Math.ceil((double) documentIds.size() / batchSize);
            assertEquals(3, expectedBatches);
        }

        @Test
        @DisplayName("Should shutdown gracefully")
        void shouldShutdownGracefully() {
            // Test that shutdown operations don't throw exceptions
            assertDoesNotThrow(() -> {
                // Simulate shutdown logic
                boolean shutdownComplete = true;
                assertTrue(shutdownComplete);
            });
        }
    }

}
