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

import com.google.cloud.aiplatform.v1.IndexEndpointServiceClient;
import com.google.cloud.aiplatform.v1.IndexServiceClient;
import com.google.cloud.aiplatform.v1.MatchServiceClient;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class VertexAiVectorSearchConnectionManager {

    private static final ConcurrentHashMap<String, IndexServiceClient> indexServiceClients = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, IndexEndpointServiceClient> endpointServiceClients = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, MatchServiceClient> matchServiceClients = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Storage> storageClients = new ConcurrentHashMap<>();

    /**
     * Get IndexServiceClient for managing indexes
     *
     * @param projectId Google Cloud Project ID
     * @param location  Google Cloud Location
     * @return IndexServiceClient instance
     */
    public static IndexServiceClient getIndexServiceClient(String projectId, String location) {
        String cacheKey = projectId + ":" + location;
        return indexServiceClients.computeIfAbsent(cacheKey, key -> {
            try {
                IndexServiceClient client = IndexServiceClient.create();
                log.info("Created IndexServiceClient for project: {}, location: {}", projectId, location);
                return client;
            } catch (IOException e) {
                throw new VertexAiVectorSearchException("INDEX_SERVICE_CLIENT_CREATION_FAILED", 
                    "Failed to create IndexServiceClient", e);
            }
        });
    }

    /**
     * Get IndexEndpointServiceClient for managing index endpoints
     *
     * @param projectId Google Cloud Project ID
     * @param location  Google Cloud Location
     * @return IndexEndpointServiceClient instance
     */
    public static IndexEndpointServiceClient getIndexEndpointServiceClient(String projectId, String location) {
        String cacheKey = projectId + ":" + location;
        return endpointServiceClients.computeIfAbsent(cacheKey, key -> {
            try {
                IndexEndpointServiceClient client = IndexEndpointServiceClient.create();
                log.info("Created IndexEndpointServiceClient for project: {}, location: {}", projectId, location);
                return client;
            } catch (IOException e) {
                throw new VertexAiVectorSearchException("INDEX_ENDPOINT_SERVICE_CLIENT_CREATION_FAILED", 
                    "Failed to create IndexEndpointServiceClient", e);
            }
        });
    }

    /**
     * Get MatchServiceClient for performing vector searches
     *
     * @param projectId Google Cloud Project ID
     * @param location  Google Cloud Location
     * @return MatchServiceClient instance
     */
    public static MatchServiceClient getMatchServiceClient(String projectId, String location) {
        String cacheKey = projectId + ":" + location;
        return matchServiceClients.computeIfAbsent(cacheKey, key -> {
            try {
                MatchServiceClient client = MatchServiceClient.create();
                log.info("Created MatchServiceClient for project: {}, location: {}", projectId, location);
                return client;
            } catch (IOException e) {
                throw new VertexAiVectorSearchException("MATCH_SERVICE_CLIENT_CREATION_FAILED", 
                    "Failed to create MatchServiceClient", e);
            }
        });
    }

    /**
     * Get Storage client for Cloud Storage operations
     *
     * @param projectId Google Cloud Project ID
     * @return Storage instance
     */
    public static Storage getStorageClient(String projectId) {
        return storageClients.computeIfAbsent(projectId, key -> {
            try {
                Storage storage = StorageOptions.newBuilder()
                    .setProjectId(projectId)
                    .build()
                    .getService();
                log.info("Created Storage client for project: {}", projectId);
                return storage;
            } catch (Exception e) {
                throw new VertexAiVectorSearchException("STORAGE_CLIENT_CREATION_FAILED", 
                    "Failed to create Storage client", e);
            }
        });
    }

    /**
     * Close all clients for a specific project and location
     *
     * @param projectId Google Cloud Project ID
     * @param location  Google Cloud Location
     */
    public static void closeClients(String projectId, String location) {
        String cacheKey = projectId + ":" + location;
        
        IndexServiceClient indexClient = indexServiceClients.remove(cacheKey);
        if (indexClient != null) {
            indexClient.close();
            log.info("Closed IndexServiceClient for project: {}, location: {}", projectId, location);
        }

        IndexEndpointServiceClient endpointClient = endpointServiceClients.remove(cacheKey);
        if (endpointClient != null) {
            endpointClient.close();
            log.info("Closed IndexEndpointServiceClient for project: {}, location: {}", projectId, location);
        }

        MatchServiceClient matchClient = matchServiceClients.remove(cacheKey);
        if (matchClient != null) {
            matchClient.close();
            log.info("Closed MatchServiceClient for project: {}, location: {}", projectId, location);
        }

        // Storage client doesn't need explicit closing in this context
        storageClients.remove(projectId);
    }

    /**
     * Close all clients
     */
    public static void closeAllClients() {
        indexServiceClients.values().forEach(IndexServiceClient::close);
        indexServiceClients.clear();

        endpointServiceClients.values().forEach(IndexEndpointServiceClient::close);
        endpointServiceClients.clear();

        matchServiceClients.values().forEach(MatchServiceClient::close);
        matchServiceClients.clear();

        storageClients.clear();

        log.info("Closed all VertexAI Vector Search clients");
    }

}
