package com.alibaba.langengine.vectra.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VectraExceptionTest {

    @Test
    void testBasicException() {
        VectraException exception = new VectraException("Test message");
        
        assertEquals("Test message", exception.getMessage());
        assertEquals("VECTRA_ERROR", exception.getErrorCode());
        assertEquals(VectraException.ErrorSeverity.MEDIUM, exception.getSeverity());
        assertNotNull(exception.getTimestamp());
    }

    @Test
    void testExceptionWithCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        VectraException exception = new VectraException("Test message", cause);
        
        assertEquals("Test message", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testExceptionWithErrorCode() {
        VectraException exception = new VectraException("CLIENT_FAILED", "Client initialization failed");
        
        assertEquals("CLIENT_FAILED", exception.getErrorCode());
        assertEquals("CLIENT_OPERATION", exception.getOperation());
        assertEquals(VectraException.ErrorSeverity.CRITICAL, exception.getSeverity());
    }

    @Test
    void testSeverityDetermination() {
        VectraException critical = new VectraException("CRITICAL_ERROR", "Critical failure");
        VectraException high = new VectraException("VECTOR_ERROR", "Vector operation failed");
        VectraException medium = new VectraException("WARNING_CODE", "Warning message");
        VectraException low = new VectraException("INFO_CODE", "Info message");
        
        assertEquals(VectraException.ErrorSeverity.CRITICAL, critical.getSeverity());
        assertEquals(VectraException.ErrorSeverity.HIGH, high.getSeverity());
        assertEquals(VectraException.ErrorSeverity.MEDIUM, medium.getSeverity());
        assertEquals(VectraException.ErrorSeverity.LOW, low.getSeverity());
    }

    @Test
    void testOperationExtraction() {
        VectraException clientEx = new VectraException("CLIENT_ERROR", "Client error");
        VectraException collectionEx = new VectraException("COLLECTION_ERROR", "Collection error");
        VectraException vectorEx = new VectraException("VECTOR_ERROR", "Vector error");
        VectraException searchEx = new VectraException("SEARCH_ERROR", "Search error");
        
        assertEquals("CLIENT_OPERATION", clientEx.getOperation());
        assertEquals("COLLECTION_OPERATION", collectionEx.getOperation());
        assertEquals("VECTOR_OPERATION", vectorEx.getOperation());
        assertEquals("SEARCH_OPERATION", searchEx.getOperation());
    }

    @Test
    void testToString() {
        VectraException exception = new VectraException("TEST_ERROR", "Test message");
        String toString = exception.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("VectraException"));
        assertTrue(toString.contains("TEST_ERROR"));
    }
}