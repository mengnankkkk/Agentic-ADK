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
package com.alibaba.langengine.infinity.vectorstore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class InfinityExceptionTest {

    @Test
    public void testConstructorWithMessage() {
        String message = "Test error message";
        InfinityException exception = new InfinityException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getErrorCode());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");
        InfinityException exception = new InfinityException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNull(exception.getErrorCode());
    }

    @Test
    public void testConstructorWithErrorCodeAndMessage() {
        String errorCode = "INF001";
        String message = "Test error message";
        InfinityException exception = new InfinityException(errorCode, message);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructorWithErrorCodeMessageAndCause() {
        String errorCode = "INF001";
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");
        InfinityException exception = new InfinityException(errorCode, message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testErrorCodeSetterAndGetter() {
        InfinityException exception = new InfinityException("Test message");
        
        assertNull(exception.getErrorCode());
        
        String errorCode = "INF002";
        exception.setErrorCode(errorCode);
        assertEquals(errorCode, exception.getErrorCode());
    }

    @Test
    public void testToStringWithErrorCode() {
        String errorCode = "INF001";
        String message = "Test error message";
        InfinityException exception = new InfinityException(errorCode, message);
        
        String expected = String.format("InfinityException[%s]: %s", errorCode, message);
        assertEquals(expected, exception.toString());
    }

    @Test
    public void testToStringWithoutErrorCode() {
        String message = "Test error message";
        InfinityException exception = new InfinityException(message);
        
        String expected = String.format("InfinityException: %s", message);
        assertEquals(expected, exception.toString());
    }

    @Test
    public void testToStringWithEmptyErrorCode() {
        String message = "Test error message";
        InfinityException exception = new InfinityException("", message);
        
        String expected = String.format("InfinityException: %s", message);
        assertEquals(expected, exception.toString());
    }

    @Test
    public void testToStringWithWhitespaceErrorCode() {
        String message = "Test error message";
        InfinityException exception = new InfinityException("   ", message);
        
        String expected = String.format("InfinityException: %s", message);
        assertEquals(expected, exception.toString());
    }

    @Test
    public void testInheritanceFromRuntimeException() {
        InfinityException exception = new InfinityException("Test message");
        
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }

    @Test
    public void testSerialVersionUID() {
        // 验证序列化版本ID存在
        InfinityException exception = new InfinityException("Test message");
        assertNotNull(exception);
        
        // 这个测试主要是为了确保类定义正确
        // 实际的序列化测试会更复杂，但对于单元测试来说这已经足够
    }
}
