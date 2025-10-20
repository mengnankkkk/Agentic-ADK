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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


public class Tile38BatchTest {

    private Tile38Client mockClient;
    private Tile38BatchProcessor batchProcessor;
    private final String collectionName = "test_collection";

    @BeforeEach
    public void setUp() {
        mockClient = mock(Tile38Client.class);
        batchProcessor = new Tile38BatchProcessor(mockClient, collectionName, 2);
    }

    @Test
    public void testBatchAddDocuments() {
        // Prepare test data
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Document doc = new Document();
            doc.setUniqueId("doc" + i);
            doc.setPageContent("Content " + i);
            doc.setEmbedding(Arrays.asList(0.1 * i, 0.2 * i));
            documents.add(doc);
        }

        // Mock client behavior
        when(mockClient.set(eq(collectionName), anyString(), anyDouble(), anyDouble(), any()))
                .thenReturn("OK");

        // Execute
        assertDoesNotThrow(() -> batchProcessor.batchAddDocuments(documents));

        // Verify that all documents were processed
        verify(mockClient, times(5)).set(eq(collectionName), anyString(), 
                anyDouble(), anyDouble(), any());
    }

    @Test
    public void testBatchDeleteDocuments() {
        List<String> documentIds = Arrays.asList("doc1", "doc2", "doc3");

        // Mock client behavior
        when(mockClient.del(eq(collectionName), anyString())).thenReturn("1");

        // Execute
        assertDoesNotThrow(() -> batchProcessor.batchDeleteDocuments(documentIds));

        // Verify
        verify(mockClient, times(3)).del(eq(collectionName), anyString());
    }

    @Test
    public void testEmptyBatchOperations() {
        List<Document> emptyDocs = new ArrayList<>();
        List<String> emptyIds = new ArrayList<>();

        assertDoesNotThrow(() -> batchProcessor.batchAddDocuments(emptyDocs));
        assertDoesNotThrow(() -> batchProcessor.batchDeleteDocuments(emptyIds));

        verify(mockClient, never()).set(anyString(), anyString(), anyDouble(), anyDouble(), any());
        verify(mockClient, never()).del(anyString(), anyString());
    }

    @Test
    public void testBatchProcessingWithException() {
        List<Document> documents = Arrays.asList(new Document());
        documents.get(0).setUniqueId("test-doc");
        documents.get(0).setPageContent("Test content");

        // Mock client to throw exception
        when(mockClient.set(anyString(), anyString(), anyDouble(), anyDouble(), any()))
                .thenThrow(new RuntimeException("Client error"));

        // Execute and verify exception
        assertThrows(Exception.class, () -> batchProcessor.batchAddDocuments(documents));
    }

    @Test
    public void testShutdown() {
        assertDoesNotThrow(() -> batchProcessor.shutdown());
    }

}