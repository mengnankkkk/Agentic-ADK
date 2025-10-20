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
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Slf4j
public class Tile38BatchProcessor {

    private final int batchSize;
    private final ExecutorService executorService;
    private final Tile38Client client;
    private final String collectionName;

    public Tile38BatchProcessor(Tile38Client client, String collectionName, int batchSize) {
        this.client = client;
        this.collectionName = collectionName;
        this.batchSize = batchSize;
        this.executorService = Executors.newFixedThreadPool(
            Math.min(Runtime.getRuntime().availableProcessors(), 4));
    }

    public void batchAddDocuments(List<Document> documents) {
        if (documents.isEmpty()) return;

        List<List<Document>> batches = createBatches(documents);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (List<Document> batch : batches) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                processBatch(batch);
            }, executorService);
            futures.add(future);
        }

        // Wait for all batches to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .join();

        log.info("Batch processing completed for {} documents in {} batches", 
            documents.size(), batches.size());
    }

    public void batchDeleteDocuments(List<String> documentIds) {
        if (documentIds.isEmpty()) return;

        List<List<String>> batches = createIdBatches(documentIds);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (List<String> batch : batches) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (String id : batch) {
                    try {
                        client.del(collectionName, id);
                    } catch (Exception e) {
                        log.error("Failed to delete document {}", id, e);
                        throw new Tile38Exception("BATCH_DELETE_ERROR", 
                            "Failed to delete document: " + id, e);
                    }
                }
            }, executorService);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .join();

        log.info("Batch deletion completed for {} documents", documentIds.size());
    }

    private void processBatch(List<Document> batch) {
        for (Document document : batch) {
            try {
                // Process single document
                java.util.Map<String, String> fields = new java.util.HashMap<>();
                fields.put("content", document.getPageContent());
                
                if (document.getMetadata() != null) {
                    document.getMetadata().forEach((key, value) -> 
                        fields.put(key, value != null ? value.toString() : ""));
                }

                // Convert embedding to coordinates
                double lat = 0.0, lon = 0.0;
                if (document.getEmbedding() != null && !document.getEmbedding().isEmpty()) {
                    List<Double> embedding = document.getEmbedding();
                    if (embedding.size() >= 2) {
                        lat = embedding.get(0) * 90.0;
                        lon = embedding.get(1) * 180.0;
                    }
                }

                client.set(collectionName, document.getUniqueId(), lat, lon, fields);
            } catch (Exception e) {
                log.error("Failed to process document {}", document.getUniqueId(), e);
                throw new Tile38Exception("BATCH_PROCESS_ERROR", 
                    "Failed to process document: " + document.getUniqueId(), e);
            }
        }
    }

    private List<List<Document>> createBatches(List<Document> documents) {
        List<List<Document>> batches = new ArrayList<>();
        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            batches.add(documents.subList(i, end));
        }
        return batches;
    }

    private List<List<String>> createIdBatches(List<String> ids) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += batchSize) {
            int end = Math.min(i + batchSize, ids.size());
            batches.add(ids.subList(i, end));
        }
        return batches;
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}