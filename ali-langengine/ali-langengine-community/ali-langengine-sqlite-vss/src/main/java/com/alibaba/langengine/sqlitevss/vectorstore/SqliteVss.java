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
package com.alibaba.langengine.sqlitevss.vectorstore;

import com.alibaba.langengine.core.embeddings.Embeddings;
import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.core.vectorstore.VectorStore;
import com.alibaba.langengine.sqlitevss.SqliteVssConfiguration;
import com.alibaba.langengine.sqlitevss.vectorstore.service.SqliteVssClient;
import com.alibaba.langengine.sqlitevss.vectorstore.service.SqliteVssService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class SqliteVss extends VectorStore implements Closeable {

    /**
     * Embeddings provider for generating vector representations
     */
    private Embeddings embedding;

    /**
     * Collection name (table name) for storing documents
     */
    private String collectionName;

    /**
     * Configuration parameters for the SQLite-VSS vector store
     */
    private SqliteVssParam param;

    /**
     * Internal client for database operations
     */
    private SqliteVssClient client;

    /**
     * Internal service layer for business logic
     */
    private SqliteVssService service;

    /**
     * Constructor with default configuration
     *
     * @param embedding      embeddings provider
     * @param collectionName collection name for documents
     */
    public SqliteVss(Embeddings embedding, String collectionName) {
        this(createDefaultParam(collectionName), embedding);
    }

    /**
     * Constructor with custom database path
     *
     * @param dbPath         database file path
     * @param embedding      embeddings provider
     * @param collectionName collection name for documents
     */
    public SqliteVss(String dbPath, Embeddings embedding, String collectionName) {
        this(SqliteVssParam.withDbPathAndCollection(dbPath, collectionName), embedding);
    }

    /**
     * Constructor with full parameter configuration
     *
     * @param param     vector store parameters
     * @param embedding embeddings provider
     */
    public SqliteVss(SqliteVssParam param, Embeddings embedding) {
        if (param == null) {
            throw SqliteVssException.configError("SqliteVssParam cannot be null");
        }
        if (embedding == null) {
            throw SqliteVssException.configError("Embeddings cannot be null");
        }

        // 验证参数有效性
        param.validate();

        this.param = param;
        this.embedding = embedding;
        this.collectionName = param.getCollectionName();

        initializeComponents();
        log.info("SQLite-VSS vector store initialized with collection: {}", collectionName);
    }

    private static SqliteVssParam createDefaultParam(String collectionName) {
        return SqliteVssParam.builder()
                .dbPath(SqliteVssConfiguration.getDbPath())
                .collectionName(StringUtils.isNotBlank(collectionName) ? collectionName : "documents")
                .maxPoolSize(SqliteVssConfiguration.getMaxPoolSize())
                .connectionTimeoutSeconds(SqliteVssConfiguration.getConnectionTimeout())
                .maxIdleTimeSeconds(SqliteVssConfiguration.getMaxIdleTime())
                .build();
    }

    private void initializeComponents() {
        try {
            this.client = new SqliteVssClient(param);
            this.service = new SqliteVssService(client, param, embedding);
        } catch (Exception e) {
            throw SqliteVssException.connectionError("Failed to initialize SQLite-VSS components", e);
        }
    }

    /**
     * Add documents to the vector store
     *
     * @param documents list of documents to add
     */
    @Override
    public void addDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            log.warn("No documents provided for addition");
            return;
        }

        try {
            // Validate and prepare documents
            for (Document document : documents) {
                if (StringUtils.isEmpty(document.getUniqueId())) {
                    document.setUniqueId(UUID.randomUUID().toString());
                }
                if (StringUtils.isEmpty(document.getPageContent())) {
                    throw SqliteVssException.configError("Document content cannot be empty");
                }
                if (MapUtils.isEmpty(document.getMetadata())) {
                    document.setMetadata(new java.util.HashMap<>());
                }
            }

            service.addDocuments(documents);
            log.info("Successfully added {} documents to collection '{}'", documents.size(), collectionName);
        } catch (Exception e) {
            throw SqliteVssException.documentError("Failed to add documents", e);
        }
    }

    /**
     * Search for similar documents
     *
     * @param query            query text
     * @param k                number of results to return
     * @param maxDistanceValue maximum distance threshold
     * @param type             search type (reserved for future use)
     * @return list of similar documents
     */
    @Override
    public List<Document> similaritySearch(String query, int k, Double maxDistanceValue, Integer type) {
        if (StringUtils.isBlank(query)) {
            throw SqliteVssException.configError("Query cannot be null or empty");
        }
        if (k <= 0) {
            throw SqliteVssException.configError("Number of results (k) must be positive");
        }

        try {
            List<Document> results = service.searchSimilarDocuments(query, k, maxDistanceValue);
            log.debug("Found {} similar documents for query in collection '{}'", results.size(), collectionName);
            return results;
        } catch (Exception e) {
            throw SqliteVssException.searchError("Failed to search similar documents", e);
        }
    }

    /**
     * Search with metadata filters
     *
     * @param query          query text
     * @param k              number of results to return
     * @param metadataFilter metadata filters to apply
     * @return list of matching documents
     */
    public List<Document> similaritySearchWithMetadata(String query, int k, Map<String, Object> metadataFilter) {
        if (StringUtils.isBlank(query)) {
            throw SqliteVssException.configError("Query cannot be null or empty");
        }
        if (k <= 0) {
            throw SqliteVssException.configError("Number of results (k) must be positive");
        }

        try {
            List<Document> results = service.searchWithMetadataFilter(query, k, metadataFilter);
            log.debug("Found {} documents with metadata filter in collection '{}'", results.size(), collectionName);
            return results;
        } catch (Exception e) {
            throw SqliteVssException.searchError("Failed to search with metadata filter", e);
        }
    }

    /**
     * Delete a document by ID
     *
     * @param documentId unique document identifier
     * @return true if document was deleted, false if not found
     */
    public boolean deleteDocument(String documentId) {
        if (StringUtils.isBlank(documentId)) {
            throw SqliteVssException.configError("Document ID cannot be null or empty");
        }

        try {
            boolean deleted = service.deleteDocument(documentId);
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
     * Get the total number of documents in the collection
     *
     * @return document count
     */
    public long getDocumentCount() {
        try {
            return service.getDocumentCount();
        } catch (Exception e) {
            throw SqliteVssException.sqlError("Failed to get document count", e);
        }
    }

    /**
     * Get the collection name
     *
     * @return collection name
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * Get the database path
     *
     * @return database file path
     */
    public String getDbPath() {
        return param.getDbPath();
    }

    /**
     * Get vector dimension
     *
     * @return vector dimension
     */
    public int getVectorDimension() {
        return param.getVectorDimension();
    }

    /**
     * Get distance metric
     *
     * @return distance metric
     */
    public String getDistanceMetric() {
        return param.getDistanceMetric();
    }

    /**
     * Close the vector store and release resources
     */
    @Override
    public void close() {
        try {
            if (service != null) {
                service.close();
            }
            log.info("SQLite-VSS vector store closed successfully");
        } catch (Exception e) {
            log.error("Error closing SQLite-VSS vector store", e);
        }
    }

    /**
     * Create a new collection (table)
     *
     * @param collectionName name of the collection to create
     */
    public void createCollection(String collectionName) {
        if (StringUtils.isBlank(collectionName)) {
            throw SqliteVssException.configError("Collection name cannot be null or empty");
        }

        try {
            client.createCollectionIfNotExists(collectionName);
            log.info("Collection '{}' created successfully", collectionName);
        } catch (Exception e) {
            throw SqliteVssException.sqlError("Failed to create collection: " + collectionName, e);
        }
    }
}
