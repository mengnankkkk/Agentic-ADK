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

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
public class DashVectorClientPool {
    
    private static final ConcurrentHashMap<String, Object> clientPool = new ConcurrentHashMap<>();
    private static final AtomicInteger connectionCount = new AtomicInteger(0);
    private static final int MAX_CONNECTIONS = 10;
    
    public static synchronized Object getClient(String apiKey, String endpoint) {
        String key = apiKey + ":" + endpoint;
        return clientPool.computeIfAbsent(key, k -> {
            if (connectionCount.get() >= MAX_CONNECTIONS) {
                throw new DashVectorException("CONNECTION_LIMIT_EXCEEDED", 
                    "Maximum connection limit reached: " + MAX_CONNECTIONS);
            }
            connectionCount.incrementAndGet();
            log.info("Creating new DashVector client connection. Total connections: {}", connectionCount.get());
            return new Object(); // 实际应该创建真实的 DashVector 客户端
        });
    }
    
    public static synchronized void releaseClient(String apiKey, String endpoint) {
        String key = apiKey + ":" + endpoint;
        if (clientPool.remove(key) != null) {
            connectionCount.decrementAndGet();
            log.info("Released DashVector client connection. Remaining connections: {}", connectionCount.get());
        }
    }
    
    public static int getActiveConnections() {
        return connectionCount.get();
    }
}