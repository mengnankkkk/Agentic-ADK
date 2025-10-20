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

import com.alibaba.langengine.atlas.AtlasConfiguration;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.InsertManyOptions;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Slf4j
public class AtlasBatchProcessor {

    private static final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "atlas-batch-processor");
        t.setDaemon(true);
        return t;
    });

    /**
     * Process documents in batches with retry mechanism
     */
    public static void processBatch(MongoCollection<Document> collection, List<Document> documents) {
        if (documents.isEmpty()) {
            return;
        }

        int batchSize = AtlasConfiguration.ATLAS_BATCH_SIZE;
        int totalBatches = (documents.size() + batchSize - 1) / batchSize;
        
        log.info("Processing {} documents in {} batches of size {}", documents.size(), totalBatches, batchSize);

        for (int i = 0; i < documents.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, documents.size());
            List<Document> batch = documents.subList(i, endIndex);
            
            processSingleBatch(collection, batch, (i / batchSize) + 1, totalBatches);
        }
    }

    /**
     * Process documents in batches asynchronously
     */
    public static CompletableFuture<Void> processBatchAsync(MongoCollection<Document> collection, List<Document> documents) {
        return CompletableFuture.runAsync(() -> processBatch(collection, documents), executorService);
    }

    private static void processSingleBatch(MongoCollection<Document> collection, List<Document> batch, int batchNum, int totalBatches) {
        int attempts = 0;
        int maxAttempts = AtlasConfiguration.ATLAS_MAX_RETRY_ATTEMPTS;
        
        while (attempts < maxAttempts) {
            try {
                InsertManyOptions options = new InsertManyOptions().ordered(false);
                collection.insertMany(batch, options);
                
                log.debug("Successfully processed batch {}/{} with {} documents", batchNum, totalBatches, batch.size());
                return;
                
            } catch (Exception e) {
                attempts++;
                log.warn("Batch {}/{} failed on attempt {}/{}: {}", batchNum, totalBatches, attempts, maxAttempts, e.getMessage());
                
                if (attempts >= maxAttempts) {
                    throw new AtlasException("BATCH_INSERT_FAILED", 
                        String.format("Failed to insert batch %d/%d after %d attempts", batchNum, totalBatches, maxAttempts), e);
                }
                
                try {
                    Thread.sleep(AtlasConfiguration.ATLAS_RETRY_DELAY_MS * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AtlasException("BATCH_INTERRUPTED", "Batch processing was interrupted", ie);
                }
            }
        }
    }

    /**
     * Shutdown the executor service
     */
    public static void shutdown() {
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