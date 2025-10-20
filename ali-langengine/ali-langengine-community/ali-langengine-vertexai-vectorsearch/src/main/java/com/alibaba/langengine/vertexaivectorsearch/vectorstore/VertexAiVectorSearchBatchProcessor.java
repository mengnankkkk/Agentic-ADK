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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Slf4j
public class VertexAiVectorSearchBatchProcessor {

    private final VertexAiVectorSearchService service;
    private final int batchSize;
    private final ExecutorService executorService;

    public VertexAiVectorSearchBatchProcessor(VertexAiVectorSearchService service) {
        this(service, 100); // Default batch size
    }

    public VertexAiVectorSearchBatchProcessor(VertexAiVectorSearchService service, int batchSize) {
        this.service = service;
        this.batchSize = batchSize;
        this.executorService = Executors.newFixedThreadPool(4); // Default thread pool size
    }

    /**
     * Process documents in batches synchronously
     *
     * @param documents List of documents to process
     */
    public void processBatch(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            log.warn("No documents to process in batch");
            return;
        }

        log.info("Starting batch processing for {} documents with batch size {}", documents.size(), batchSize);

        try {
            for (int i = 0; i < documents.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, documents.size());
                List<Document> batch = documents.subList(i, endIndex);
                
                log.debug("Processing batch {}-{} of {}", i + 1, endIndex, documents.size());
                service.addDocuments(batch);
            }
            
            log.info("Completed batch processing for {} documents", documents.size());
        } catch (Exception e) {
            throw new VertexAiVectorSearchException("BATCH_PROCESSING_FAILED", 
                "Failed to process documents in batch", e);
        }
    }

    /**
     * Process documents in batches asynchronously
     *
     * @param documents List of documents to process
     * @return CompletableFuture for async processing
     */
    public CompletableFuture<Void> processBatchAsync(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            log.warn("No documents to process in async batch");
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            processBatch(documents);
        }, executorService);
    }

    /**
     * Process multiple batches concurrently
     *
     * @param documents List of documents to process
     * @param concurrency Number of concurrent batches
     * @return CompletableFuture for async processing
     */
    public CompletableFuture<Void> processBatchConcurrent(List<Document> documents, int concurrency) {
        if (CollectionUtils.isEmpty(documents)) {
            log.warn("No documents to process in concurrent batch");
            return CompletableFuture.completedFuture(null);
        }

        log.info("Starting concurrent batch processing for {} documents with {} threads", 
            documents.size(), concurrency);

        CompletableFuture<Void>[] futures = new CompletableFuture[concurrency];
        int documentsPerThread = documents.size() / concurrency;
        int remainder = documents.size() % concurrency;

        int startIndex = 0;
        for (int i = 0; i < concurrency; i++) {
            int endIndex = startIndex + documentsPerThread + (i < remainder ? 1 : 0);
            List<Document> threadDocuments = documents.subList(startIndex, endIndex);
            
            futures[i] = CompletableFuture.runAsync(() -> {
                processBatch(threadDocuments);
            }, executorService);
            
            startIndex = endIndex;
        }

        return CompletableFuture.allOf(futures);
    }

    /**
     * Delete documents in batches
     *
     * @param documentIds List of document IDs to delete
     */
    public void deleteBatch(List<String> documentIds) {
        if (CollectionUtils.isEmpty(documentIds)) {
            log.warn("No document IDs to delete in batch");
            return;
        }

        log.info("Starting batch deletion for {} document IDs with batch size {}", 
            documentIds.size(), batchSize);

        try {
            for (int i = 0; i < documentIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, documentIds.size());
                List<String> batch = documentIds.subList(i, endIndex);
                
                log.debug("Deleting batch {}-{} of {}", i + 1, endIndex, documentIds.size());
                service.deleteDocuments(batch);
            }
            
            log.info("Completed batch deletion for {} document IDs", documentIds.size());
        } catch (Exception e) {
            throw new VertexAiVectorSearchException("BATCH_DELETION_FAILED", 
                "Failed to delete documents in batch", e);
        }
    }

    /**
     * Shutdown the batch processor
     */
    public void shutdown() {
        log.info("Shutting down VertexAiVectorSearchBatchProcessor");
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.warn("ExecutorService did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("VertexAiVectorSearchBatchProcessor shutdown completed");
    }

}
