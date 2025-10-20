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
package com.alibaba.langengine.tile38.vectorstore;

import com.alibaba.langengine.core.indexes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


public class Tile38ServiceTest {

    private Tile38Client mockClient;
    private Tile38Service service;
    private final String collectionName = "test_collection";

    @BeforeEach
    public void setUp() {
        mockClient = mock(Tile38Client.class);
        service = new Tile38Service(mockClient, collectionName, 100, 1000);
    }

    @Test
    public void testAddDocuments() {
        // Prepare test data
        List<Document> documents = new ArrayList<>();
        Document doc = new Document();
        doc.setUniqueId("test-doc-1");
        doc.setPageContent("Test content");
        doc.setEmbedding(Arrays.asList(0.1, 0.2, 0.3));
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "test");
        metadata.put("category", "example");
        doc.setMetadata(metadata);
        
        documents.add(doc);

        // Mock client behavior
        when(mockClient.set(eq(collectionName), eq("test-doc-1"), anyDouble(), anyDouble(), any()))
                .thenReturn("OK");

        // Execute
        assertDoesNotThrow(() -> service.addDocuments(documents));

        // Verify
        verify(mockClient, times(1)).set(eq(collectionName), eq("test-doc-1"), 
                anyDouble(), anyDouble(), any());
    }

    @Test
    public void testAddDocumentsWithEmptyList() {
        List<Document> emptyDocuments = new ArrayList<>();

        // Execute
        assertDoesNotThrow(() -> service.addDocuments(emptyDocuments));

        // Verify that client is not called for empty list
        verify(mockClient, never()).set(anyString(), anyString(), anyDouble(), anyDouble(), any());
    }

    @Test
    public void testAddDocumentsGeneratesIdWhenMissing() {
        // Prepare test data without ID
        List<Document> documents = new ArrayList<>();
        Document doc = new Document();
        doc.setPageContent("Test content without ID");
        documents.add(doc);

        // Mock client behavior
        when(mockClient.set(eq(collectionName), anyString(), anyDouble(), anyDouble(), any()))
                .thenReturn("OK");

        // Execute
        assertDoesNotThrow(() -> service.addDocuments(documents));

        // Verify that ID was generated
        assertNotNull(doc.getUniqueId());
        verify(mockClient, times(1)).set(eq(collectionName), anyString(), 
                anyDouble(), anyDouble(), any());
    }

    @Test
    public void testSimilaritySearch() {
        String query = "test query";
        int k = 5;
        Double maxDistance = 100.0;

        // Mock client response
        List<Object> mockResults = Arrays.asList(
                Arrays.asList("doc1", Map.of("content", "Test content 1")),
                Arrays.asList("doc2", Map.of("content", "Test content 2"))
        );
        when(mockClient.within(collectionName, 0.0, 0.0, maxDistance, k))
                .thenReturn(mockResults);

        // Execute
        List<Document> results = service.similaritySearch(query, k, maxDistance);

        // Verify
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(mockClient, times(1)).within(collectionName, 0.0, 0.0, maxDistance, k);
    }

    @Test
    public void testNearbySearch() {
        double lat = 40.7128;
        double lon = -74.0060;
        int k = 3;

        // Mock client response
        List<Object> mockResults = Arrays.asList(
                Arrays.asList("nearby1", Map.of("content", "Nearby content 1"))
        );
        when(mockClient.nearby(collectionName, lat, lon, k)).thenReturn(mockResults);

        // Execute
        List<Document> results = service.nearbySearch(lat, lon, k);

        // Verify
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(mockClient, times(1)).nearby(collectionName, lat, lon, k);
    }

    @Test
    public void testDeleteDocument() {
        String docId = "doc-to-delete";

        // Mock client behavior
        when(mockClient.del(collectionName, docId)).thenReturn("1");

        // Execute
        assertDoesNotThrow(() -> service.deleteDocument(docId));

        // Verify
        verify(mockClient, times(1)).del(collectionName, docId);
    }

    @Test
    public void testDropCollection() {
        // Mock client behavior
        when(mockClient.drop(collectionName)).thenReturn("OK");

        // Execute
        assertDoesNotThrow(() -> service.dropCollection());

        // Verify
        verify(mockClient, times(1)).drop(collectionName);
    }

    @Test
    public void testAddDocumentsWithClientException() {
        // Prepare test data
        List<Document> documents = new ArrayList<>();
        Document doc = new Document();
        doc.setUniqueId("test-doc");
        doc.setPageContent("Test content");
        documents.add(doc);

        // Mock client to throw exception
        when(mockClient.set(anyString(), anyString(), anyDouble(), anyDouble(), any()))
                .thenThrow(new RuntimeException("Client error"));

        // Execute and verify exception
        assertThrows(Tile38Exception.class, () -> service.addDocuments(documents));
    }

    @Test
    public void testSimilaritySearchWithException() {
        // Mock client to throw exception
        when(mockClient.within(anyString(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenThrow(new RuntimeException("Search error"));

        // Execute and verify exception
        assertThrows(Tile38Exception.class, () -> 
                service.similaritySearch("query", 5, 100.0));
    }

}