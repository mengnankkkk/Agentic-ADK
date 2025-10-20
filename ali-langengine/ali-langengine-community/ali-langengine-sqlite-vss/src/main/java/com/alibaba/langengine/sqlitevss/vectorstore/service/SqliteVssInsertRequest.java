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
public class SqliteVssInsertRequest {

    /**
     * Document unique ID
     */
    private String id;

    /**
     * Document content
     */
    private String content;

    /**
     * Document vector
     */
    private List<Double> vector;

    /**
     * Document metadata
     */
    private Map<String, Object> metadata;

    /**
     * Collection name
     */
    private String collectionName;

    /**
     * Whether to update if exists
     */
    @Builder.Default
    private boolean upsert = false;
}
