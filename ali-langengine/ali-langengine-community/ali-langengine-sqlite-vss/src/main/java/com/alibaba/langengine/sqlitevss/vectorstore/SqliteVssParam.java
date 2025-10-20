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
package com.alibaba.langengine.sqlitevss.vectorstore;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@Builder
public class SqliteVssParam {

    /**
     * Database file path
     */
    @Builder.Default
    private String dbPath = "vectorstore.db";

    /**
     * Collection name (table name)
     */
    @Builder.Default
    private String collectionName = "documents";

    /**
     * Vector dimension
     */
    @Builder.Default
    private int vectorDimension = 1536;

    /**
     * Distance metric for similarity search
     * Supported: cosine, l2, inner_product
     */
    @Builder.Default
    private String distanceMetric = "cosine";

    /**
     * Maximum pool size for connection pooling
     */
    @Builder.Default
    private int maxPoolSize = 10;

    /**
     * Connection timeout in seconds
     */
    @Builder.Default
    private long connectionTimeoutSeconds = 30;

    /**
     * Maximum idle time in seconds
     */
    @Builder.Default
    private long maxIdleTimeSeconds = 600;

    /**
     * Whether to enable WAL mode for better performance
     */
    @Builder.Default
    private boolean enableWalMode = true;

    /**
     * Whether to enable foreign keys
     */
    @Builder.Default
    private boolean enableForeignKeys = true;

    /**
     * SQLite page size in bytes
     */
    @Builder.Default
    private int pageSize = 4096;

    /**
     * Cache size in pages
     */
    @Builder.Default
    private int cacheSize = 2000;

    /**
     * Whether to auto-create table if not exists
     */
    @Builder.Default
    private boolean autoCreateTable = true;

    /**
     * Index creation parameters
     */
    @Builder.Default
    private IndexParam indexParam = IndexParam.builder().build();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndexParam {

        /**
         * Index type for vector search
         * Currently only supports 'flat' for exact search
         */
        @Builder.Default
        private String indexType = "flat";

        /**
         * Whether to create index immediately after table creation
         */
        @Builder.Default
        private boolean createIndexOnStartup = true;

        /**
         * Index rebuild threshold (number of documents)
         */
        @Builder.Default
        private int rebuildThreshold = 10000;

        /**
         * 验证索引参数的有效性
         * 
         * @throws SqliteVssException 如果参数无效
         */
        public void validate() {
            if (indexType == null || indexType.trim().isEmpty()) {
                throw SqliteVssException.configError("Index type cannot be null or empty");
            }
            
            String normalizedType = indexType.toLowerCase().trim();
            if (!normalizedType.equals("flat")) {
                throw SqliteVssException.configError("Unsupported index type: " + indexType + ". Currently only 'flat' is supported");
            }
            
            if (rebuildThreshold <= 0) {
                throw SqliteVssException.configError("Rebuild threshold must be greater than 0, got: " + rebuildThreshold);
            }
        }
    }

    /**
     * Create default parameters
     */
    public static SqliteVssParam defaultParam() {
        return SqliteVssParam.builder().build();
    }

    /**
     * 验证参数的有效性
     * 
     * @throws SqliteVssException 如果参数无效
     */
    public void validate() {
        if (dbPath == null || dbPath.trim().isEmpty()) {
            throw SqliteVssException.configError("Database path cannot be null or empty");
        }
        
        if (collectionName == null || collectionName.trim().isEmpty()) {
            throw SqliteVssException.configError("Collection name cannot be null or empty");
        }
        
        if (vectorDimension <= 0) {
            throw SqliteVssException.configError("Vector dimension must be greater than 0, got: " + vectorDimension);
        }
        
        if (vectorDimension > 10000) {
            throw SqliteVssException.configError("Vector dimension too large (max 10000), got: " + vectorDimension);
        }
        
        if (distanceMetric == null || distanceMetric.trim().isEmpty()) {
            throw SqliteVssException.configError("Distance metric cannot be null or empty");
        }
        
        String normalizedMetric = distanceMetric.toLowerCase().trim();
        if (!normalizedMetric.equals("cosine") && !normalizedMetric.equals("l2") && !normalizedMetric.equals("inner_product")) {
            throw SqliteVssException.configError("Unsupported distance metric: " + distanceMetric + ". Supported: cosine, l2, inner_product");
        }
        
        if (maxPoolSize <= 0) {
            throw SqliteVssException.configError("Max pool size must be greater than 0, got: " + maxPoolSize);
        }
        
        if (connectionTimeoutSeconds <= 0) {
            throw SqliteVssException.configError("Connection timeout must be greater than 0, got: " + connectionTimeoutSeconds);
        }
        
        if (indexParam != null) {
            indexParam.validate();
        }
    }

    /**
     * Create parameters with custom database path
     */
    public static SqliteVssParam withDbPath(String dbPath) {
        return SqliteVssParam.builder()
                .dbPath(dbPath)
                .build();
    }

    /**
     * Create parameters with custom database path and collection name
     */
    public static SqliteVssParam withDbPathAndCollection(String dbPath, String collectionName) {
        return SqliteVssParam.builder()
                .dbPath(dbPath)
                .collectionName(collectionName)
                .build();
    }
}
