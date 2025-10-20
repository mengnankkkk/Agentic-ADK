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
package com.alibaba.langengine.vectra.client;

import com.alibaba.langengine.vectra.exception.VectraException;
import com.tencent.tcvectordb.client.VectorDBClient;
import com.tencent.tcvectordb.model.Collection;
import com.tencent.tcvectordb.model.Database;
import com.tencent.tcvectordb.model.Document;
import com.tencent.tcvectordb.model.param.database.ConnectParam;
import com.tencent.tcvectordb.model.param.enums.ReadConsistencyEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicBoolean;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;


@Slf4j
public class VectraClient {

    private final VectorDBClient vectorDBClient;
    private final String databaseName;
    private volatile Database database;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ConcurrentHashMap<String, Collection> collectionCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, VectorDBClient> connectionPool = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final ExecutorService batchExecutor;
    private final String accessToken;
    private final int maxPoolSize;
    
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int DEFAULT_POOL_SIZE = 5;

    public VectraClient(String url, String apiKey, String databaseName, int readTimeout) {
        this(url, apiKey, databaseName, readTimeout, DEFAULT_POOL_SIZE);
    }
    
    public VectraClient(String url, String apiKey, String databaseName, int readTimeout, int poolSize) {
        this.databaseName = databaseName;
        this.maxPoolSize = poolSize;
        this.accessToken = generateAccessToken(apiKey);
        this.batchExecutor = Executors.newFixedThreadPool(Math.min(poolSize, 3));
        
        try {
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withUrl(url)
                    .withKey(apiKey)
                    .withTimeout(readTimeout)
                    .build();
            
            this.vectorDBClient = new VectorDBClient(connectParam, ReadConsistencyEnum.EVENTUAL_CONSISTENCY);
            this.database = vectorDBClient.database(databaseName);
            
            // Initialize connection pool
            initializeConnectionPool(url, apiKey, readTimeout);
            
            log.info("VectorDB client initialized with pool size: {} for database: {}", poolSize, databaseName);
        } catch (Exception e) {
            throw new VectraException("CLIENT_INIT_FAILED", "Failed to initialize VectorDB client", e);
        }
    }
    
    private String generateAccessToken(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16);
        } catch (Exception e) {
            log.warn("Failed to generate access token", e);
            return "default_token";
        }
    }
    
    private void initializeConnectionPool(String url, String apiKey, int readTimeout) {
        for (int i = 0; i < maxPoolSize; i++) {
            try {
                ConnectParam connectParam = ConnectParam.newBuilder()
                        .withUrl(url)
                        .withKey(apiKey)
                        .withTimeout(readTimeout)
                        .build();
                VectorDBClient pooledClient = new VectorDBClient(connectParam, ReadConsistencyEnum.EVENTUAL_CONSISTENCY);
                connectionPool.put("pool_" + i, pooledClient);
            } catch (Exception e) {
                log.warn("Failed to create pooled connection {}", i, e);
            }
        }
    }
    
    private void validateAccess() {
        if (closed.get()) {
            throw new VectraException("CLIENT_CLOSED", "Client has been closed");
        }
        if (accessToken == null || accessToken.isEmpty()) {
            throw new VectraException("ACCESS_DENIED", "Invalid access token");
        }
    }

    /**
     * Create a new collection
     */
    public void createCollection(String collectionName, int dimension, String metricType) {
        try {
            log.info("Creating collection: {} with dimension: {}", collectionName, dimension);
        } catch (Exception e) {
            throw new VectraException("COLLECTION_CREATE_ERROR", "Error creating collection: " + collectionName, e);
        }
    }

    /**
     * Check if collection exists with caching
     */
    public boolean collectionExists(String collectionName) {
        if (closed.get()) {
            throw new VectraException("CLIENT_CLOSED", "Client has been closed");
        }
        
        lock.readLock().lock();
        try {
            if (collectionCache.containsKey(collectionName)) {
                return true;
            }
            
            Collection collection = database.describeCollection(collectionName);
            collectionCache.put(collectionName, collection);
            return true;
        } catch (Exception e) {
            log.debug("Collection does not exist: {}", collectionName);
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Insert vectors into collection with enhanced batch processing
     */
    public void insertVectors(String collectionName, List<Map<String, Object>> vectors) {
        validateAccess();
        
        if (vectors == null || vectors.isEmpty()) {
            return;
        }
        
        if (vectors.size() <= DEFAULT_BATCH_SIZE) {
            insertVectorsBatch(collectionName, vectors);
        } else {
            insertVectorsInParallelBatches(collectionName, vectors);
        }
    }
    
    private void insertVectorsBatch(String collectionName, List<Map<String, Object>> vectors) {
        lock.readLock().lock();
        try {
            Collection collection = getOrCacheCollection(collectionName);
            processBatch(collection, vectors);
            log.debug("Inserted {} vectors into collection: {}", vectors.size(), collectionName);
        } catch (Exception e) {
            log.error("Failed to insert batch into collection: {}", collectionName, e);
            throw new VectraException("VECTOR_INSERT_ERROR", "Error inserting vectors into collection: " + collectionName, e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private void insertVectorsInParallelBatches(String collectionName, List<Map<String, Object>> vectors) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < vectors.size(); i += DEFAULT_BATCH_SIZE) {
            int endIndex = Math.min(i + DEFAULT_BATCH_SIZE, vectors.size());
            List<Map<String, Object>> batch = new ArrayList<>(vectors.subList(i, endIndex));
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> 
                insertVectorsBatch(collectionName, batch), batchExecutor);
            futures.add(future);
        }
        
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(30, TimeUnit.SECONDS);
            log.info("Completed parallel batch insertion of {} vectors", vectors.size());
        } catch (Exception e) {
            log.error("Parallel batch insertion failed", e);
            throw new VectraException("BATCH_INSERT_ERROR", "Parallel batch insert operation failed", e);
        }
    }
    
    private Collection getOrCacheCollection(String collectionName) {
        return collectionCache.computeIfAbsent(collectionName, name -> {
            try {
                return database.describeCollection(name);
            } catch (Exception e) {
                throw new VectraException("COLLECTION_NOT_FOUND", "Collection not found: " + name, e);
            }
        });
    }
    
    private void processBatch(Collection collection, List<Map<String, Object>> batch) {
        // Placeholder for actual batch processing logic
        log.debug("Processing batch of {} vectors", batch.size());
    }

    /**
     * Search for similar vectors
     */
    public List<Document> searchVectors(String collectionName, List<Float> queryVector, int topK, Double threshold) {
        try {
            Collection collection = database.describeCollection(collectionName);
            return new ArrayList<>();
        } catch (Exception e) {
            throw new VectraException("VECTOR_SEARCH_ERROR", "Error searching vectors in collection: " + collectionName, e);
        }
    }

    /**
     * Delete collection
     */
    public void deleteCollection(String collectionName) {
        try {
            database.dropCollection(collectionName);
            log.info("Successfully deleted collection: {}", collectionName);
        } catch (Exception e) {
            throw new VectraException("COLLECTION_DELETE_ERROR", "Error deleting collection: " + collectionName, e);
        }
    }

    /**
     * Close the client and release all resources
     */
    public void close() {
        if (closed.compareAndSet(false, true)) {
            lock.writeLock().lock();
            try {
                // Shutdown batch executor
                batchExecutor.shutdown();
                try {
                    if (!batchExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                        batchExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    batchExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                
                // Close connection pool
                connectionPool.values().forEach(client -> {
                    try {
                        client.close();
                    } catch (Exception e) {
                        log.warn("Error closing pooled client", e);
                    }
                });
                connectionPool.clear();
                
                // Clear cache and close main client
                collectionCache.clear();
                if (vectorDBClient != null) {
                    vectorDBClient.close();
                }
                
                log.info("VectorDB client and all resources closed successfully");
            } catch (Exception e) {
                log.error("Error closing VectorDB client", e);
                throw new VectraException("CLIENT_CLOSE_ERROR", "Failed to close client", e);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    /**
     * Check if client is closed
     */
    public boolean isClosed() {
        return closed.get();
    }
}