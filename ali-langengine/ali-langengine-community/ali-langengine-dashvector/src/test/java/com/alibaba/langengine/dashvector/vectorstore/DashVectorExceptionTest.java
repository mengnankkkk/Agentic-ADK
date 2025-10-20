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
package com.alibaba.langengine.dashvector.vectorstore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class DashVectorExceptionTest {

    @Test
    public void testConstructorWithMessage() {
        String message = "Test error message";
        DashVectorException exception = new DashVectorException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getErrorCode());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");
        DashVectorException exception = new DashVectorException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNull(exception.getErrorCode());
    }

    @Test
    public void testConstructorWithErrorCodeAndMessage() {
        String errorCode = "DASHVECTOR_001";
        String message = "Test error message";
        DashVectorException exception = new DashVectorException(errorCode, message);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructorWithErrorCodeMessageAndCause() {
        String errorCode = "DASHVECTOR_002";
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");
        DashVectorException exception = new DashVectorException(errorCode, message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testInheritanceFromRuntimeException() {
        DashVectorException exception = new DashVectorException("Test message");
        assertTrue(exception instanceof RuntimeException);
    }
    
    @Test
    public void testErrorCodeConstructor() {
        DashVectorException exception = new DashVectorException(
            DashVectorException.ErrorCode.CONNECTION_FAILED, "Connection failed");
        
        assertEquals("Connection failed", exception.getMessage());
        assertEquals("DASHVECTOR_001", exception.getErrorCode());
        assertTrue(exception.getTimestamp() > 0);
    }
    
    @Test
    public void testErrorCodeWithCauseConstructor() {
        Throwable cause = new RuntimeException("Root cause");
        DashVectorException exception = new DashVectorException(
            DashVectorException.ErrorCode.AUTHENTICATION_FAILED, "Auth failed", cause);
        
        assertEquals("Auth failed", exception.getMessage());
        assertEquals("DASHVECTOR_002", exception.getErrorCode());
        assertEquals(cause, exception.getCause());
        assertTrue(exception.getTimestamp() > 0);
    }
    
    @Test
    public void testTimestampIsSet() {
        long beforeException = System.currentTimeMillis();
        DashVectorException exception = new DashVectorException("Test message");
        long afterException = System.currentTimeMillis();
        
        assertTrue(exception.getTimestamp() >= beforeException);
        assertTrue(exception.getTimestamp() <= afterException);
    }
    
    @Test
    public void testAllErrorCodes() {
        DashVectorException.ErrorCode[] errorCodes = DashVectorException.ErrorCode.values();
        assertTrue(errorCodes.length > 0);
        
        for (DashVectorException.ErrorCode errorCode : errorCodes) {
            assertNotNull(errorCode.getCode());
            assertNotNull(errorCode.getDescription());
            assertFalse(errorCode.getCode().isEmpty());
            assertFalse(errorCode.getDescription().isEmpty());
        }
    }

}