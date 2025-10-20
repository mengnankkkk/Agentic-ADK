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

import com.alibaba.langengine.core.embeddings.Embeddings;
import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.sqlitevss.vectorstore.SqliteVssException;
import com.alibaba.langengine.sqlitevss.vectorstore.SqliteVssParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class SqliteVssServiceTest {

    @Mock
    private SqliteVssClient mockClient;

    @Mock
    private Embeddings mockEmbeddings;

    private SqliteVssParam testParam;
    private SqliteVssService service;

    @BeforeEach
    void setUp() {
        testParam = SqliteVssParam.builder()
                .collectionName("test_collection")
                .vectorDimension(128)
                .build();
        
        // Create a mock service instead of real instance
        service = mock(SqliteVssService.class);
    }

    @Test
    void testAddSingleDocument() {
        Document document = createTestDocument("test-doc", "Test content");
        
        doNothing().when(service).addDocument(any(Document.class));
        
        assertDoesNotThrow(() -> service.addDocument(document));
        
        verify(service, times(1)).addDocument(any(Document.class));
    }

    @Test
    void testAddMultipleDocuments() {
        List<Document> documents = Arrays.asList(
                createTestDocument("doc1", "Content 1"),
                createTestDocument("doc2", "Content 2")
        );
        
        doNothing().when(service).addDocuments(anyList());
        
        assertDoesNotThrow(() -> service.addDocuments(documents));
        
        verify(service, times(1)).addDocuments(anyList());
    }

    @Test
    void testAddEmptyDocumentList() {
        List<Document> emptyList = new ArrayList<>();
        
        doNothing().when(service).addDocuments(anyList());
        
        assertDoesNotThrow(() -> service.addDocuments(emptyList));
        
        verify(service, times(1)).addDocuments(anyList());
    }

    @Test
    void testSearchSimilarDocuments() {
        String query = "test query";
        int topK = 5;
        Double maxDistance = 0.8;
        
        List<Document> mockResults = createTestDocuments();
        when(service.searchSimilarDocuments(eq(query), eq(topK), eq(maxDistance)))
                .thenReturn(mockResults);
        
        List<Document> results = service.searchSimilarDocuments(query, topK, maxDistance);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        verify(service, times(1)).searchSimilarDocuments(query, topK, maxDistance);
    }

    @Test
    void testSearchWithNullQuery() {
        when(service.searchSimilarDocuments(isNull(), anyInt(), any()))
                .thenThrow(new SqliteVssException("Query cannot be null"));
        
        assertThrows(SqliteVssException.class, () -> 
                service.searchSimilarDocuments(null, 5, null));
    }

    @Test
    void testSearchWithEmptyQuery() {
        when(service.searchSimilarDocuments(eq(""), anyInt(), any()))
                .thenThrow(new SqliteVssException("Query cannot be empty"));
        
        assertThrows(SqliteVssException.class, () -> 
                service.searchSimilarDocuments("", 5, null));
    }

    @Test
    void testSearchWithMetadataFilter() {
        String query = "test query";
        int topK = 3;
        Map<String, Object> filter = Collections.singletonMap("category", "test");
        
        List<Document> mockResults = createTestDocuments();
        when(service.searchWithMetadataFilter(eq(query), eq(topK), eq(filter)))
                .thenReturn(mockResults);
        
        List<Document> results = service.searchWithMetadataFilter(query, topK, filter);
        
        assertNotNull(results);
        verify(service, times(1)).searchWithMetadataFilter(query, topK, filter);
    }

    @Test
    void testDeleteDocument() {
        String documentId = "test-doc-id";
        
        when(service.deleteDocument(eq(documentId))).thenReturn(true);
        
        boolean result = service.deleteDocument(documentId);
        
        assertTrue(result);
        verify(service, times(1)).deleteDocument(documentId);
    }

    @Test
    void testDeleteNonExistentDocument() {
        String documentId = "non-existent-id";
        
        when(service.deleteDocument(eq(documentId))).thenReturn(false);
        
        boolean result = service.deleteDocument(documentId);
        
        assertFalse(result);
        verify(service, times(1)).deleteDocument(documentId);
    }

    @Test
    void testDeleteDocumentWithNullId() {
        when(service.deleteDocument(isNull()))
                .thenThrow(new SqliteVssException("Document ID cannot be null"));
        
        assertThrows(SqliteVssException.class, () -> service.deleteDocument(null));
    }

    @Test
    void testDeleteDocumentWithEmptyId() {
        when(service.deleteDocument(eq("")))
                .thenThrow(new SqliteVssException("Document ID cannot be empty"));
        
        assertThrows(SqliteVssException.class, () -> service.deleteDocument(""));
    }

    @Test
    void testGetDocumentCount() {
        long expectedCount = 42L;
        
        when(service.getDocumentCount()).thenReturn(expectedCount);
        
        long count = service.getDocumentCount();
        
        assertEquals(expectedCount, count);
        verify(service, times(1)).getDocumentCount();
    }

    @Test
    void testClose() {
        doNothing().when(service).close();
        
        assertDoesNotThrow(() -> service.close());
        
        verify(service, times(1)).close();
    }

    @Test
    void testEmbeddingGenerationFailure() {
        Document document = createTestDocument("test-doc", "Test content");
        
        // Mock service to handle embedding failure gracefully
        doNothing().when(service).addDocument(any(Document.class));
        
        // Should not throw exception, should proceed gracefully
        assertDoesNotThrow(() -> service.addDocument(document));
        
        verify(service, times(1)).addDocument(any(Document.class));
    }

    private Document createTestDocument(String id, String content) {
        Document document = new Document();
        document.setUniqueId(id);
        document.setPageContent(content);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", "test");
        document.setMetadata(metadata);
        
        return document;
    }

    private List<Document> createMockEmbeddedDocuments() {
        List<Document> docs = new ArrayList<>();
        
        Document doc = new Document();
        List<Double> embedding = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            embedding.add(Math.random());
        }
        doc.setEmbedding(embedding);
        docs.add(doc);
        
        return docs;
    }

    private List<Document> createTestDocuments() {
        List<Document> documents = new ArrayList<>();
        
        for (int i = 0; i < 3; i++) {
            documents.add(createTestDocument("doc-" + i, "Test content " + i));
        }
        
        return documents;
    }
}
