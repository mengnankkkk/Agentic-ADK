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

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
public class Tile38ConnectionPool {

    private final RedisClient redisClient;
    private final GenericObjectPool<StatefulRedisConnection<String, String>> connectionPool;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public Tile38ConnectionPool(Tile38Param param) {
        try {
            RedisURI.Builder uriBuilder = RedisURI.Builder
                    .redis(param.getHost(), param.getPort())
                    .withTimeout(Duration.ofMillis(param.getTimeout()));

            if (param.getPassword() != null) {
                uriBuilder.withPassword(param.getPassword().toCharArray());
            }

            if (param.isEnableSsl()) {
                uriBuilder.withSsl(true);
            }

            RedisURI redisURI = uriBuilder.build();
            this.redisClient = RedisClient.create(redisURI);

            GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig = 
                new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(param.getPoolSize());
            poolConfig.setMaxIdle(param.getPoolSize());
            poolConfig.setMinIdle(1);
            poolConfig.setMaxWaitMillis(param.getTimeout());
            poolConfig.setTimeBetweenEvictionRunsMillis(30000);
            poolConfig.setMinEvictableIdleTimeMillis(param.getMaxIdleTime() * 1000L);

            this.connectionPool = ConnectionPoolSupport.createGenericObjectPool(
                () -> redisClient.connect(), poolConfig);

            log.info("Tile38 connection pool initialized with {} connections", param.getPoolSize());
        } catch (Exception e) {
            throw new Tile38Exception("POOL_INIT_ERROR", "Failed to initialize connection pool", e);
        }
    }

    public StatefulRedisConnection<String, String> borrowConnection() {
        if (closed.get()) {
            throw new Tile38Exception("POOL_CLOSED", "Connection pool is closed");
        }
        try {
            return connectionPool.borrowObject();
        } catch (Exception e) {
            throw new Tile38Exception("CONNECTION_BORROW_ERROR", "Failed to borrow connection", e);
        }
    }

    public void returnConnection(StatefulRedisConnection<String, String> connection) {
        if (connection != null && !closed.get()) {
            try {
                connectionPool.returnObject(connection);
            } catch (Exception e) {
                log.warn("Failed to return connection to pool", e);
            }
        }
    }

    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                connectionPool.close();
                redisClient.shutdown();
                log.info("Tile38 connection pool closed");
            } catch (Exception e) {
                log.error("Error closing connection pool", e);
            }
        }
    }

    public boolean isClosed() {
        return closed.get();
    }

    public int getActiveConnections() {
        return connectionPool.getNumActive();
    }

    public int getIdleConnections() {
        return connectionPool.getNumIdle();
    }

}