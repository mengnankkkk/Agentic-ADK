package com.alibaba.langengine.vertexaivectorsearch.vectorstore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VertexAiVectorSearchExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String message = "Test error message";
        VertexAiVectorSearchException exception = new VertexAiVectorSearchException(message);
        
        assertEquals(message, exception.getMessage());
        assertEquals("VERTEX_AI_VECTOR_SEARCH_ERROR", exception.getErrorCode());
    }

    @Test
    void testConstructorWithErrorCodeAndMessage() {
        String errorCode = "CUSTOM_ERROR";
        String message = "Test error message";
        VertexAiVectorSearchException exception = new VertexAiVectorSearchException(errorCode, message);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");
        VertexAiVectorSearchException exception = new VertexAiVectorSearchException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals("VERTEX_AI_VECTOR_SEARCH_ERROR", exception.getErrorCode());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testConstructorWithErrorCodeMessageAndCause() {
        String errorCode = "CUSTOM_ERROR";
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");
        VertexAiVectorSearchException exception = new VertexAiVectorSearchException(errorCode, message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testToString() {
        String errorCode = "TEST_ERROR";
        String message = "Test message";
        VertexAiVectorSearchException exception = new VertexAiVectorSearchException(errorCode, message);
        
        String expected = "VertexAiVectorSearchException[TEST_ERROR]: Test message";
        assertEquals(expected, exception.toString());
    }
}