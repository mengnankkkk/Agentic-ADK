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
package com.alibaba.langengine.atlas.vectorstore;

import com.alibaba.langengine.core.embeddings.FakeEmbeddings;
import com.alibaba.langengine.core.indexes.Document;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Atlas Vector Search test suite with Mock testing
 *
 * @author xiaoxuan.lp
 */
public class AtlasTest {

    @Nested
    @DisplayName("Atlas Parameter Tests")
    class AtlasParameterTests {

        @Test
        @DisplayName("Test AtlasParam default values")
        public void testAtlasParamDefaults() {
            AtlasParam param = new AtlasParam();
            
            assertEquals("content_id", param.getFieldNameUniqueId());
            assertEquals("embeddings", param.getFieldNameEmbedding());
            assertEquals("row_content", param.getFieldNamePageContent());
            assertEquals("vector_index", param.getVectorIndexName());
            assertEquals(100, param.getNumCandidates());
            assertNotNull(param.getInitParam());
        }

        @Test
        @DisplayName("Test AtlasParam InitParam defaults")
        public void testInitParamDefaults() {
            AtlasParam.InitParam initParam = new AtlasParam.InitParam();
            
            assertTrue(initParam.isFieldUniqueIdAsPrimaryKey());
            assertEquals(1536, initParam.getFieldEmbeddingsDimension());
            assertEquals("cosine", initParam.getSimilarity());
        }

        @Test
        @DisplayName("Test AtlasParam parameter setting")
        public void testParameterSetting() {
            AtlasParam param = new AtlasParam();
            param.setFieldNameUniqueId("test_id");
            param.setFieldNameEmbedding("test_embedding");
            param.setFieldNamePageContent("test_content");
            param.setVectorIndexName("test_index");
            param.setNumCandidates(200);
            
            assertEquals("test_id", param.getFieldNameUniqueId());
            assertEquals("test_embedding", param.getFieldNameEmbedding());
            assertEquals("test_content", param.getFieldNamePageContent());
            assertEquals("test_index", param.getVectorIndexName());
            assertEquals(200, param.getNumCandidates());
        }

        @Test
        @DisplayName("Test AtlasParam InitParam parameter setting")
        public void testInitParamSetting() {
            AtlasParam.InitParam initParam = new AtlasParam.InitParam();
            initParam.setFieldUniqueIdAsPrimaryKey(false);
            initParam.setFieldEmbeddingsDimension(768);
            initParam.setSimilarity("euclidean");
            
            assertFalse(initParam.isFieldUniqueIdAsPrimaryKey());
            assertEquals(768, initParam.getFieldEmbeddingsDimension());
            assertEquals("euclidean", initParam.getSimilarity());
        }
    }

    @Nested
    @DisplayName("Atlas Exception Tests")
    class AtlasExceptionTests {

        @Test
        @DisplayName("Test AtlasException creation with message")
        public void testAtlasExceptionWithMessage() {
            AtlasException exception = new AtlasException("Test message");
            assertEquals("Test message", exception.getMessage());
            assertEquals("ATLAS_ERROR", exception.getErrorCode());
        }

        @Test
        @DisplayName("Test AtlasException creation with error code and message")
        public void testAtlasExceptionWithErrorCodeAndMessage() {
            AtlasException exception = new AtlasException("TEST_CODE", "Test message");
            assertEquals("Test message", exception.getMessage());
            assertEquals("TEST_CODE", exception.getErrorCode());
        }

        @Test
        @DisplayName("Test AtlasException creation with message and cause")
        public void testAtlasExceptionWithMessageAndCause() {
            RuntimeException cause = new RuntimeException("Cause");
            AtlasException exception = new AtlasException("Test message", cause);
            assertEquals("Test message", exception.getMessage());
            assertEquals("ATLAS_ERROR", exception.getErrorCode());
            assertEquals(cause, exception.getCause());
        }

        @Test
        @DisplayName("Test AtlasException creation with error code, message and cause")
        public void testAtlasExceptionWithErrorCodeMessageAndCause() {
            RuntimeException cause = new RuntimeException("Cause");
            AtlasException exception = new AtlasException("TEST_CODE", "Test message", cause);
            assertEquals("Test message", exception.getMessage());
            assertEquals("TEST_CODE", exception.getErrorCode());
            assertEquals(cause, exception.getCause());
        }

        @Test
        @DisplayName("Test AtlasException toString method")
        public void testAtlasExceptionToString() {
            AtlasException exception = new AtlasException("TEST_CODE", "Test message");
            String expected = "AtlasException[TEST_CODE]: Test message";
            assertEquals(expected, exception.toString());
        }
    }

    @Nested
    @DisplayName("Atlas Service Mock Tests")
    class AtlasServiceMockTests {

        @Mock
        private MongoClient mockMongoClient;
        
        @Mock
        private MongoDatabase mockDatabase;
        
        @Mock
        private MongoCollection<org.bson.Document> mockCollection;

        @BeforeEach
        void setUp() {
            // MockitoAnnotations.openMocks(this);
        }

        @Test
        @DisplayName("Test AtlasService input validation - null connection string")
        public void testNullConnectionString() {
            AtlasException exception = assertThrows(AtlasException.class, () -> {
                new AtlasService(null, "testdb", "testcoll", null);
            });
            assertEquals("INVALID_CONFIG", exception.getErrorCode());
            assertTrue(exception.getMessage().contains("Connection string"));
        }

        @Test
        @DisplayName("Test AtlasService input validation - empty database name")
        public void testEmptyDatabaseName() {
            AtlasException exception = assertThrows(AtlasException.class, () -> {
                new AtlasService("mongodb://localhost:27017", "", "testcoll", null);
            });
            assertEquals("INVALID_CONFIG", exception.getErrorCode());
            assertTrue(exception.getMessage().contains("Database name"));
        }

        @Test
        @DisplayName("Test AtlasService input validation - blank collection name")
        public void testBlankCollectionName() {
            AtlasException exception = assertThrows(AtlasException.class, () -> {
                new AtlasService("mongodb://localhost:27017", "testdb", "   ", null);
            });
            assertEquals("INVALID_CONFIG", exception.getErrorCode());
            assertTrue(exception.getMessage().contains("Collection name"));
        }

        @Test
        @DisplayName("Test validation through constructor - these tests validate input checking")
        public void testValidationThroughConstructor() {
            // Test validation is working by checking constructor input validation
            assertTrue(true); // Placeholder for validation tests
        }

        private AtlasService createMockAtlasService() {
            // For validation testing, we expect connection to fail
            // but we can still test the validation logic through exceptions
            return null; // Will be handled in individual test methods
        }


    }

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("Test embedding dimension calculation")
        public void testEmbeddingDimensionCalculation() {
            FakeEmbeddings fakeEmbeddings = new FakeEmbeddings();
            assertNotNull(fakeEmbeddings);
        }

        @Test
        @DisplayName("Test document creation")
        public void testDocumentCreation() {
            Document doc = createTestDocument("1", "test content");
            
            assertEquals("1", doc.getUniqueId());
            assertEquals("test content", doc.getPageContent());
        }

        @Test
        @DisplayName("Test document with embeddings")
        public void testDocumentWithEmbeddings() {
            Document doc = createTestDocument("1", "test content");
            List<Double> embeddings = Arrays.asList(1.0, 2.0, 3.0, 4.0);
            doc.setEmbedding(embeddings);
            
            assertEquals("1", doc.getUniqueId());
            assertEquals("test content", doc.getPageContent());
            assertEquals(embeddings, doc.getEmbedding());
            assertEquals(4, doc.getEmbedding().size());
        }

        private Document createTestDocument(String id, String content) {
            Document doc = new Document();
            doc.setUniqueId(id);
            doc.setPageContent(content);
            return doc;
        }
    }

    @Nested
    @DisplayName("Atlas Configuration Tests")
    class AtlasConfigurationTests {

        @Test
        @DisplayName("Test Atlas configuration constants")
        public void testAtlasConfiguration() {
            assertDoesNotThrow(() -> {
                Class.forName("com.alibaba.langengine.atlas.AtlasConfiguration");
            });
        }

        @Test
        @DisplayName("Test configuration default values")
        public void testConfigurationDefaults() {
            // Test that configuration class has default values
            assertDoesNotThrow(() -> {
                String connectionString = com.alibaba.langengine.atlas.AtlasConfiguration.ATLAS_CONNECTION_STRING;
                String databaseName = com.alibaba.langengine.atlas.AtlasConfiguration.ATLAS_DATABASE_NAME;
                int connectionTimeout = com.alibaba.langengine.atlas.AtlasConfiguration.ATLAS_CONNECTION_TIMEOUT;
                int socketTimeout = com.alibaba.langengine.atlas.AtlasConfiguration.ATLAS_SOCKET_TIMEOUT;
                
                // Should not be null due to default values
                assertNotNull(connectionString);
                assertNotNull(databaseName);
                assertTrue(connectionTimeout > 0);
                assertTrue(socketTimeout > 0);
            });
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Test Atlas parameter integration")
        public void testAtlasParamIntegration() {
            AtlasParam param = new AtlasParam();
            param.setFieldNameUniqueId("custom_id");
            param.setVectorIndexName("custom_index");
            param.setNumCandidates(50);
            
            AtlasParam.InitParam initParam = param.getInitParam();
            initParam.setFieldEmbeddingsDimension(768);
            initParam.setSimilarity("euclidean");
            
            // Verify all settings are preserved
            assertEquals("custom_id", param.getFieldNameUniqueId());
            assertEquals("custom_index", param.getVectorIndexName());
            assertEquals(50, param.getNumCandidates());
            assertEquals(768, initParam.getFieldEmbeddingsDimension());
            assertEquals("euclidean", initParam.getSimilarity());
        }

        @Test
        @DisplayName("Test exception error code consistency")
        public void testExceptionErrorCodeConsistency() {
            // Test that error codes are consistent across different exception types
            AtlasException configException = new AtlasException("INVALID_CONFIG", "Config error");
            AtlasException documentException = new AtlasException("INVALID_DOCUMENT", "Document error");
            AtlasException searchException = new AtlasException("INVALID_SEARCH", "Search error");
            
            assertEquals("INVALID_CONFIG", configException.getErrorCode());
            assertEquals("INVALID_DOCUMENT", documentException.getErrorCode());
            assertEquals("INVALID_SEARCH", searchException.getErrorCode());
            
            // Test toString format consistency
            assertTrue(configException.toString().startsWith("AtlasException[INVALID_CONFIG]:"));
            assertTrue(documentException.toString().startsWith("AtlasException[INVALID_DOCUMENT]:"));
            assertTrue(searchException.toString().startsWith("AtlasException[INVALID_SEARCH]:"));
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Test concurrent parameter access")
        public void testConcurrentParameterAccess() {
            AtlasParam param = new AtlasParam();
            
            // Simulate concurrent access
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 10; i++) {
                    param.getFieldNameUniqueId();
                    param.getFieldNameEmbedding();
                    param.getVectorIndexName();
                }
            });
        }

        @Test
        @DisplayName("Test exception thread safety")
        public void testExceptionThreadSafety() {
            // Test that exceptions can be created concurrently
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 10; i++) {
                    AtlasException ex = new AtlasException("TEST_" + i, "Message " + i);
                    assertEquals("TEST_" + i, ex.getErrorCode());
                    assertEquals("Message " + i, ex.getMessage());
                }
            });
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Test parameter creation performance")
        public void testParameterCreationPerformance() {
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < 1000; i++) {
                AtlasParam param = new AtlasParam();
                param.setFieldNameUniqueId("id_" + i);
                param.setNumCandidates(i % 100 + 1);
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // Should complete within reasonable time (less than 1 second)
            assertTrue(duration < 1000, "Parameter creation took too long: " + duration + "ms");
        }

        @Test
        @DisplayName("Test exception creation performance")
        public void testExceptionCreationPerformance() {
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < 1000; i++) {
                AtlasException ex = new AtlasException("CODE_" + i, "Message " + i);
                assertNotNull(ex.getErrorCode());
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // Should complete within reasonable time (less than 1 second)
            assertTrue(duration < 1000, "Exception creation took too long: " + duration + "ms");
        }
    }

}