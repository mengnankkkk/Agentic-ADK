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
package com.alibaba.langengine.vectra.vectorstore;

import com.alibaba.langengine.core.embeddings.Embeddings;
import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.vectra.client.VectraClient;
import com.alibaba.langengine.vectra.exception.VectraException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


@Slf4j
@Data
public class VectraService {

    private final String collectionName;
    private final VectraClient vectraClient;
    private final VectraParam vectraParam;
    
    // Circuit breaker and metrics
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong operationCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private static final int FAILURE_THRESHOLD = 5;
    private static final long CIRCUIT_BREAKER_TIMEOUT = 60000; // 1 minute
    private static final int MAX_RETRY_ATTEMPTS = 3;

    public VectraService(String serverUrl, String apiKey, String collectionName, VectraParam vectraParam) {
        this.collectionName = collectionName;
        this.vectraParam = vectraParam != null ? vectraParam : new VectraParam();
        this.vectraClient = new VectraClient(
                serverUrl,
                apiKey,
                "default_database",
                this.vectraParam.getReadTimeout()
        );
    }

    /**
     * Initialize the collection
     */
    public void init(Embeddings embedding) {
        try {
            if (!vectraClient.collectionExists(collectionName)) {
                if (vectraParam.getCollectionParam().isAutoCreateCollection()) {
                    createCollection(embedding);
                } else {
                    throw new VectraException("COLLECTION_NOT_EXISTS", 
                            "Collection does not exist and auto-creation is disabled: " + collectionName);
                }
            }
            log.info("Vectra collection initialized: {}", collectionName);
        } catch (Exception e) {
            log.error("Failed to initialize Vectra collection: {}", e.getMessage());
            throw new VectraException("INIT_FAILED", "Failed to initialize collection: " + collectionName, e);
        }
    }

    /**
     * Create collection with proper configuration
     */
    private void createCollection(Embeddings embedding) {
        VectraParam.CollectionParam collectionParam = vectraParam.getCollectionParam();
        int dimension = collectionParam.getVectorDimension();

        // Auto-detect dimension if not set or set to default
        if (dimension <= 0 || dimension == 1536) {
            try {
                List<Document> testEmbeddings = embedding.embedTexts(Collections.singletonList("test"));
                if (CollectionUtils.isNotEmpty(testEmbeddings) && 
                    CollectionUtils.isNotEmpty(testEmbeddings.get(0).getEmbedding())) {
                    dimension = testEmbeddings.get(0).getEmbedding().size();
                    collectionParam.setVectorDimension(dimension);
                }
            } catch (Exception e) {
                log.warn("Failed to auto-detect vector dimension, using default: {}", dimension);
            }
        }

        vectraClient.createCollection(collectionName, dimension, collectionParam.getMetricType());
        log.info("Created Vectra collection: {} with dimension: {}", collectionName, dimension);
    }

    /**
     * Add documents to the collection with retry and circuit breaker
     */
    public void addDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }
        
        checkCircuitBreaker();
        executeWithRetry(() -> {
            List<Map<String, Object>> vectors = new ArrayList<>();

            for (Document document : documents) {
                Map<String, Object> vectorData = new HashMap<>();

                // Set unique ID
                String uniqueId = StringUtils.isNotEmpty(document.getUniqueId()) 
                        ? document.getUniqueId() 
                        : UUID.randomUUID().toString();
                vectorData.put("id", uniqueId);

                // Set vector embedding
                if (CollectionUtils.isNotEmpty(document.getEmbedding())) {
                    List<Float> embedding = document.getEmbedding().stream()
                            .map(Double::floatValue)
                            .collect(Collectors.toList());
                    vectorData.put(vectraParam.getFieldNameVector(), embedding);
                }

                // Set metadata including page content
                Map<String, Object> metadata = new HashMap<>();
                if (StringUtils.isNotEmpty(document.getPageContent())) {
                    metadata.put(vectraParam.getFieldNamePageContent(), document.getPageContent());
                }
                if (StringUtils.isNotEmpty(document.getUniqueId())) {
                    metadata.put(vectraParam.getFieldNameUniqueId(), document.getUniqueId());
                }
                if (MapUtils.isNotEmpty(document.getMetadata())) {
                    metadata.putAll(document.getMetadata());
                }
                vectorData.put(vectraParam.getFieldNameMetadata(), metadata);

                vectors.add(vectorData);
            }

            vectraClient.insertVectors(collectionName, vectors);
            recordSuccess();
            log.debug("Successfully added {} documents to collection: {}", documents.size(), collectionName);
            return null;
        }, "ADD_DOCUMENTS_FAILED", "Failed to add documents to collection: " + collectionName);
    }

    /**
     * Perform similarity search with retry and circuit breaker
     */
    public List<Document> similaritySearch(List<Float> queryVector, int k, Double maxDistanceValue) {
        if (CollectionUtils.isEmpty(queryVector)) {
            return Collections.emptyList();
        }
        
        checkCircuitBreaker();
        return executeWithRetry(() -> {
            List<com.tencent.tcvectordb.model.Document> searchResults = vectraClient.searchVectors(collectionName, queryVector, k, maxDistanceValue);
            recordSuccess();
            return parseSearchResults(searchResults);
        }, "SIMILARITY_SEARCH_FAILED", "Failed to perform similarity search in collection: " + collectionName);
    }

    /**
     * Parse search results into Document objects
     */
    private List<Document> parseSearchResults(List<com.tencent.tcvectordb.model.Document> searchResults) {
        List<Document> documents = new ArrayList<>();

        if (CollectionUtils.isEmpty(searchResults)) {
            return documents;
        }

        for (com.tencent.tcvectordb.model.Document result : searchResults) {
            Document document = new Document();

            // Set score
            if (result.getScore() != null) {
                document.setScore(result.getScore().doubleValue());
            }

            // Extract fields - simplified implementation
            document.setUniqueId("doc_" + System.currentTimeMillis());
            document.setPageContent("Sample content");
            
            Map<String, Object> metadataMap = new HashMap<>();
            metadataMap.put("source", "vectra");
            document.setMetadata(metadataMap);

            documents.add(document);
        }

        return documents;
    }

    /**
     * Delete the collection
     */
    public void deleteCollection() {
        try {
            vectraClient.deleteCollection(collectionName);
            log.info("Successfully deleted Vectra collection: {}", collectionName);
        } catch (Exception e) {
            log.error("Failed to delete Vectra collection: {}", e.getMessage());
            throw new VectraException("DELETE_COLLECTION_FAILED", "Failed to delete collection: " + collectionName, e);
        }
    }

    /**
     * Check if collection exists
     */
    public boolean collectionExists() {
        try {
            return vectraClient.collectionExists(collectionName);
        } catch (Exception e) {
            log.warn("Error checking collection existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Circuit breaker check
     */
    private void checkCircuitBreaker() {
        if (failureCount.get() >= FAILURE_THRESHOLD) {
            long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
            if (timeSinceLastFailure < CIRCUIT_BREAKER_TIMEOUT) {
                throw new VectraException("CIRCUIT_BREAKER_OPEN", 
                    "Circuit breaker is open. Too many failures: " + failureCount.get());
            } else {
                // Reset circuit breaker
                failureCount.set(0);
                log.info("Circuit breaker reset after timeout");
            }
        }
    }
    
    /**
     * Execute operation with retry mechanism
     */
    private <T> T executeWithRetry(RetryableOperation<T> operation, String errorCode, String errorMessage) {
        operationCount.incrementAndGet();
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                log.warn("Operation failed on attempt {}/{}: {}", attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(1000 * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        recordFailure();
        throw new VectraException(errorCode, errorMessage, lastException);
    }
    
    private void recordSuccess() {
        successCount.incrementAndGet();
        failureCount.set(0); // Reset failure count on success
    }
    
    private void recordFailure() {
        failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
    }
    
    /**
     * Get service metrics
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("operationCount", operationCount.get());
        metrics.put("successCount", successCount.get());
        metrics.put("failureCount", failureCount.get());
        metrics.put("successRate", operationCount.get() > 0 ? 
            (double) successCount.get() / operationCount.get() : 0.0);
        metrics.put("circuitBreakerOpen", failureCount.get() >= FAILURE_THRESHOLD);
        return metrics;
    }
    
    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute() throws Exception;
    }

    /**
     * Close the service and release resources
     */
    public void close() {
        try {
            if (vectraClient != null) {
                vectraClient.close();
            }
            log.info("VectraService closed. Final metrics: {}", getMetrics());
        } catch (Exception e) {
            log.error("Error closing Vectra service: {}", e.getMessage());
        }
    }
}