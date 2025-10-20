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
package com.alibaba.langengine.sqlitevss.vectorstore.service;

import com.alibaba.langengine.core.embeddings.Embeddings;
import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.sqlitevss.vectorstore.SqliteVssException;
import com.alibaba.langengine.sqlitevss.vectorstore.SqliteVssParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
public class SqliteVssService {

    private final SqliteVssClient client;
    private final SqliteVssParam param;
    private final Embeddings embeddings;

    public SqliteVssService(SqliteVssClient client, SqliteVssParam param, Embeddings embeddings) {
        this.client = client;
        this.param = param;
        this.embeddings = embeddings;
    }

    /**
     * Add a single document to the vector store
     */
    public void addDocument(Document document) {
        addDocuments(Collections.singletonList(document));
    }

    /**
     * Add multiple documents to the vector store
     */
    public void addDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }

        validateDocuments(documents);
        List<SqliteVssInsertRequest> requests = prepareInsertRequests(documents);
        
        try {
            client.insertDocumentsBatch(requests);
            log.info("Successfully added {} documents to collection '{}'", 
                     documents.size(), param.getCollectionName());
        } catch (Exception e) {
            throw SqliteVssException.documentError("Failed to add documents", e);
        }
    }

    /**
     * Search for similar documents
     */
    public List<Document> searchSimilarDocuments(String query, int topK, Double maxDistance) {
        if (StringUtils.isBlank(query)) {
            throw SqliteVssException.configError("Query cannot be null or empty");
        }

        // Generate query embedding
        List<Double> queryVector = null;
        if (embeddings != null) {
            try {
                List<Document> embeddedQuery = embeddings.embedTexts(Collections.singletonList(query));
                if (!embeddedQuery.isEmpty() && CollectionUtils.isNotEmpty(embeddedQuery.get(0).getEmbedding())) {
                    queryVector = embeddedQuery.get(0).getEmbedding();
                }
            } catch (Exception e) {
                log.warn("Failed to generate embedding for query, fallback to text search", e);
            }
        }

        SqliteVssSearchRequest searchRequest = SqliteVssSearchRequest.builder()
                .queryText(query)
                .queryVector(queryVector)
                .topK(topK)
                .maxDistance(maxDistance)
                .collectionName(param.getCollectionName())
                .includeDistances(true)
                .includeVectors(false)
                .build();

        SqliteVssSearchResponse response = client.searchSimilarDocuments(searchRequest);
        return convertToDocuments(response.getResults());
    }

    /**
     * Search with metadata filters
     */
    public List<Document> searchWithMetadataFilter(String query, int topK, Map<String, Object> metadataFilter) {
        List<Double> queryVector = null;
        if (embeddings != null && StringUtils.isNotBlank(query)) {
            try {
                List<Document> embeddedQuery = embeddings.embedTexts(Collections.singletonList(query));
                if (!embeddedQuery.isEmpty() && CollectionUtils.isNotEmpty(embeddedQuery.get(0).getEmbedding())) {
                    queryVector = embeddedQuery.get(0).getEmbedding();
                }
            } catch (Exception e) {
                log.warn("Failed to generate embedding for query with metadata filter", e);
            }
        }

        SqliteVssSearchRequest searchRequest = SqliteVssSearchRequest.builder()
                .queryText(query)
                .queryVector(queryVector)
                .topK(topK)
                .metadataFilter(metadataFilter)
                .collectionName(param.getCollectionName())
                .includeDistances(true)
                .includeVectors(false)
                .build();

        SqliteVssSearchResponse response = client.searchSimilarDocuments(searchRequest);
        return convertToDocuments(response.getResults());
    }

    /**
     * Delete a document by ID
     */
    public boolean deleteDocument(String documentId) {
        if (StringUtils.isBlank(documentId)) {
            throw SqliteVssException.configError("Document ID cannot be null or empty");
        }

        try {
            boolean deleted = client.deleteDocument(param.getCollectionName(), documentId);
            if (deleted) {
                log.info("Successfully deleted document: {}", documentId);
            } else {
                log.warn("Document not found for deletion: {}", documentId);
            }
            return deleted;
        } catch (Exception e) {
            throw SqliteVssException.documentError("Failed to delete document: " + documentId, e);
        }
    }

    /**
     * Get total document count
     */
    public long getDocumentCount() {
        try {
            return client.getDocumentCount(param.getCollectionName());
        } catch (Exception e) {
            throw SqliteVssException.sqlError("Failed to get document count", e);
        }
    }

    /**
     * Close the service and underlying resources
     */
    public void close() {
        try {
            client.close();
            log.info("SQLite-VSS service closed successfully");
        } catch (Exception e) {
            log.error("Error closing SQLite-VSS service", e);
        }
    }

    private void validateDocuments(List<Document> documents) {
        for (Document document : documents) {
            if (StringUtils.isBlank(document.getPageContent())) {
                throw SqliteVssException.configError("Document content cannot be null or empty");
            }
            
            // Generate unique ID if not provided
            if (StringUtils.isBlank(document.getUniqueId())) {
                document.setUniqueId(UUID.randomUUID().toString());
            }

            // Initialize metadata if null
            if (document.getMetadata() == null) {
                document.setMetadata(new HashMap<>());
            }
        }
    }

    private List<SqliteVssInsertRequest> prepareInsertRequests(List<Document> documents) {
        // Generate embeddings for documents that don't have them
        List<Document> documentsWithEmbeddings = embedDocuments(documents);
        
        return documentsWithEmbeddings.stream()
                .map(this::convertToInsertRequest)
                .collect(Collectors.toList());
    }

    private List<Document> embedDocuments(List<Document> documents) {
        if (embeddings == null) {
            return documents;
        }

        List<Document> documentsNeedingEmbedding = documents.stream()
                .filter(doc -> CollectionUtils.isEmpty(doc.getEmbedding()))
                .collect(Collectors.toList());

        if (documentsNeedingEmbedding.isEmpty()) {
            return documents;
        }

        try {
            List<String> textsToEmbed = documentsNeedingEmbedding.stream()
                    .map(Document::getPageContent)
                    .collect(Collectors.toList());

            List<Document> embeddedDocs = embeddings.embedTexts(textsToEmbed);

            // Update original documents with embeddings
            for (int i = 0; i < documentsNeedingEmbedding.size(); i++) {
                Document originalDoc = documentsNeedingEmbedding.get(i);
                Document embeddedDoc = embeddedDocs.get(i);
                if (CollectionUtils.isNotEmpty(embeddedDoc.getEmbedding())) {
                    originalDoc.setEmbedding(embeddedDoc.getEmbedding());
                }
            }

            return documents;
        } catch (Exception e) {
            log.warn("Failed to generate embeddings for documents, proceeding without embeddings", e);
            return documents;
        }
    }

    private SqliteVssInsertRequest convertToInsertRequest(Document document) {
        return SqliteVssInsertRequest.builder()
                .id(document.getUniqueId())
                .content(document.getPageContent())
                .vector(document.getEmbedding())
                .metadata(document.getMetadata())
                .collectionName(param.getCollectionName())
                .upsert(true) // Allow updates by default
                .build();
    }

    private List<Document> convertToDocuments(List<SqliteVssSearchResponse.SearchResult> results) {
        return results.stream()
                .map(this::convertToDocument)
                .collect(Collectors.toList());
    }

    private Document convertToDocument(SqliteVssSearchResponse.SearchResult result) {
        Document document = new Document();
        document.setUniqueId(result.getId());
        document.setPageContent(result.getContent());
        document.setMetadata(result.getMetadata() != null ? result.getMetadata() : new HashMap<>());
        document.setScore(result.getScore());
        document.setEmbedding(result.getVector());
        return document;
    }
}
