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

import com.alibaba.langengine.core.embeddings.Embeddings;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bson.Document;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Slf4j
@Data
public class AtlasService {

    private final String connectionString;
    private final String databaseName;
    private final String collectionName;
    private AtlasParam atlasParam;
    private volatile MongoClient mongoClient;
    private volatile MongoDatabase database;
    private volatile MongoCollection<Document> collection;
    private final ReentrantReadWriteLock serviceLock = new ReentrantReadWriteLock();
    private volatile boolean initialized = false;

    public AtlasService(String connectionString, String databaseName, String collectionName, AtlasParam atlasParam) {
        validateInputs(connectionString, databaseName, collectionName);
        
        this.connectionString = connectionString;
        this.databaseName = databaseName;
        this.collectionName = collectionName;
        this.atlasParam = atlasParam;
        
        initializeConnection();
    }

    private void validateInputs(String connectionString, String databaseName, String collectionName) {
        if (StringUtils.isBlank(connectionString)) {
            throw new AtlasException("INVALID_CONFIG", "Connection string cannot be null or empty");
        }
        if (StringUtils.isBlank(databaseName)) {
            throw new AtlasException("INVALID_CONFIG", "Database name cannot be null or empty");
        }
        if (StringUtils.isBlank(collectionName)) {
            throw new AtlasException("INVALID_CONFIG", "Collection name cannot be null or empty");
        }
    }

    private void initializeConnection() {
        serviceLock.writeLock().lock();
        try {
            if (initialized) {
                return;
            }
            
            this.mongoClient = AtlasConnectionManager.getClient(connectionString);
            this.database = mongoClient.getDatabase(databaseName);
            this.collection = database.getCollection(collectionName, Document.class);
            this.initialized = true;
            
            log.info("AtlasService initialized: database={}, collection={}", databaseName, collectionName);
        } catch (Exception e) {
            throw new AtlasException("CONNECTION_FAILED", "Failed to initialize Atlas connection", e);
        } finally {
            serviceLock.writeLock().unlock();
        }
    }

    private void ensureInitialized() {
        if (!initialized) {
            initializeConnection();
        }
    }

    /**
     * Add documents to Atlas Vector Search
     */
    public void addDocuments(List<com.alibaba.langengine.core.indexes.Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.warn("No documents to add");
            return;
        }

        serviceLock.readLock().lock();
        try {
            ensureInitialized();
            
            AtlasParam param = loadParam();
            String fieldNameUniqueId = param.getFieldNameUniqueId();
            String fieldNamePageContent = param.getFieldNamePageContent();
            String fieldNameEmbedding = param.getFieldNameEmbedding();

            List<Document> bsonDocuments = convertToBsonDocuments(documents, fieldNameUniqueId, 
                fieldNamePageContent, fieldNameEmbedding);

            AtlasBatchProcessor.processBatch(collection, bsonDocuments);
            log.info("Successfully added {} documents to Atlas collection", documents.size());
        } catch (AtlasException e) {
            throw e;
        } catch (Exception e) {
            throw new AtlasException("INSERT_FAILED", "Failed to add documents", e);
        } finally {
            serviceLock.readLock().unlock();
        }
    }

    /**
     * Add documents asynchronously
     */
    public CompletableFuture<Void> addDocumentsAsync(List<com.alibaba.langengine.core.indexes.Document> documents) {
        return CompletableFuture.runAsync(() -> addDocuments(documents));
    }

    private List<Document> convertToBsonDocuments(List<com.alibaba.langengine.core.indexes.Document> documents,
            String fieldNameUniqueId, String fieldNamePageContent, String fieldNameEmbedding) {
        List<Document> bsonDocuments = Lists.newArrayList();
        for (com.alibaba.langengine.core.indexes.Document document : documents) {
            validateDocument(document);
            
            Document bsonDoc = new Document();
            bsonDoc.append(fieldNameUniqueId, NumberUtils.toLong(document.getUniqueId()));
            bsonDoc.append(fieldNamePageContent, document.getPageContent());
            
            List<Double> embeddings = document.getEmbedding();
            if (embeddings == null || embeddings.isEmpty()) {
                throw new AtlasException("INVALID_DOCUMENT", "Document embedding cannot be null or empty");
            }
            bsonDoc.append(fieldNameEmbedding, embeddings);
            
            bsonDocuments.add(bsonDoc);
        }
        return bsonDocuments;
    }

    private void validateDocument(com.alibaba.langengine.core.indexes.Document document) {
        if (document == null) {
            throw new AtlasException("INVALID_DOCUMENT", "Document cannot be null");
        }
        if (StringUtils.isBlank(document.getUniqueId())) {
            throw new AtlasException("INVALID_DOCUMENT", "Document uniqueId cannot be null or empty");
        }
        if (StringUtils.isBlank(document.getPageContent())) {
            throw new AtlasException("INVALID_DOCUMENT", "Document pageContent cannot be null or empty");
        }
    }

    /**
     * Vector similarity search
     */
    public List<com.alibaba.langengine.core.indexes.Document> similaritySearch(List<Double> embeddings, int k) {
        validateSearchInputs(embeddings, k);
        
        serviceLock.readLock().lock();
        try {
            ensureInitialized();
            
            AtlasParam param = loadParam();
            String fieldNameUniqueId = param.getFieldNameUniqueId();
            String fieldNamePageContent = param.getFieldNamePageContent();
            String fieldNameEmbedding = param.getFieldNameEmbedding();
            String vectorIndexName = param.getVectorIndexName();
            int numCandidates = Math.max(param.getNumCandidates(), k);

            List<Document> pipeline = buildSearchPipeline(fieldNameEmbedding, fieldNameUniqueId, 
                fieldNamePageContent, vectorIndexName, embeddings, numCandidates, k);

            List<com.alibaba.langengine.core.indexes.Document> documents = Lists.newArrayList();
            for (Document result : collection.aggregate(pipeline)) {
                com.alibaba.langengine.core.indexes.Document document = convertToDocument(result, 
                    fieldNameUniqueId, fieldNamePageContent);
                documents.add(document);
            }

            log.debug("Found {} similar documents", documents.size());
            return documents;
        } catch (AtlasException e) {
            throw e;
        } catch (Exception e) {
            throw new AtlasException("SEARCH_FAILED", "Failed to perform similarity search", e);
        } finally {
            serviceLock.readLock().unlock();
        }
    }

    /**
     * Perform similarity search asynchronously
     */
    public CompletableFuture<List<com.alibaba.langengine.core.indexes.Document>> similaritySearchAsync(
            List<Double> embeddings, int k) {
        return CompletableFuture.supplyAsync(() -> similaritySearch(embeddings, k));
    }

    private void validateSearchInputs(List<Double> embeddings, int k) {
        if (embeddings == null || embeddings.isEmpty()) {
            throw new AtlasException("INVALID_SEARCH", "Embeddings cannot be null or empty");
        }
        if (k <= 0) {
            throw new AtlasException("INVALID_SEARCH", "k must be greater than 0");
        }
    }

    private List<Document> buildSearchPipeline(String fieldNameEmbedding, String fieldNameUniqueId, 
            String fieldNamePageContent, String vectorIndexName, List<Double> embeddings, 
            int numCandidates, int k) {
        List<Document> pipeline = Lists.newArrayList();
        pipeline.add(new Document("$vectorSearch", new Document()
            .append("index", vectorIndexName)
            .append("path", fieldNameEmbedding)
            .append("queryVector", embeddings)
            .append("numCandidates", numCandidates)
            .append("limit", k)));
        pipeline.add(new Document("$project", new Document()
            .append(fieldNameUniqueId, 1)
            .append(fieldNamePageContent, 1)
            .append("score", new Document("$meta", "vectorSearchScore"))));
        return pipeline;
    }

    private com.alibaba.langengine.core.indexes.Document convertToDocument(Document result, 
            String fieldNameUniqueId, String fieldNamePageContent) {
        com.alibaba.langengine.core.indexes.Document document = new com.alibaba.langengine.core.indexes.Document();
        document.setUniqueId(String.valueOf(result.get(fieldNameUniqueId)));
        document.setPageContent((String) result.get(fieldNamePageContent));
        Object scoreObj = result.get("score");
        if (scoreObj != null) {
            document.setScore(((Number) scoreObj).doubleValue());
        }
        return document;
    }

    /**
     * Initialize collection and vector search index
     */
    public void init(Embeddings embedding) {
        try {
            AtlasParam param = loadParam();
            AtlasParam.InitParam initParam = param.getInitParam();
            
            if (initParam.getFieldEmbeddingsDimension() <= 0) {
                List<com.alibaba.langengine.core.indexes.Document> embeddingDocuments = embedding.embedTexts(Lists.newArrayList("test"));
                com.alibaba.langengine.core.indexes.Document document = embeddingDocuments.get(0);
                initParam.setFieldEmbeddingsDimension(document.getEmbedding().size());
            }
            
            log.info("Atlas collection initialized with embedding dimension: {}", initParam.getFieldEmbeddingsDimension());
        } catch (Exception e) {
            throw new AtlasException("Failed to initialize Atlas collection", e);
        }
    }

    /**
     * Load parameters with defaults
     */
    private AtlasParam loadParam() {
        if (atlasParam == null) {
            atlasParam = new AtlasParam();
        }
        return atlasParam;
    }

    /**
     * Close MongoDB connection safely
     */
    public void close() {
        serviceLock.writeLock().lock();
        try {
            if (initialized) {
                AtlasConnectionManager.closeConnection(connectionString);
                mongoClient = null;
                database = null;
                collection = null;
                initialized = false;
                log.info("Atlas connection closed successfully");
            }
        } catch (Exception e) {
            log.warn("Error closing Atlas connection: {}", e.getMessage());
        } finally {
            serviceLock.writeLock().unlock();
        }
    }

    /**
     * Check if the connection is active
     */
    public boolean isConnected() {
        serviceLock.readLock().lock();
        try {
            if (!initialized || mongoClient == null || database == null) {
                return false;
            }
            database.runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            log.debug("Connection check failed: {}", e.getMessage());
            return false;
        } finally {
            serviceLock.readLock().unlock();
        }
    }

    /**
     * Get connection statistics
     */
    public String getConnectionStats() {
        serviceLock.readLock().lock();
        try {
            if (!initialized) {
                return "Not initialized";
            }
            return String.format("Connected to %s/%s, Collection: %s", 
                databaseName, collectionName, isConnected() ? "Active" : "Inactive");
        } finally {
            serviceLock.readLock().unlock();
        }
    }

}