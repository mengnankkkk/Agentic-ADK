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

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class Tile38Param {

    /**
     * Tile38 server host
     */
    @Builder.Default
    private String host = "localhost";

    /**
     * Tile38 server port
     */
    @Builder.Default
    private int port = 9851;

    /**
     * Tile38 server password
     */
    private String password;

    /**
     * Connection timeout in milliseconds
     */
    @Builder.Default
    private int timeout = 5000;

    /**
     * Collection name for storing vectors
     */
    private String collectionName;

    /**
     * Vector dimension
     */
    @Builder.Default
    private int dimension = 1536;

    /**
     * Distance metric for similarity search
     */
    @Builder.Default
    private String distanceMetric = "cosine";

    /**
     * Connection pool size
     */
    @Builder.Default
    private int poolSize = 10;

    /**
     * Connection pool max idle time in seconds
     */
    @Builder.Default
    private int maxIdleTime = 300;

    /**
     * Enable SSL/TLS
     */
    @Builder.Default
    private boolean enableSsl = false;

    /**
     * API key for authentication
     */
    private String apiKey;

    /**
     * Batch size for bulk operations
     */
    @Builder.Default
    private int batchSize = 100;

    /**
     * Enable input validation
     */
    @Builder.Default
    private boolean enableValidation = true;

    /**
     * Maximum query result size
     */
    @Builder.Default
    private int maxResultSize = 1000;

}