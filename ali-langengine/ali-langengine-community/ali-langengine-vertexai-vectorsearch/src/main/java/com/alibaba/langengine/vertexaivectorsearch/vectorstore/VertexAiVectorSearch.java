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

import com.alibaba.fastjson.JSON;
import com.alibaba.langengine.core.embeddings.Embeddings;
import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.core.vectorstore.VectorStore;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.langengine.vertexaivectorsearch.VertexAiVectorSearchConfiguration.*;


@Slf4j
@Data
@EqualsAndHashCode(callSuper=false)
public class VertexAiVectorSearch extends VectorStore {

    /**
     * Embedding model
     */
    private Embeddings embedding;

    /**
     * Google Cloud Project ID
     */
    private final String projectId;

    /**
     * Google Cloud Location/Region
     */
    private final String location;

    /**
     * Index Display Name
     */
    private final String indexDisplayName;

    /**
     * Index Endpoint Display Name
     */
    private final String indexEndpointDisplayName;

    private final VertexAiVectorSearchService vertexAiService;

    private final VertexAiVectorSearchBatchProcessor batchProcessor;

    public VertexAiVectorSearch(String projectId, String location, String indexDisplayName, String indexEndpointDisplayName) {
        this(projectId, location, indexDisplayName, indexEndpointDisplayName, null);
    }

    public VertexAiVectorSearch(String projectId, String location, String indexDisplayName, 
                              String indexEndpointDisplayName, VertexAiVectorSearchParam param) {
        this.projectId = projectId != null ? projectId : VERTEX_AI_PROJECT_ID;
        this.location = location != null ? location : VERTEX_AI_LOCATION;
        this.indexDisplayName = indexDisplayName != null ? indexDisplayName : DEFAULT_INDEX_DISPLAY_NAME;
        this.indexEndpointDisplayName = indexEndpointDisplayName != null ? indexEndpointDisplayName : DEFAULT_ENDPOINT_DISPLAY_NAME;
        
        validateConfiguration();
        
        this.vertexAiService = new VertexAiVectorSearchService(this.projectId, this.location, 
            this.indexDisplayName, this.indexEndpointDisplayName, param);
        this.batchProcessor = new VertexAiVectorSearchBatchProcessor(this.vertexAiService);
    }

    private void validateConfiguration() {
        if (this.projectId == null || this.projectId.trim().isEmpty()) {
            throw new VertexAiVectorSearchException("INVALID_CONFIG", 
                "Project ID is required. Set VERTEX_AI_PROJECT_ID environment variable or system property vertex.ai.project.id");
        }
        if (this.location == null || this.location.trim().isEmpty()) {
            throw new VertexAiVectorSearchException("INVALID_CONFIG", 
                "Location is required. Set VERTEX_AI_LOCATION environment variable or system property vertex.ai.location");
        }
        if (this.indexDisplayName == null || this.indexDisplayName.trim().isEmpty()) {
            throw new VertexAiVectorSearchException("INVALID_CONFIG", 
                "Index display name is required");
        }
        if (this.indexEndpointDisplayName == null || this.indexEndpointDisplayName.trim().isEmpty()) {
            throw new VertexAiVectorSearchException("INVALID_CONFIG", 
                "Index endpoint display name is required");
        }
    }

    /**
     * Initialize Vertex AI Vector Search index and endpoint
     */
    public void init() {
        synchronized (this) {
            try {
                vertexAiService.init(embedding);
            } catch (Exception e) {
                throw new VertexAiVectorSearchException("INIT_FAILED", "init vertex ai vector search failed", e);
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
        batchProcessor.processBatch(documents);
    }

    /**
     * Add documents asynchronously
     */
    public CompletableFuture<Void> addDocumentsAsync(List<Document> documents) {
        return CompletableFuture.runAsync(() -> {
            addDocuments(documents);
        });
    }

    /**
     * Add documents with concurrent batch processing
     */
    public CompletableFuture<Void> addDocumentsConcurrent(List<Document> documents, int concurrency) {
        if (CollectionUtils.isEmpty(documents)) {
            return CompletableFuture.completedFuture(null);
        }
        documents = embedding.embedDocument(documents);
        return batchProcessor.processBatchConcurrent(documents, concurrency);
    }

    @Override
    public void addDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }
        
        log.info("Adding {} documents to Vertex AI Vector Search", documents.size());
        
        try {
            documents = embedding.embedDocument(documents);
            vertexAiService.addDocuments(documents);
            
            log.info("Successfully added {} documents", documents.size());
        } catch (Exception e) {
            log.error("Failed to add documents", e);
            throw new VertexAiVectorSearchException("ADD_DOCUMENTS_FAILED", "Failed to add documents", e);
        }
    }

    @Override
    public List<Document> similaritySearch(String query, int k) {
        if (query == null || query.trim().isEmpty()) {
            throw new VertexAiVectorSearchException("INVALID_QUERY", "Query cannot be null or empty");
        }
        
        log.info("Performing similarity search for query: '{}' with k={}", query, k);
        
        try {
            // Embed the query
            List<String> queryEmbeddingStrings = embedding.embedQuery(query, k);
            List<Double> queryEmbedding = queryEmbeddingStrings.stream()
                .map(Double::parseDouble)
                .collect(java.util.stream.Collectors.toList());
            
            // Perform search
            List<Document> results = vertexAiService.similaritySearch(query, queryEmbedding, k);
            
            log.info("Similarity search returned {} results", results.size());
            return results;
        } catch (Exception e) {
            log.error("Failed to perform similarity search", e);
            throw new VertexAiVectorSearchException("SIMILARITY_SEARCH_FAILED", "Failed to perform similarity search", e);
        }
    }

    public List<Document> similaritySearchByVector(List<Double> embedding, int k) {
        if (CollectionUtils.isEmpty(embedding)) {
            throw new VertexAiVectorSearchException("INVALID_EMBEDDING", "Embedding vector cannot be null or empty");
        }
        
        log.info("Performing similarity search by vector with k={}", k);
        
        try {
            List<Document> results = vertexAiService.similaritySearch(null, embedding, k);
            
            log.info("Similarity search by vector returned {} results", results.size());
            return results;
        } catch (Exception e) {
            log.error("Failed to perform similarity search by vector", e);
            throw new VertexAiVectorSearchException("SIMILARITY_SEARCH_FAILED", "Failed to perform similarity search by vector", e);
        }
    }

    @Override
    public List<Document> similaritySearch(String query, int k, Double maxDistanceValue, Integer type) {
        // For now, we'll ignore maxDistanceValue and type parameters and delegate to the main method
        // These could be implemented later based on Vertex AI Vector Search capabilities
        return similaritySearch(query, k);
    }

    /**
     * Delete documents by IDs
     */
    public void deleteDocuments(List<String> documentIds) {
        if (CollectionUtils.isEmpty(documentIds)) {
            log.warn("No document IDs provided for deletion");
            return;
        }
        
        log.info("Deleting {} documents from Vertex AI Vector Search", documentIds.size());
        
        try {
            vertexAiService.deleteDocuments(documentIds);
            log.info("Successfully deleted {} documents", documentIds.size());
        } catch (Exception e) {
            log.error("Failed to delete documents", e);
            throw new VertexAiVectorSearchException("DELETE_DOCUMENTS_FAILED", "Failed to delete documents", e);
        }
    }

    /**
     * Delete documents by IDs in batch
     */
    public void deleteDocumentsBatch(List<String> documentIds) {
        if (CollectionUtils.isEmpty(documentIds)) {
            log.warn("No document IDs provided for batch deletion");
            return;
        }
        
        log.info("Batch deleting {} documents from Vertex AI Vector Search", documentIds.size());
        
        try {
            batchProcessor.deleteBatch(documentIds);
            log.info("Successfully batch deleted {} documents", documentIds.size());
        } catch (Exception e) {
            log.error("Failed to batch delete documents", e);
            throw new VertexAiVectorSearchException("BATCH_DELETE_FAILED", "Failed to batch delete documents", e);
        }
    }

    /**
     * Get configuration information
     */
    public String getConfigInfo() {
        return String.format("VertexAiVectorSearch[projectId=%s, location=%s, indexDisplayName=%s, indexEndpointDisplayName=%s]", 
            projectId, location, indexDisplayName, indexEndpointDisplayName);
    }

    /**
     * Close the vector store and clean up resources
     */
    public void close() {
        try {
            if (batchProcessor != null) {
                batchProcessor.shutdown();
            }
            if (vertexAiService != null) {
                vertexAiService.close();
            }
            log.info("VertexAiVectorSearch closed successfully");
        } catch (Exception e) {
            log.error("Error closing VertexAiVectorSearch", e);
            throw new VertexAiVectorSearchException("CLOSE_FAILED", "Failed to close VertexAiVectorSearch", e);
        }
    }

}
