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
package com.alibaba.langengine.vectra.vectorstore;

import lombok.Data;


@Data
public class VectraParam {

    /**
     * Collection configuration parameters
     */
    private CollectionParam collectionParam = new CollectionParam();

    /**
     * Field name for page content
     */
    private String fieldNamePageContent = "page_content";

    /**
     * Field name for unique ID
     */
    private String fieldNameUniqueId = "content_id";

    /**
     * Field name for vector embeddings
     */
    private String fieldNameVector = "vector";

    /**
     * Field name for metadata
     */
    private String fieldNameMetadata = "metadata";

    /**
     * Connection timeout in seconds
     */
    private int connectTimeout = 30;

    /**
     * Read timeout in seconds
     */
    private int readTimeout = 60;

    /**
     * Write timeout in seconds
     */
    private int writeTimeout = 60;

    @Data
    public static class CollectionParam {
        /**
         * Vector dimension
         */
        private int vectorDimension = 1536;

        /**
         * Distance metric: cosine, euclidean, dot_product
         */
        private String metricType = "cosine";

        /**
         * Whether to create collection if not exists
         */
        private boolean autoCreateCollection = true;

        /**
         * Index type for vector field
         */
        private String indexType = "hnsw";

        /**
         * HNSW index parameters
         */
        private HnswParam hnswParam = new HnswParam();

        @Data
        public static class HnswParam {
            /**
             * M parameter for HNSW algorithm
             */
            private int m = 16;

            /**
             * ef_construction parameter for HNSW algorithm
             */
            private int efConstruction = 200;

            /**
             * ef parameter for HNSW search
             */
            private int ef = 10;
        }
    }

    /**
     * Builder pattern for VectraParam
     */
    public static class Builder {
        private VectraParam param = new VectraParam();

        public Builder vectorDimension(int dimension) {
            param.getCollectionParam().setVectorDimension(dimension);
            return this;
        }

        public Builder metricType(String metricType) {
            param.getCollectionParam().setMetricType(metricType);
            return this;
        }

        public Builder fieldNamePageContent(String fieldName) {
            param.setFieldNamePageContent(fieldName);
            return this;
        }

        public Builder fieldNameUniqueId(String fieldName) {
            param.setFieldNameUniqueId(fieldName);
            return this;
        }

        public Builder fieldNameVector(String fieldName) {
            param.setFieldNameVector(fieldName);
            return this;
        }

        public Builder fieldNameMetadata(String fieldName) {
            param.setFieldNameMetadata(fieldName);
            return this;
        }

        public Builder connectTimeout(int timeout) {
            param.setConnectTimeout(timeout);
            return this;
        }

        public Builder readTimeout(int timeout) {
            param.setReadTimeout(timeout);
            return this;
        }

        public Builder writeTimeout(int timeout) {
            param.setWriteTimeout(timeout);
            return this;
        }

        public Builder autoCreateCollection(boolean autoCreate) {
            param.getCollectionParam().setAutoCreateCollection(autoCreate);
            return this;
        }

        public Builder hnswM(int m) {
            param.getCollectionParam().getHnswParam().setM(m);
            return this;
        }

        public Builder hnswEfConstruction(int efConstruction) {
            param.getCollectionParam().getHnswParam().setEfConstruction(efConstruction);
            return this;
        }

        public Builder hnswEf(int ef) {
            param.getCollectionParam().getHnswParam().setEf(ef);
            return this;
        }

        public VectraParam build() {
            return param;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}