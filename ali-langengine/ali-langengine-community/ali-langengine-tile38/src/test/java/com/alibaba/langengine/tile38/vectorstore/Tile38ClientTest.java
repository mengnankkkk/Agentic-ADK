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
package com.alibaba.langengine.tile38.vectorstore;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


public class Tile38ClientTest {

    @Test
    public void testTile38ParamBuilder() {
        Tile38Param param = Tile38Param.builder()
                .host("test-host")
                .port(9999)
                .password("test-password")
                .timeout(10000)
                .collectionName("test-collection")
                .dimension(512)
                .distanceMetric("euclidean")
                .build();

        assertEquals("test-host", param.getHost());
        assertEquals(9999, param.getPort());
        assertEquals("test-password", param.getPassword());
        assertEquals(10000, param.getTimeout());
        assertEquals("test-collection", param.getCollectionName());
        assertEquals(512, param.getDimension());
        assertEquals("euclidean", param.getDistanceMetric());
    }

    @Test
    public void testTile38ParamDefaults() {
        Tile38Param param = Tile38Param.builder().build();

        assertEquals("localhost", param.getHost());
        assertEquals(9851, param.getPort());
        assertEquals(5000, param.getTimeout());
        assertEquals(1536, param.getDimension());
        assertEquals("cosine", param.getDistanceMetric());
        assertNull(param.getPassword());
        assertNull(param.getCollectionName());
    }

    @Test
    public void testTile38Exception() {
        // Test basic constructor
        Tile38Exception exception1 = new Tile38Exception("Test message");
        assertEquals("Test message", exception1.getMessage());
        assertNull(exception1.getErrorCode());

        // Test constructor with cause
        RuntimeException cause = new RuntimeException("Root cause");
        Tile38Exception exception2 = new Tile38Exception("Test message", cause);
        assertEquals("Test message", exception2.getMessage());
        assertEquals(cause, exception2.getCause());

        // Test constructor with error code
        Tile38Exception exception3 = new Tile38Exception("ERROR_001", "Test message");
        assertEquals("Test message", exception3.getMessage());
        assertEquals("ERROR_001", exception3.getErrorCode());

        // Test constructor with error code and cause
        Tile38Exception exception4 = new Tile38Exception("ERROR_002", "Test message", cause);
        assertEquals("Test message", exception4.getMessage());
        assertEquals("ERROR_002", exception4.getErrorCode());
        assertEquals(cause, exception4.getCause());
    }

    @Test
    public void testClientConnectionFailure() {
        // Test with invalid parameters that should cause connection failure
        Tile38Param invalidParam = Tile38Param.builder()
                .host("invalid-host-that-does-not-exist")
                .port(9999)
                .timeout(1000)
                .build();

        // Create client (this won't fail immediately)
        Tile38Client client = new Tile38Client(invalidParam);
        
        // This should throw a Tile38Exception when trying to use the connection
        assertThrows(Tile38Exception.class, () -> {
            client.set("test", "id1", 1.0, 1.0, null);
        });
        
        client.close();
    }

}