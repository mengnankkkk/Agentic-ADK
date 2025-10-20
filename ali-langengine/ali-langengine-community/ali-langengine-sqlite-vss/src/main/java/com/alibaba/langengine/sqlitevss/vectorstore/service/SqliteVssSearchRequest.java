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
package com.alibaba.langengine.sqlitevss.vectorstore.service;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SqliteVssSearchRequest {

    /**
     * Query vector
     */
    private List<Double> queryVector;

    /**
     * Query text (will be embedded if queryVector is not provided)
     */
    private String queryText;

    /**
     * Number of results to return
     */
    @Builder.Default
    private int topK = 5;

    /**
     * Maximum distance threshold
     */
    private Double maxDistance;

    /**
     * Metadata filters
     */
    private Map<String, Object> metadataFilter;

    /**
     * Whether to include vectors in results
     */
    @Builder.Default
    private boolean includeVectors = false;

    /**
     * Whether to include distances in results
     */
    @Builder.Default
    private boolean includeDistances = true;

    /**
     * Collection name to search in
     */
    private String collectionName;
}
