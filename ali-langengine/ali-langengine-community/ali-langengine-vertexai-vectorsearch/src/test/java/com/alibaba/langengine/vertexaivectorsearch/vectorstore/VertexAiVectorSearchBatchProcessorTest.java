package com.alibaba.langengine.vertexaivectorsearch.vectorstore;

import com.alibaba.langengine.core.indexes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VertexAiVectorSearchBatchProcessorTest {

    @Mock
    private VertexAiVectorSearchService mockService;

    private VertexAiVectorSearchBatchProcessor batchProcessor;

    @BeforeEach
    void setUp() {
        batchProcessor = new VertexAiVectorSearchBatchProcessor(mockService, 2);
    }

    @Test
    void testConstructorWithDefaults() {
        VertexAiVectorSearchBatchProcessor processor = new VertexAiVectorSearchBatchProcessor(mockService);
        assertNotNull(processor);
    }

    @Test
    void testProcessBatch() {
        Document doc1 = new Document();
        doc1.setPageContent("content1");
        Document doc2 = new Document();
        doc2.setPageContent("content2");
        Document doc3 = new Document();
        doc3.setPageContent("content3");
        
        List<Document> docs = Arrays.asList(doc1, doc2, doc3);
        
        batchProcessor.processBatch(docs);
        
        // Should be called twice due to batch size of 2
        verify(mockService, times(2)).addDocuments(any());
    }

    @Test
    void testProcessBatchEmpty() {
        batchProcessor.processBatch(null);
        batchProcessor.processBatch(Arrays.asList());
        
        verify(mockService, never()).addDocuments(any());
    }

    @Test
    void testProcessBatchAsync() {
        Document doc = new Document();
        doc.setPageContent("content");
        List<Document> docs = Arrays.asList(doc);
        
        CompletableFuture<Void> future = batchProcessor.processBatchAsync(docs);
        
        assertDoesNotThrow(() -> future.get());
        verify(mockService).addDocuments(docs);
    }

    @Test
    void testProcessBatchAsyncEmpty() {
        CompletableFuture<Void> future = batchProcessor.processBatchAsync(null);
        
        assertDoesNotThrow(() -> future.get());
        verify(mockService, never()).addDocuments(any());
    }

    @Test
    void testProcessBatchConcurrent() {
        Document doc1 = new Document();
        doc1.setPageContent("content1");
        Document doc2 = new Document();
        doc2.setPageContent("content2");
        Document doc3 = new Document();
        doc3.setPageContent("content3");
        Document doc4 = new Document();
        doc4.setPageContent("content4");
        
        List<Document> docs = Arrays.asList(doc1, doc2, doc3, doc4);
        
        CompletableFuture<Void> future = batchProcessor.processBatchConcurrent(docs, 2);
        
        assertDoesNotThrow(() -> future.get());
        verify(mockService, atLeastOnce()).addDocuments(any());
    }

    @Test
    void testProcessBatchConcurrentEmpty() {
        CompletableFuture<Void> future = batchProcessor.processBatchConcurrent(null, 2);
        
        assertDoesNotThrow(() -> future.get());
        verify(mockService, never()).addDocuments(any());
    }

    @Test
    void testDeleteBatch() {
        List<String> ids = Arrays.asList("id1", "id2", "id3");
        
        batchProcessor.deleteBatch(ids);
        
        // Should be called twice due to batch size of 2
        verify(mockService, times(2)).deleteDocuments(any());
    }

    @Test
    void testDeleteBatchEmpty() {
        batchProcessor.deleteBatch(null);
        batchProcessor.deleteBatch(Arrays.asList());
        
        verify(mockService, never()).deleteDocuments(any());
    }

    @Test
    void testShutdown() {
        assertDoesNotThrow(() -> batchProcessor.shutdown());
    }
}