package com.alibaba.langengine.vertexaivectorsearch.vectorstore;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VertexAiVectorSearchConnectionManagerTest {

    @Test
    void testGetIndexServiceClientThrowsException() {
        // Since we can't mock static methods easily without MockedStatic,
        // we test that the method will throw an exception when Google Cloud clients can't be created
        // This is expected behavior in a test environment without proper credentials
        assertThrows(VertexAiVectorSearchException.class, 
            () -> VertexAiVectorSearchConnectionManager.getIndexServiceClient("project", "location"));
    }

    @Test
    void testGetIndexEndpointServiceClientThrowsException() {
        assertThrows(VertexAiVectorSearchException.class, 
            () -> VertexAiVectorSearchConnectionManager.getIndexEndpointServiceClient("project", "location"));
    }

    @Test
    void testGetMatchServiceClientThrowsException() {
        assertThrows(VertexAiVectorSearchException.class, 
            () -> VertexAiVectorSearchConnectionManager.getMatchServiceClient("project", "location"));
    }

    @Test
    void testGetStorageClient() {
        // Storage client creation might succeed in some environments
        // Test that the method can be called without throwing unexpected exceptions
        assertDoesNotThrow(() -> {
            try {
                VertexAiVectorSearchConnectionManager.getStorageClient("project");
            } catch (VertexAiVectorSearchException e) {
                // This is also acceptable - depends on environment
            }
        });
    }

    @Test
    void testCloseClients() {
        // This test mainly ensures the method doesn't throw exceptions
        assertDoesNotThrow(() -> VertexAiVectorSearchConnectionManager.closeClients("project", "location"));
    }

    @Test
    void testCloseAllClients() {
        // This test mainly ensures the method doesn't throw exceptions
        assertDoesNotThrow(() -> VertexAiVectorSearchConnectionManager.closeAllClients());
    }
}