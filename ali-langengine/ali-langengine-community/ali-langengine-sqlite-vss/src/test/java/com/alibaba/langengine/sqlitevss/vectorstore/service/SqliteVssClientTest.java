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
package com.alibaba.langengine.sqlitevss.vectorstore.service;

import com.alibaba.langengine.sqlitevss.vectorstore.SqliteVssException;
import com.alibaba.langengine.sqlitevss.vectorstore.SqliteVssParam;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
public class SqliteVssClientTest {

    @TempDir
    Path tempDir;

    @Test
    void testClientCreation() {
        SqliteVssParam param = SqliteVssParam.builder()
                .dbPath(tempDir.resolve("test.db").toString())
                .collectionName("test_collection")
                .build();

        assertDoesNotThrow(() -> {
            try (SqliteVssClient client = new SqliteVssClient(param)) {
                assertNotNull(client);
            }
        });
    }

    @Test
    void testCreateCollectionIfNotExists() {
        SqliteVssParam param = SqliteVssParam.builder()
                .dbPath(tempDir.resolve("test.db").toString())
                .collectionName("test_collection")
                .build();

        try (SqliteVssClient client = new SqliteVssClient(param)) {
            assertDoesNotThrow(() -> client.createCollectionIfNotExists("new_collection"));
        }
    }

    @Test
    void testInsertDocument() {
        SqliteVssParam param = SqliteVssParam.builder()
                .dbPath(tempDir.resolve("test.db").toString())
                .collectionName("test_collection")
                .build();

        try (SqliteVssClient client = new SqliteVssClient(param)) {
            SqliteVssInsertRequest request = SqliteVssInsertRequest.builder()
                    .id("test-doc-1")
                    .content("Test document content")
                    .vector(createTestVector())
                    .metadata(createTestMetadata())
                    .collectionName("test_collection")
                    .build();

            assertDoesNotThrow(() -> client.insertDocument(request));
        }
    }

    @Test
    void testInsertDocumentsBatch() {
        SqliteVssParam param = SqliteVssParam.builder()
                .dbPath(tempDir.resolve("test.db").toString())
                .collectionName("test_collection")
                .build();

        try (SqliteVssClient client = new SqliteVssClient(param)) {
            List<SqliteVssInsertRequest> requests = Arrays.asList(
                    createInsertRequest("doc-1", "Content 1"),
                    createInsertRequest("doc-2", "Content 2"),
                    createInsertRequest("doc-3", "Content 3")
            );

            assertDoesNotThrow(() -> client.insertDocumentsBatch(requests));
        }
    }

    @Test
    void testInsertEmptyBatch() {
        SqliteVssParam param = SqliteVssParam.builder()
                .dbPath(tempDir.resolve("test.db").toString())
                .collectionName("test_collection")
                .build();

        try (SqliteVssClient client = new SqliteVssClient(param)) {
            List<SqliteVssInsertRequest> emptyRequests = new ArrayList<>();
            
            assertDoesNotThrow(() -> client.insertDocumentsBatch(emptyRequests));
        }
    }

    @Test
    void testSearchSimilarDocuments() {
        SqliteVssParam param = SqliteVssParam.builder()
                .dbPath(tempDir.resolve("test.db").toString())
                .collectionName("test_collection")
                .build();

        try (SqliteVssClient client = new SqliteVssClient(param)) {
            // First insert some test data
            List<SqliteVssInsertRequest> requests = Arrays.asList(
                    createInsertRequest("doc-1", "Machine learning basics"),
                    createInsertRequest("doc-2", "Deep learning fundamentals"),
                    createInsertRequest("doc-3", "Natural language processing")
            );
            client.insertDocumentsBatch(requests);

            // Then search
            SqliteVssSearchRequest searchRequest = SqliteVssSearchRequest.builder()
                    .queryText("machine learning")
                    .topK(2)
                    .collectionName("test_collection")
                    .includeDistances(true)
                    .build();

            SqliteVssSearchResponse response = client.searchSimilarDocuments(searchRequest);
            
            assertNotNull(response);
            assertNotNull(response.getResults());
            assertTrue(response.getExecutionTimeMs() >= 0);
        }
    }

    @Test
    void testSearchWithMetadataFilter() {
        SqliteVssParam param = SqliteVssParam.builder()
                .dbPath(tempDir.resolve("test.db").toString())
                .collectionName("test_collection")
                .build();

        try (SqliteVssClient client = new SqliteVssClient(param)) {
            // Insert test data with metadata
            Map<String, Object> metadata1 = new HashMap<>();
            metadata1.put("category", "ml");
            metadata1.put("level", "beginner");

            Map<String, Object> metadata2 = new HashMap<>();
            metadata2.put("category", "dl");
            metadata2.put("level", "advanced");

            List<SqliteVssInsertRequest> requests = Arrays.asList(
                    createInsertRequestWithMetadata("doc-1", "Content 1", metadata1),
                    createInsertRequestWithMetadata("doc-2", "Content 2", metadata2)
            );
            client.insertDocumentsBatch(requests);

            // Search with metadata filter
            Map<String, Object> filter = Collections.singletonMap("category", "ml");
            SqliteVssSearchRequest searchRequest = SqliteVssSearchRequest.builder()
                    .queryText("content")
                    .topK(5)
                    .metadataFilter(filter)
                    .collectionName("test_collection")
                    .build();

            SqliteVssSearchResponse response = client.searchSimilarDocuments(searchRequest);
            
            assertNotNull(response);
            assertNotNull(response.getResults());
        }
    }

    @Test
    void testDeleteDocument() {
        SqliteVssParam param = SqliteVssParam.builder()
                .dbPath(tempDir.resolve("test.db").toString())
                .collectionName("test_collection")
                .build();

        try (SqliteVssClient client = new SqliteVssClient(param)) {
            // Insert a document
            SqliteVssInsertRequest request = createInsertRequest("doc-to-delete", "Content to delete");
            client.insertDocument(request);

            // Delete the document
            boolean deleted = client.deleteDocument("test_collection", "doc-to-delete");
            assertTrue(deleted);

            // Try to delete again (should return false)
            boolean deletedAgain = client.deleteDocument("test_collection", "doc-to-delete");
            assertFalse(deletedAgain);
        }
    }

    @Test
    void testGetDocumentCount() {
        SqliteVssParam param = SqliteVssParam.builder()
                .dbPath(tempDir.resolve("test.db").toString())
                .collectionName("test_collection")
                .build();

        try (SqliteVssClient client = new SqliteVssClient(param)) {
            // Initially should be 0
            long initialCount = client.getDocumentCount("test_collection");
            assertEquals(0, initialCount);

            // Insert some documents
            List<SqliteVssInsertRequest> requests = Arrays.asList(
                    createInsertRequest("doc-1", "Content 1"),
                    createInsertRequest("doc-2", "Content 2")
            );
            client.insertDocumentsBatch(requests);

            // Count should increase
            long afterInsertCount = client.getDocumentCount("test_collection");
            assertEquals(2, afterInsertCount);
        }
    }

    @Test
    void testClientClose() {
        SqliteVssParam param = SqliteVssParam.builder()
                .dbPath(tempDir.resolve("test.db").toString())
                .collectionName("test_collection")
                .build();

        SqliteVssClient client = new SqliteVssClient(param);
        assertDoesNotThrow(() -> client.close());
    }

    @Test
    void testInvalidDatabasePath() {
        SqliteVssParam param = SqliteVssParam.builder()
                .dbPath("/invalid/path/that/does/not/exist/test.db")
                .collectionName("test_collection")
                .build();

        // Should handle invalid paths gracefully or throw appropriate exception
        assertThrows(SqliteVssException.class, () -> new SqliteVssClient(param));
    }

    private List<Double> createTestVector() {
        List<Double> vector = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            vector.add(Math.random());
        }
        return vector;
    }

    private Map<String, Object> createTestMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", "test");
        metadata.put("created_at", new Date().toString());
        metadata.put("version", 1);
        return metadata;
    }

    private SqliteVssInsertRequest createInsertRequest(String id, String content) {
        return SqliteVssInsertRequest.builder()
                .id(id)
                .content(content)
                .vector(createTestVector())
                .metadata(createTestMetadata())
                .collectionName("test_collection")
                .build();
    }

    private SqliteVssInsertRequest createInsertRequestWithMetadata(String id, String content, Map<String, Object> metadata) {
        return SqliteVssInsertRequest.builder()
                .id(id)
                .content(content)
                .vector(createTestVector())
                .metadata(metadata)
                .collectionName("test_collection")
                .build();
    }
}
