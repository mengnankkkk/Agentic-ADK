package com.alibaba.langengine.docloader.feishu.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeishuDocLoaderExceptionTest {

    @Test
    void testMessageOnlyConstructor() {
        FeishuDocLoaderException exception = new FeishuDocLoaderException("Test message");
        assertEquals("Test message", exception.getMessage());
        assertEquals("UNKNOWN", exception.getErrorCode());
    }

    @Test
    void testMessageAndCauseConstructor() {
        RuntimeException cause = new RuntimeException("Cause");
        FeishuDocLoaderException exception = new FeishuDocLoaderException("Test message", cause);
        assertEquals("Test message", exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals("UNKNOWN", exception.getErrorCode());
    }

    @Test
    void testErrorCodeAndMessageConstructor() {
        FeishuDocLoaderException exception = new FeishuDocLoaderException("API_ERROR", "API failed");
        assertEquals("API failed", exception.getMessage());
        assertEquals("API_ERROR", exception.getErrorCode());
    }

    @Test
    void testFullConstructor() {
        RuntimeException cause = new RuntimeException("Cause");
        FeishuDocLoaderException exception = new FeishuDocLoaderException("LOAD_ERROR", "Load failed", cause);
        assertEquals("Load failed", exception.getMessage());
        assertEquals("LOAD_ERROR", exception.getErrorCode());
        assertEquals(cause, exception.getCause());
    }
}