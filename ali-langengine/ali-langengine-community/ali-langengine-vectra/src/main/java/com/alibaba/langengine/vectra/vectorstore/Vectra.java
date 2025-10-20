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

import com.alibaba.fastjson.JSON;
import com.alibaba.langengine.core.embeddings.Embeddings;
import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.core.vectorstore.VectorStore;
import com.alibaba.langengine.vectra.exception.VectraException;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

import static com.alibaba.langengine.vectra.VectraConfiguration.*;


@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class Vectra extends VectorStore {

    /**
     * Embedding model for vector generation
     */
    private Embeddings embedding;

    /**
     * Collection name in Vectra database
     */
    private final String collectionName;

    /**
     * Vectra service for database operations
     */
    private final VectraService vectraService;

    /**
     * Create Vectra instance with default configuration
     *
     * @param collectionName the name of the collection
     */
    public Vectra(String collectionName) {
        this(collectionName, null);
    }

    /**
     * Create Vectra instance with custom parameters
     *
     * @param collectionName the name of the collection
     * @param vectraParam custom parameters for Vectra configuration
     */
    public Vectra(String collectionName, VectraParam vectraParam) {
        this.collectionName = collectionName;
        
        String serverUrl = VECTRA_SERVER_URL;
        String apiKey = VECTRA_API_KEY;
        
        this.vectraService = new VectraService(serverUrl, apiKey, collectionName, vectraParam);
    }

    /**
     * Create Vectra instance with custom server URL
     *
     * @param serverUrl the Vectra server URL
     * @param collectionName the name of the collection
     * @param vectraParam custom parameters for Vectra configuration
     */
    public Vectra(String serverUrl, String collectionName, VectraParam vectraParam) {
        this.collectionName = collectionName;
        
        String apiKey = VECTRA_API_KEY;
        
        this.vectraService = new VectraService(serverUrl, apiKey, collectionName, vectraParam);
    }

    /**
     * Create Vectra instance with full configuration
     *
     * @param serverUrl the Vectra server URL
     * @param apiKey the API key for authentication
     * @param collectionName the name of the collection
     * @param vectraParam custom parameters for Vectra configuration
     */
    public Vectra(String serverUrl, String apiKey, String collectionName, VectraParam vectraParam) {
        this.collectionName = collectionName;
        this.vectraService = new VectraService(serverUrl, apiKey, collectionName, vectraParam);
    }

    /**
     * Initialize the Vectra collection
     * 
     * This method will:
     * 1. Check if the collection exists
     * 2. Create the collection if it doesn't exist (when auto-creation is enabled)
     * 3. Configure the collection with proper vector dimensions and index settings
     * 
     * Note: This method should be called before performing any vector operations
     */
    public void init() {
        try {
            if (embedding == null) {
                throw new VectraException("EMBEDDING_NOT_SET", "Embedding model must be set before initialization");
            }
            
            vectraService.init(embedding);
            log.info("Vectra vector store initialized successfully for collection: {}", collectionName);
        } catch (Exception e) {
            log.error("Failed to initialize Vectra vector store: {}", e.getMessage());
            throw new VectraException("INIT_FAILED", "Failed to initialize Vectra vector store", e);
        }
    }

    /**
     * Add documents to the vector store
     * 
     * This method will:
     * 1. Generate embeddings for documents that don't have them
     * 2. Insert the documents with their vectors and metadata into the collection
     * 
     * @param documents list of documents to add
     */
    @Override
    public void addDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }

        try {
            // Generate embeddings for documents
            documents = embedding.embedDocument(documents);
            
            // Add documents to Vectra
            vectraService.addDocuments(documents);
            
            log.debug("Successfully added {} documents to Vectra collection: {}", documents.size(), collectionName);
        } catch (Exception e) {
            log.error("Failed to add documents to Vectra: {}", e.getMessage());
            throw new VectraException("ADD_DOCUMENTS_FAILED", "Failed to add documents to Vectra", e);
        }
    }

    /**
     * Perform similarity search in the vector store
     * 
     * @param query the search query text
     * @param k the number of similar documents to return
     * @param maxDistanceValue maximum distance threshold for results (optional)
     * @param type search type (reserved for future use)
     * @return list of similar documents with similarity scores
     */
    @Override
    public List<Document> similaritySearch(String query, int k, Double maxDistanceValue, Integer type) {
        if (query == null || query.trim().isEmpty()) {
            return Lists.newArrayList();
        }

        try {
            // Generate query embedding
            List<String> embeddingStrings = embedding.embedQuery(query, k);
            if (CollectionUtils.isEmpty(embeddingStrings) || !embeddingStrings.get(0).startsWith("[")) {
                log.warn("Failed to generate valid embedding for query: {}", query);
                return Lists.newArrayList();
            }

            // Parse embedding vector
            List<Float> queryVector = JSON.parseArray(embeddingStrings.get(0), Float.class);
            
            // Perform similarity search
            List<Document> results = vectraService.similaritySearch(queryVector, k, maxDistanceValue);
            
            log.debug("Similarity search returned {} results for query: {}", results.size(), query);
            return results;
            
        } catch (Exception e) {
            log.error("Failed to perform similarity search: {}", e.getMessage());
            throw new VectraException("SIMILARITY_SEARCH_FAILED", "Failed to perform similarity search", e);
        }
    }

    /**
     * Perform similarity search using vector embedding directly
     * 
     * @param queryVector the query vector
     * @param k the number of similar documents to return
     * @param maxDistanceValue maximum distance threshold for results (optional)
     * @return list of similar documents with similarity scores
     */
    public List<Document> similaritySearchByVector(List<Float> queryVector, int k, Double maxDistanceValue) {
        try {
            return vectraService.similaritySearch(queryVector, k, maxDistanceValue);
        } catch (Exception e) {
            log.error("Failed to perform similarity search by vector: {}", e.getMessage());
            throw new VectraException("SIMILARITY_SEARCH_BY_VECTOR_FAILED", "Failed to perform similarity search by vector", e);
        }
    }

    /**
     * Check if the collection exists
     * 
     * @return true if the collection exists, false otherwise
     */
    public boolean collectionExists() {
        try {
            return vectraService.collectionExists();
        } catch (Exception e) {
            log.warn("Error checking collection existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Delete the collection
     * 
     * Warning: This operation will permanently delete all data in the collection
     */
    public void deleteCollection() {
        try {
            vectraService.deleteCollection();
            log.info("Successfully deleted Vectra collection: {}", collectionName);
        } catch (Exception e) {
            log.error("Failed to delete Vectra collection: {}", e.getMessage());
            throw new VectraException("DELETE_COLLECTION_FAILED", "Failed to delete collection", e);
        }
    }

    /**
     * Close the vector store and release resources
     * 
     * This method should be called when the vector store is no longer needed
     * to properly clean up connections and resources
     */
    public void close() {
        try {
            vectraService.close();
            log.debug("Vectra vector store closed successfully");
        } catch (Exception e) {
            log.error("Error closing Vectra vector store: {}", e.getMessage());
        }
    }

    /**
     * Get the collection name
     * 
     * @return the collection name
     */
    public String getCollectionName() {
        return collectionName;
    }
}