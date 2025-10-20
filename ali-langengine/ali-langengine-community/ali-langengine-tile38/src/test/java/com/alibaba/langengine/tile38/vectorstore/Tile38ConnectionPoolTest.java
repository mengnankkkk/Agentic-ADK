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

import static org.junit.jupiter.api.Assertions.*;


public class Tile38ConnectionPoolTest {

    @Test
    public void testConnectionPoolCreationFailure() {
        Tile38Param param = Tile38Param.builder()
                .host("invalid-host-that-does-not-exist")
                .port(9999)
                .poolSize(5)
                .timeout(1000)
                .build();

        // Create pool (this won't fail immediately)
        Tile38ConnectionPool pool = new Tile38ConnectionPool(param);
        
        // This should throw a Tile38Exception when trying to borrow a connection
        assertThrows(Tile38Exception.class, () -> {
            pool.borrowConnection();
        });
        
        pool.close();
    }

    @Test
    public void testParamBuilderWithPoolSettings() {
        Tile38Param param = Tile38Param.builder()
                .host("localhost")
                .port(9851)
                .poolSize(20)
                .maxIdleTime(600)
                .enableSsl(true)
                .apiKey("test-key")
                .batchSize(200)
                .enableValidation(false)
                .maxResultSize(2000)
                .build();

        assertEquals(20, param.getPoolSize());
        assertEquals(600, param.getMaxIdleTime());
        assertTrue(param.isEnableSsl());
        assertEquals("test-key", param.getApiKey());
        assertEquals(200, param.getBatchSize());
        assertFalse(param.isEnableValidation());
        assertEquals(2000, param.getMaxResultSize());
    }

    @Test
    public void testParamBuilderDefaults() {
        Tile38Param param = Tile38Param.builder().build();

        assertEquals(10, param.getPoolSize());
        assertEquals(300, param.getMaxIdleTime());
        assertFalse(param.isEnableSsl());
        assertNull(param.getApiKey());
        assertEquals(100, param.getBatchSize());
        assertTrue(param.isEnableValidation());
        assertEquals(1000, param.getMaxResultSize());
    }

}