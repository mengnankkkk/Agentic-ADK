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
package com.alibaba.langengine.atlas.vectorstore;

import com.alibaba.langengine.atlas.AtlasConfiguration;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ConnectionPoolSettings;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;


@Slf4j
public class AtlasConnectionManager {

    private static final ConcurrentHashMap<String, MongoClient> clientCache = new ConcurrentHashMap<>();
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Get or create MongoDB client with connection pooling
     */
    public static MongoClient getClient(String connectionString) {
        lock.readLock().lock();
        try {
            MongoClient client = clientCache.get(connectionString);
            if (client != null) {
                return client;
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            // Double-check pattern
            MongoClient client = clientCache.get(connectionString);
            if (client != null) {
                return client;
            }

            client = createClient(connectionString);
            clientCache.put(connectionString, client);
            log.info("Created new MongoDB client for connection: {}", maskConnectionString(connectionString));
            return client;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static MongoClient createClient(String connectionString) {
        try {
            ConnectionPoolSettings poolSettings = ConnectionPoolSettings.builder()
                    .maxSize(AtlasConfiguration.ATLAS_MAX_POOL_SIZE)
                    .minSize(AtlasConfiguration.ATLAS_MIN_POOL_SIZE)
                    .maxWaitTime(AtlasConfiguration.ATLAS_MAX_WAIT_TIME, TimeUnit.MILLISECONDS)
                    .maxConnectionIdleTime(AtlasConfiguration.ATLAS_MAX_IDLE_TIME, TimeUnit.MILLISECONDS)
                    .build();

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(connectionString))
                    .applyToConnectionPoolSettings(builder -> builder.applySettings(poolSettings))
                    .build();

            return MongoClients.create(settings);
        } catch (Exception e) {
            throw new AtlasException("CONNECTION_POOL_FAILED", "Failed to create MongoDB client with connection pool", e);
        }
    }

    /**
     * Close all connections safely
     */
    public static void closeAll() {
        lock.writeLock().lock();
        try {
            clientCache.values().forEach(client -> {
                try {
                    client.close();
                } catch (Exception e) {
                    log.warn("Error closing MongoDB client: {}", e.getMessage());
                }
            });
            clientCache.clear();
            log.info("All MongoDB connections closed");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Close specific connection
     */
    public static void closeConnection(String connectionString) {
        lock.writeLock().lock();
        try {
            MongoClient client = clientCache.remove(connectionString);
            if (client != null) {
                client.close();
                log.info("Closed MongoDB connection: {}", maskConnectionString(connectionString));
            }
        } catch (Exception e) {
            log.warn("Error closing MongoDB connection: {}", e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static String maskConnectionString(String connectionString) {
        if (connectionString == null) return "null";
        return connectionString.replaceAll("://[^@]+@", "://***:***@");
    }

}