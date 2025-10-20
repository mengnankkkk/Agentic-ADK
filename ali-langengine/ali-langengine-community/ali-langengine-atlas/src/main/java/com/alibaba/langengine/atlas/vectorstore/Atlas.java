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

import com.alibaba.fastjson.JSON;
import com.alibaba.langengine.core.embeddings.Embeddings;
import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.core.vectorstore.VectorStore;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

import static com.alibaba.langengine.atlas.AtlasConfiguration.ATLAS_CONNECTION_STRING;
import static com.alibaba.langengine.atlas.AtlasConfiguration.ATLAS_DATABASE_NAME;


@Slf4j
@Data
public class Atlas extends VectorStore {

    /**
     * Embedding model
     */
    private Embeddings embedding;

    /**
     * Collection name
     */
    private final String collection;

    /**
     * Database name
     */
    private final String database;

    private final AtlasService atlasService;

    public Atlas(String collection) {
        this(collection, null);
    }

    public Atlas(String collection, String database) {
        this(collection, database, null);
    }

    public Atlas(String collection, String database, AtlasParam atlasParam) {
        this.collection = collection;
        this.database = database != null ? database : ATLAS_DATABASE_NAME;
        String connectionString = ATLAS_CONNECTION_STRING;
        atlasService = new AtlasService(connectionString, this.database, collection, atlasParam);
    }

    /**
     * Initialize Atlas Vector Search collection and index
     */
    public void init() {
        synchronized (this) {
            try {
                atlasService.init(embedding);
            } catch (Exception e) {
                throw new AtlasException("INIT_FAILED", "init atlas failed", e);
            }
        }
    }

    /**
     * Add documents with batch processing
     */
    public void addDocumentsBatch(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }
        documents = embedding.embedDocument(documents);
        atlasService.addDocuments(documents);
    }

    /**
     * Add documents asynchronously
     */
    public java.util.concurrent.CompletableFuture<Void> addDocumentsAsync(List<Document> documents) {
        return java.util.concurrent.CompletableFuture.runAsync(() -> {
            addDocuments(documents);
        });
    }

    /**
     * Perform similarity search with timeout
     */
    public List<Document> similaritySearchWithTimeout(String query, int k, long timeoutMs) {
        java.util.concurrent.CompletableFuture<List<Document>> future = 
            java.util.concurrent.CompletableFuture.supplyAsync(() -> similaritySearch(query, k));
        
        try {
            return future.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new AtlasException("SEARCH_TIMEOUT", "Search operation timed out", e);
        }
    }

    /**
     * Get service statistics
     */
    public String getStats() {
        return atlasService.getConnectionStats();
    }

    /**
     * Check if service is healthy
     */
    public boolean isHealthy() {
        return atlasService.isConnected();
    }

    @Override
    public void addDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }
        documents = embedding.embedDocument(documents);
        atlasService.addDocuments(documents);
    }

    @Override
    public List<Document> similaritySearch(String query, int k, Double maxDistanceValue, Integer type) {
        List<String> embeddingStrings = embedding.embedQuery(query, k);
        if (CollectionUtils.isEmpty(embeddingStrings) || !embeddingStrings.get(0).startsWith("[")) {
            return Lists.newArrayList();
        }
        List<Double> embeddings = JSON.parseArray(embeddingStrings.get(0), Double.class);
        return atlasService.similaritySearch(embeddings, k);
    }

}