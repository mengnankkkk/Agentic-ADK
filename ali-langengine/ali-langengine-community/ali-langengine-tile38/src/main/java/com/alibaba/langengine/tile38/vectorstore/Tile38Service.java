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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
public class Tile38Service {

    private final Tile38Client client;
    private final String collectionName;
    private final Tile38BatchProcessor batchProcessor;
    private final int maxResultSize;

    public Tile38Service(Tile38Client client, String collectionName, int batchSize, int maxResultSize) {
        this.client = client;
        this.collectionName = collectionName;
        this.batchProcessor = new Tile38BatchProcessor(client, collectionName, batchSize);
        this.maxResultSize = maxResultSize;
    }

    /**
     * Add documents to Tile38
     */
    public void addDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }

        for (Document document : documents) {
            try {
                if (StringUtils.isEmpty(document.getUniqueId())) {
                    document.setUniqueId(UUID.randomUUID().toString());
                }

                Map<String, String> fields = new HashMap<>();
                fields.put("content", document.getPageContent());
                
                if (document.getMetadata() != null) {
                    document.getMetadata().forEach((key, value) -> 
                        fields.put(key, value != null ? value.toString() : ""));
                }

                // Convert embedding to coordinates (simplified approach)
                double lat = 0.0, lon = 0.0;
                if (CollectionUtils.isNotEmpty(document.getEmbedding())) {
                    List<Double> embedding = document.getEmbedding();
                    // Use first two dimensions as lat/lon for demonstration
                    if (embedding.size() >= 2) {
                        lat = embedding.get(0) * 90.0; // Scale to lat range
                        lon = embedding.get(1) * 180.0; // Scale to lon range
                    }
                }

                client.set(collectionName, document.getUniqueId(), lat, lon, fields);
                log.debug("Added document {} to collection {}", document.getUniqueId(), collectionName);
            } catch (Exception e) {
                log.error("Failed to add document {} to collection {}", 
                    document.getUniqueId(), collectionName, e);
                throw new Tile38Exception("ADD_DOCUMENT_ERROR", 
                    "Failed to add document: " + document.getUniqueId(), e);
            }
        }
    }

    /**
     * Search for similar documents
     */
    public List<Document> similaritySearch(String query, int k, Double maxDistance) {
        try {
            // For demonstration, use a simple coordinate-based search
            // In a real implementation, you would convert the query to coordinates
            double queryLat = 0.0;
            double queryLon = 0.0;
            double radius = maxDistance != null ? maxDistance : 1000.0;

            List<Object> results = client.within(collectionName, queryLat, queryLon, radius, k);
            return parseSearchResults(results);
        } catch (Exception e) {
            log.error("Failed to perform similarity search in collection {}", collectionName, e);
            throw new Tile38Exception("SEARCH_ERROR", 
                "Failed to perform similarity search", e);
        }
    }

    /**
     * Search nearby documents
     */
    public List<Document> nearbySearch(double lat, double lon, int k) {
        try {
            List<Object> results = client.nearby(collectionName, lat, lon, k);
            return parseSearchResults(results);
        } catch (Exception e) {
            log.error("Failed to perform nearby search in collection {}", collectionName, e);
            throw new Tile38Exception("NEARBY_SEARCH_ERROR", 
                "Failed to perform nearby search", e);
        }
    }

    /**
     * Delete document by id
     */
    public void deleteDocument(String id) {
        try {
            client.del(collectionName, id);
            log.debug("Deleted document {} from collection {}", id, collectionName);
        } catch (Exception e) {
            log.error("Failed to delete document {} from collection {}", id, collectionName, e);
            throw new Tile38Exception("DELETE_DOCUMENT_ERROR", 
                "Failed to delete document: " + id, e);
        }
    }

    /**
     * Drop the entire collection
     */
    public void dropCollection() {
        try {
            client.drop(collectionName);
            log.info("Dropped collection {}", collectionName);
        } catch (Exception e) {
            log.error("Failed to drop collection {}", collectionName, e);
            throw new Tile38Exception("DROP_COLLECTION_ERROR", 
                "Failed to drop collection: " + collectionName, e);
        }
    }

    /**
     * Batch add documents using parallel processing
     */
    public void batchAddDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }
        
        try {
            batchProcessor.batchAddDocuments(documents);
            log.info("Batch added {} documents to collection {}", documents.size(), collectionName);
        } catch (Exception e) {
            log.error("Failed to batch add documents to collection {}", collectionName, e);
            throw new Tile38Exception("BATCH_ADD_ERROR", 
                "Failed to batch add documents", e);
        }
    }

    /**
     * Batch delete documents
     */
    public void batchDeleteDocuments(List<String> documentIds) {
        if (CollectionUtils.isEmpty(documentIds)) {
            return;
        }
        
        try {
            batchProcessor.batchDeleteDocuments(documentIds);
            log.info("Batch deleted {} documents from collection {}", documentIds.size(), collectionName);
        } catch (Exception e) {
            log.error("Failed to batch delete documents from collection {}", collectionName, e);
            throw new Tile38Exception("BATCH_DELETE_ERROR", 
                "Failed to batch delete documents", e);
        }
    }

    /**
     * Shutdown the service and cleanup resources
     */
    public void shutdown() {
        if (batchProcessor != null) {
            batchProcessor.shutdown();
        }
    }

    /**
     * Parse search results from Tile38 response
     */
    private List<Document> parseSearchResults(List<Object> results) {
        if (CollectionUtils.isEmpty(results)) {
            return new ArrayList<>();
        }

        List<Document> documents = new ArrayList<>();
        
        // Tile38 returns results in a specific format
        // This is a simplified parsing - adjust based on actual Tile38 response format
        for (Object result : results) {
            try {
                if (result instanceof List) {
                    List<?> resultList = (List<?>) result;
                    if (resultList.size() >= 2) {
                        String id = resultList.get(0).toString();
                        Object data = resultList.get(1);
                        
                        Document document = new Document();
                        document.setUniqueId(id);
                        
                        if (data instanceof Map) {
                            Map<?, ?> dataMap = (Map<?, ?>) data;
                            Object content = dataMap.get("content");
                            if (content != null) {
                                document.setPageContent(content.toString());
                            }
                            
                            Map<String, Object> metadata = new HashMap<>();
                            dataMap.forEach((key, value) -> {
                                if (!"content".equals(key) && value != null) {
                                    metadata.put(key.toString(), value);
                                }
                            });
                            document.setMetadata(metadata);
                        }
                        
                        documents.add(document);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse search result: {}", result, e);
            }
        }
        
        return documents;
    }

}