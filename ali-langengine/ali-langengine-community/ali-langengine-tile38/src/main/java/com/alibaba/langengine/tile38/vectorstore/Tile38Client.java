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
import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;


@Slf4j
public class Tile38Client {

    private final Tile38ConnectionPool connectionPool;
    private final Tile38SecurityValidator validator;
    private final String apiKey;

    public Tile38Client(Tile38Param param) {
        try {
            this.connectionPool = new Tile38ConnectionPool(param);
            this.validator = new Tile38SecurityValidator(param.isEnableValidation());
            this.apiKey = param.getApiKey();
            
            log.info("Tile38 client initialized with connection pool for {}:{}", 
                param.getHost(), param.getPort());
        } catch (Exception e) {
            throw new Tile38Exception("CONNECT_ERROR", "Failed to initialize Tile38 client", e);
        }
    }

    /**
     * Set a point in a collection
     */
    public String set(String collection, String id, double lat, double lon, Map<String, String> fields) {
        validator.validateCollectionName(collection);
        validator.validateDocumentId(id);
        validator.validateCoordinates(lat, lon);
        if (fields != null) {
            validator.validateFieldCount(fields.size());
        }

        StatefulRedisConnection<String, String> connection = null;
        try {
            connection = connectionPool.borrowConnection();
            RedisCommands<String, String> commands = connection.sync();
            
            StringBuilder cmd = new StringBuilder();
            cmd.append("SET ").append(collection).append(" ").append(id);
            
            if (fields != null && !fields.isEmpty()) {
                for (Map.Entry<String, String> entry : fields.entrySet()) {
                    validator.validateContent(entry.getValue());
                    cmd.append(" FIELD ").append(entry.getKey()).append(" ").append(entry.getValue());
                }
            }
            
            cmd.append(" POINT ").append(lat).append(" ").append(lon);
            
            return commands.eval(cmd.toString(), null);
        } catch (Exception e) {
            throw new Tile38Exception("SET_ERROR", "Failed to set point in collection: " + collection, e);
        } finally {
            if (connection != null) {
                connectionPool.returnConnection(connection);
            }
        }
    }

    /**
     * Search nearby points
     */
    public List<Object> nearby(String collection, double lat, double lon, int limit) {
        validator.validateCollectionName(collection);
        validator.validateCoordinates(lat, lon);
        
        StatefulRedisConnection<String, String> connection = null;
        try {
            connection = connectionPool.borrowConnection();
            String cmd = String.format("NEARBY %s POINT %f %f LIMIT %d", collection, lat, lon, limit);
            return (List<Object>) connection.sync().eval(cmd, null);
        } catch (Exception e) {
            throw new Tile38Exception("NEARBY_ERROR", "Failed to search nearby in collection: " + collection, e);
        } finally {
            if (connection != null) {
                connectionPool.returnConnection(connection);
            }
        }
    }

    /**
     * Search within a radius
     */
    public List<Object> within(String collection, double lat, double lon, double radius, int limit) {
        validator.validateCollectionName(collection);
        validator.validateCoordinates(lat, lon);
        
        StatefulRedisConnection<String, String> connection = null;
        try {
            connection = connectionPool.borrowConnection();
            String cmd = String.format("WITHIN %s CIRCLE %f %f %f LIMIT %d", collection, lat, lon, radius, limit);
            return (List<Object>) connection.sync().eval(cmd, null);
        } catch (Exception e) {
            throw new Tile38Exception("WITHIN_ERROR", "Failed to search within radius in collection: " + collection, e);
        } finally {
            if (connection != null) {
                connectionPool.returnConnection(connection);
            }
        }
    }

    /**
     * Delete an object by id
     */
    public String del(String collection, String id) {
        validator.validateCollectionName(collection);
        validator.validateDocumentId(id);
        
        StatefulRedisConnection<String, String> connection = null;
        try {
            connection = connectionPool.borrowConnection();
            String cmd = String.format("DEL %s %s", collection, id);
            return connection.sync().eval(cmd, null);
        } catch (Exception e) {
            throw new Tile38Exception("DELETE_ERROR", "Failed to delete object from collection: " + collection, e);
        } finally {
            if (connection != null) {
                connectionPool.returnConnection(connection);
            }
        }
    }

    /**
     * Drop a collection
     */
    public String drop(String collection) {
        validator.validateCollectionName(collection);
        
        StatefulRedisConnection<String, String> connection = null;
        try {
            connection = connectionPool.borrowConnection();
            String cmd = String.format("DROP %s", collection);
            return connection.sync().eval(cmd, null);
        } catch (Exception e) {
            throw new Tile38Exception("DROP_ERROR", "Failed to drop collection: " + collection, e);
        } finally {
            if (connection != null) {
                connectionPool.returnConnection(connection);
            }
        }
    }

    /**
     * Close the client connection pool
     */
    public void close() {
        try {
            if (connectionPool != null) {
                connectionPool.close();
            }
            log.info("Tile38 client connection pool closed");
        } catch (Exception e) {
            log.error("Error closing Tile38 client", e);
        }
    }

}