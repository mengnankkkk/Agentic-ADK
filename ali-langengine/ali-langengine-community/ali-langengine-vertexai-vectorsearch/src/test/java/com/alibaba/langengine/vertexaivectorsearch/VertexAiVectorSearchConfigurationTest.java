package com.alibaba.langengine.vertexaivectorsearch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VertexAiVectorSearchConfigurationTest {

    @Test
    void testConfigurationConstants() {
        // Test that constants are accessible
        assertNotNull(VertexAiVectorSearchConfiguration.class);
        
        // Test that the class can be instantiated (though it's mainly for static access)
        assertDoesNotThrow(() -> new VertexAiVectorSearchConfiguration());
    }

    @Test
    void testSystemPropertyAccess() {
        // Test that configuration constants are accessible
        // Note: The constants are initialized at class loading time, so setting properties
        // after class loading won't affect them. This test verifies the constants exist.
        // VERTEX_AI_PROJECT_ID can be null if not configured
        // VERTEX_AI_LOCATION should have default value
        assertNotNull(VertexAiVectorSearchConfiguration.VERTEX_AI_LOCATION);
        
        // Test that we can access all configuration constants without exceptions
        assertDoesNotThrow(() -> {
            String projectId = VertexAiVectorSearchConfiguration.VERTEX_AI_PROJECT_ID;
            String location = VertexAiVectorSearchConfiguration.VERTEX_AI_LOCATION;
            String credentialsPath = VertexAiVectorSearchConfiguration.VERTEX_AI_CREDENTIALS_PATH;
            String indexName = VertexAiVectorSearchConfiguration.DEFAULT_INDEX_DISPLAY_NAME;
            String endpointName = VertexAiVectorSearchConfiguration.DEFAULT_ENDPOINT_DISPLAY_NAME;
        });
    }

    @Test
    void testEnvironmentVariableFallback() {
        // When system properties are not set, it should fall back to environment variables
        // This test mainly ensures the configuration class doesn't throw exceptions
        assertDoesNotThrow(() -> {
            String projectId = VertexAiVectorSearchConfiguration.VERTEX_AI_PROJECT_ID;
            String location = VertexAiVectorSearchConfiguration.VERTEX_AI_LOCATION;
            String credentialsPath = VertexAiVectorSearchConfiguration.VERTEX_AI_CREDENTIALS_PATH;
            String indexName = VertexAiVectorSearchConfiguration.DEFAULT_INDEX_DISPLAY_NAME;
            String endpointName = VertexAiVectorSearchConfiguration.DEFAULT_ENDPOINT_DISPLAY_NAME;
        });
    }
}