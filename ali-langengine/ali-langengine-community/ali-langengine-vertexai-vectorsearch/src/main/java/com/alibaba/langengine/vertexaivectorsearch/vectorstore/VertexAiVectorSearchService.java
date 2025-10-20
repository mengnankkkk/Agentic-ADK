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

import com.alibaba.langengine.core.embeddings.Embeddings;
import com.alibaba.langengine.core.indexes.Document;
import com.google.cloud.aiplatform.v1.*;
import com.google.cloud.storage.Storage;
import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;


@Slf4j
@Data
public class VertexAiVectorSearchService {

    private final String projectId;
    private final String location;
    private final String indexDisplayName;
    private final String indexEndpointDisplayName;
    private VertexAiVectorSearchParam param;
    
    private volatile IndexServiceClient indexServiceClient;
    private volatile IndexEndpointServiceClient indexEndpointServiceClient;
    private volatile MatchServiceClient matchServiceClient;
    private volatile Storage storageClient;
    
    private volatile String indexName;
    private volatile String indexEndpointName;
    private volatile String deployedIndexId;
    
    private final ReentrantReadWriteLock serviceLock = new ReentrantReadWriteLock();
    private volatile boolean initialized = false;

    public VertexAiVectorSearchService(String projectId, String location, String indexDisplayName, 
                                     String indexEndpointDisplayName, VertexAiVectorSearchParam param) {
        validateInputs(projectId, location, indexDisplayName, indexEndpointDisplayName);
        
        this.projectId = projectId;
        this.location = location;
        this.indexDisplayName = indexDisplayName;
        this.indexEndpointDisplayName = indexEndpointDisplayName;
        this.param = param != null ? param : new VertexAiVectorSearchParam();
        
        initializeClients();
    }

    private void validateInputs(String projectId, String location, String indexDisplayName, String indexEndpointDisplayName) {
        if (StringUtils.isBlank(projectId)) {
            throw new VertexAiVectorSearchException("INVALID_CONFIG", "Project ID cannot be null or empty");
        }
        if (StringUtils.isBlank(location)) {
            throw new VertexAiVectorSearchException("INVALID_CONFIG", "Location cannot be null or empty");
        }
        if (StringUtils.isBlank(indexDisplayName)) {
            throw new VertexAiVectorSearchException("INVALID_CONFIG", "Index display name cannot be null or empty");
        }
        if (StringUtils.isBlank(indexEndpointDisplayName)) {
            throw new VertexAiVectorSearchException("INVALID_CONFIG", "Index endpoint display name cannot be null or empty");
        }
    }

    private void initializeClients() {
        serviceLock.writeLock().lock();
        try {
            if (initialized) {
                return;
            }
            
            this.indexServiceClient = VertexAiVectorSearchConnectionManager.getIndexServiceClient(projectId, location);
            this.indexEndpointServiceClient = VertexAiVectorSearchConnectionManager.getIndexEndpointServiceClient(projectId, location);
            this.matchServiceClient = VertexAiVectorSearchConnectionManager.getMatchServiceClient(projectId, location);
            this.storageClient = VertexAiVectorSearchConnectionManager.getStorageClient(projectId);
            
            this.initialized = true;
            
            log.info("VertexAiVectorSearchService initialized: project={}, location={}, index={}, endpoint={}", 
                projectId, location, indexDisplayName, indexEndpointDisplayName);
        } catch (Exception e) {
            throw new VertexAiVectorSearchException("CLIENT_INITIALIZATION_FAILED", 
                "Failed to initialize Vertex AI clients", e);
        } finally {
            serviceLock.writeLock().unlock();
        }
    }

    private void ensureInitialized() {
        if (!initialized) {
            initializeClients();
        }
    }

    /**
     * Initialize Vertex AI Vector Search index and endpoint
     */
    public void init(Embeddings embeddings) {
        ensureInitialized();
        
        serviceLock.writeLock().lock();
        try {
            // Create or get index
            this.indexName = createOrGetIndex(embeddings);
            
            // Create or get index endpoint
            this.indexEndpointName = createOrGetIndexEndpoint();
            
            // Deploy index to endpoint
            this.deployedIndexId = deployIndexToEndpoint();
            
            log.info("VertexAI Vector Search initialization completed: indexName={}, endpointName={}, deployedIndexId={}", 
                indexName, indexEndpointName, deployedIndexId);
        } catch (Exception e) {
            throw new VertexAiVectorSearchException("INITIALIZATION_FAILED", 
                "Failed to initialize Vertex AI Vector Search", e);
        } finally {
            serviceLock.writeLock().unlock();
        }
    }

    /**
     * Add documents to the vector search index
     */
    public void addDocuments(List<Document> documents) {
        ensureInitialized();
        
        if (documents == null || documents.isEmpty()) {
            log.warn("No documents to add");
            return;
        }

        try {
            List<IndexDatapoint> datapoints = convertDocumentsToDatapoints(documents);
            
            // Create upsert request
            UpsertDatapointsRequest request = UpsertDatapointsRequest.newBuilder()
                .setIndex(indexName)
                .addAllDatapoints(datapoints)
                .build();
            
            // Execute upsert operation
            UpsertDatapointsResponse response = indexServiceClient.upsertDatapoints(request);
            
            log.info("Successfully added {} documents to index", documents.size());
        } catch (Exception e) {
            throw new VertexAiVectorSearchException("ADD_DOCUMENTS_FAILED", 
                "Failed to add documents to index", e);
        }
    }

    /**
     * Perform similarity search
     */
    public List<Document> similaritySearch(String query, List<Double> queryEmbedding, int k) {
        ensureInitialized();
        
        try {
            // Create find neighbors request
            FindNeighborsRequest.Query findNeighborsQuery = FindNeighborsRequest.Query.newBuilder()
                .setDatapoint(IndexDatapoint.newBuilder()
                    .setDatapointId(UUID.randomUUID().toString())
                    .addAllFeatureVector(queryEmbedding.stream().map(Double::floatValue).toList())
                    .build())
                .setNeighborCount(k)
                .build();

            FindNeighborsRequest request = FindNeighborsRequest.newBuilder()
                .setIndexEndpoint(indexEndpointName)
                .setDeployedIndexId(deployedIndexId)
                .addQueries(findNeighborsQuery)
                .build();
            
            // Execute search
            FindNeighborsResponse response = matchServiceClient.findNeighbors(request);
            
            return convertNeighborsToDocuments(response);
        } catch (Exception e) {
            throw new VertexAiVectorSearchException("SIMILARITY_SEARCH_FAILED", 
                "Failed to perform similarity search", e);
        }
    }

    /**
     * Delete documents by IDs
     */
    public void deleteDocuments(List<String> documentIds) {
        ensureInitialized();
        
        if (documentIds == null || documentIds.isEmpty()) {
            log.warn("No document IDs to delete");
            return;
        }

        try {
            RemoveDatapointsRequest request = RemoveDatapointsRequest.newBuilder()
                .setIndex(indexName)
                .addAllDatapointIds(documentIds)
                .build();
            
            RemoveDatapointsResponse response = indexServiceClient.removeDatapoints(request);
            
            log.info("Successfully deleted {} documents from index", documentIds.size());
        } catch (Exception e) {
            throw new VertexAiVectorSearchException("DELETE_DOCUMENTS_FAILED", 
                "Failed to delete documents from index", e);
        }
    }

    private String createOrGetIndex(Embeddings embeddings) {
        // Implementation for creating or getting existing index
        // This would involve checking if index exists and creating if not
        return String.format("projects/%s/locations/%s/indexes/%s", projectId, location, "generated-index-id");
    }

    private String createOrGetIndexEndpoint() {
        // Implementation for creating or getting existing index endpoint
        return String.format("projects/%s/locations/%s/indexEndpoints/%s", projectId, location, "generated-endpoint-id");
    }

    private String deployIndexToEndpoint() {
        // Implementation for deploying index to endpoint
        return "deployed-index-id";
    }

    private List<IndexDatapoint> convertDocumentsToDatapoints(List<Document> documents) {
        List<IndexDatapoint> datapoints = new ArrayList<>();
        
        for (Document doc : documents) {
            IndexDatapoint.Builder datapointBuilder = IndexDatapoint.newBuilder()
                .setDatapointId(doc.getUniqueId() != null ? doc.getUniqueId() : UUID.randomUUID().toString());
            
            // Add embedding vector
            if (doc.getEmbedding() != null && !doc.getEmbedding().isEmpty()) {
                datapointBuilder.addAllFeatureVector(doc.getEmbedding().stream().map(Double::floatValue).toList());
            }
            
            // Add metadata
            Struct.Builder structBuilder = Struct.newBuilder();
            structBuilder.putFields(param.getFieldNamePageContent(), 
                Value.newBuilder().setStringValue(doc.getPageContent()).build());
            
            if (doc.getMetadata() != null) {
                for (Map.Entry<String, Object> entry : doc.getMetadata().entrySet()) {
                    structBuilder.putFields(entry.getKey(), 
                        Value.newBuilder().setStringValue(entry.getValue().toString()).build());
                }
            }
            
            datapointBuilder.addRestricts(
                IndexDatapoint.Restriction.newBuilder()
                    .setNamespace("default")
                    .build());
            
            datapoints.add(datapointBuilder.build());
        }
        
        return datapoints;
    }

    private List<Document> convertNeighborsToDocuments(FindNeighborsResponse response) {
        List<Document> documents = new ArrayList<>();
        
        for (FindNeighborsResponse.NearestNeighbors neighbors : response.getNearestNeighborsList()) {
            for (FindNeighborsResponse.Neighbor neighbor : neighbors.getNeighborsList()) {
                Document doc = new Document();
                doc.setUniqueId(neighbor.getDatapoint().getDatapointId());
                
                // Extract content and metadata from datapoint
                // This would need to be implemented based on how data was stored
                
                documents.add(doc);
            }
        }
        
        return documents;
    }

    /**
     * Close the service and clean up resources
     */
    public void close() {
        VertexAiVectorSearchConnectionManager.closeClients(projectId, location);
        log.info("VertexAiVectorSearchService closed");
    }

}
