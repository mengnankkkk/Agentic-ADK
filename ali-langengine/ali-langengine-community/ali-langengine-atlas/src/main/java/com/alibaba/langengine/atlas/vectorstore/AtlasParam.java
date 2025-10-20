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

import lombok.Data;


@Data
public class AtlasParam {

    /**
     * Field name for unique ID
     */
    private String fieldNameUniqueId = "content_id";

    /**
     * Field name for embedding vector
     */
    private String fieldNameEmbedding = "embeddings";

    /**
     * Field name for page content
     */
    private String fieldNamePageContent = "row_content";

    /**
     * Vector search index name
     */
    private String vectorIndexName = "vector_index";

    /**
     * Number of candidates for vector search
     */
    private int numCandidates = 100;

    /**
     * Initialization parameters for creating collection
     */
    private InitParam initParam = new InitParam();

    @Data
    public static class InitParam {

        /**
         * Whether to use uniqueId as primary key
         */
        private boolean fieldUniqueIdAsPrimaryKey = true;

        /**
         * Embedding vector dimension
         */
        private int fieldEmbeddingsDimension = 1536;

        /**
         * Vector search similarity metric
         */
        private String similarity = "cosine";

    }

}