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
package com.alibaba.langengine.infinity.vectorstore;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;


@Data
public class InfinityParam {

    /**
     * 向量字段名
     */
    private String fieldNameEmbedding = "embeddings";

    /**
     * 内容字段名
     */
    private String fieldNamePageContent = "page_content";

    /**
     * 唯一ID字段名
     */
    private String fieldNameUniqueId = "unique_id";

    /**
     * 元数据字段名
     */
    private String fieldNameMetadata = "metadata";

    /**
     * 自定义搜索扩展参数
     */
    private Map<String, Object> searchParams = new HashMap<>();

    /**
     * 初始化参数, 用于创建Table
     */
    private InitParam initParam = new InitParam();

    @Data
    public static class InitParam {

        /**
         * 是否使用uniqueId作为主键, 如果是的话, addDocuments的时候uniqueId不要为空
         */
        private boolean fieldUniqueIdAsPrimaryKey = true;

        /**
         * pageContent字段最大长度
         */
        private int fieldPageContentMaxLength = 8192;

        /**
         * embeddings字段向量维度, 如果设置为0, 则会通过embedding模型查询一条数据, 看维度是多少
         */
        private int fieldEmbeddingsDimension = 1536;

        /**
         * 相似度搜索的距离类型
         * 支持: l2, ip (inner product), cosine
         */
        private String distanceType = "cosine";

        /**
         * 索引类型
         * 支持: ivfflat, hnsw
         */
        private String indexType = "hnsw";

        /**
         * 索引参数
         */
        private Map<String, Object> indexParams = new HashMap<>();

        /**
         * 默认构造函数，设置HNSW索引默认参数
         */
        public InitParam() {
            // HNSW默认参数
            indexParams.put("M", 16);
            indexParams.put("ef_construction", 200);
            indexParams.put("ef", 200);
        }
    }

    /**
     * 默认构造函数，设置搜索默认参数
     */
    public InfinityParam() {
        // 默认搜索参数
        searchParams.put("ef", 200);
        searchParams.put("limit", 10);
    }
}
