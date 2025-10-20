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
package com.alibaba.langengine.vertexaivectorsearch;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class VertexAiVectorSearchConfiguration {

    /**
     * Google Cloud Project ID
     */
    public static final String VERTEX_AI_PROJECT_ID = System.getProperty("vertex.ai.project.id", 
        System.getenv("VERTEX_AI_PROJECT_ID"));

    /**
     * Google Cloud Project Location/Region
     */
    public static final String VERTEX_AI_LOCATION = getConfigValue("vertex.ai.location", 
        "VERTEX_AI_LOCATION", "us-central1");

    /**
     * Google Cloud Service Account Key Path
     */
    public static final String VERTEX_AI_CREDENTIALS_PATH = System.getProperty("vertex.ai.credentials.path",
        System.getenv("VERTEX_AI_CREDENTIALS_PATH"));

    /**
     * Default Index Display Name
     */
    public static final String DEFAULT_INDEX_DISPLAY_NAME = getConfigValue("vertex.ai.index.display.name",
        "VERTEX_AI_INDEX_DISPLAY_NAME", "vector_search_index");

    /**
     * Default Endpoint Display Name
     */
    public static final String DEFAULT_ENDPOINT_DISPLAY_NAME = getConfigValue("vertex.ai.endpoint.display.name",
        "VERTEX_AI_ENDPOINT_DISPLAY_NAME", "vector_search_endpoint");

    private static String getConfigValue(String systemProperty, String envVar, String defaultValue) {
        String value = System.getProperty(systemProperty);
        if (value == null) {
            value = System.getenv(envVar);
        }
        return value != null ? value : defaultValue;
    }

    static {
        log.info("VertexAiVectorSearchConfiguration initialized");
        log.debug("VERTEX_AI_PROJECT_ID: {}", VERTEX_AI_PROJECT_ID != null ? "configured" : "not configured");
        log.debug("VERTEX_AI_LOCATION: {}", VERTEX_AI_LOCATION != null ? "configured" : "not configured");
        log.debug("VERTEX_AI_CREDENTIALS_PATH: {}", VERTEX_AI_CREDENTIALS_PATH != null ? "configured" : "not configured");
    }
}
