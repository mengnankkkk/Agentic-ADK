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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class InfinityParamTest {

    @Test
    public void testDefaultConstructor() {
        InfinityParam param = new InfinityParam();
        
        // 验证默认字段名
        assertEquals("embeddings", param.getFieldNameEmbedding());
        assertEquals("page_content", param.getFieldNamePageContent());
        assertEquals("unique_id", param.getFieldNameUniqueId());
        assertEquals("metadata", param.getFieldNameMetadata());
        
        // 验证默认搜索参数
        assertNotNull(param.getSearchParams());
        assertEquals(200, param.getSearchParams().get("ef"));
        assertEquals(10, param.getSearchParams().get("limit"));
        
        // 验证初始化参数
        assertNotNull(param.getInitParam());
    }

    @Test
    public void testInitParamDefaults() {
        InfinityParam.InitParam initParam = new InfinityParam.InitParam();
        
        // 验证默认值
        assertTrue(initParam.isFieldUniqueIdAsPrimaryKey());
        assertEquals(8192, initParam.getFieldPageContentMaxLength());
        assertEquals(1536, initParam.getFieldEmbeddingsDimension());
        assertEquals("cosine", initParam.getDistanceType());
        assertEquals("hnsw", initParam.getIndexType());
        
        // 验证默认索引参数
        assertNotNull(initParam.getIndexParams());
        assertEquals(16, initParam.getIndexParams().get("M"));
        assertEquals(200, initParam.getIndexParams().get("ef_construction"));
        assertEquals(200, initParam.getIndexParams().get("ef"));
    }

    @Test
    public void testFieldNameSetters() {
        InfinityParam param = new InfinityParam();
        
        param.setFieldNameEmbedding("custom_embeddings");
        param.setFieldNamePageContent("custom_content");
        param.setFieldNameUniqueId("custom_id");
        param.setFieldNameMetadata("custom_metadata");
        
        assertEquals("custom_embeddings", param.getFieldNameEmbedding());
        assertEquals("custom_content", param.getFieldNamePageContent());
        assertEquals("custom_id", param.getFieldNameUniqueId());
        assertEquals("custom_metadata", param.getFieldNameMetadata());
    }

    @Test
    public void testInitParamSetters() {
        InfinityParam.InitParam initParam = new InfinityParam.InitParam();
        
        initParam.setFieldUniqueIdAsPrimaryKey(false);
        initParam.setFieldPageContentMaxLength(4096);
        initParam.setFieldEmbeddingsDimension(768);
        initParam.setDistanceType("l2");
        initParam.setIndexType("ivfflat");
        
        assertFalse(initParam.isFieldUniqueIdAsPrimaryKey());
        assertEquals(4096, initParam.getFieldPageContentMaxLength());
        assertEquals(768, initParam.getFieldEmbeddingsDimension());
        assertEquals("l2", initParam.getDistanceType());
        assertEquals("ivfflat", initParam.getIndexType());
    }

    @Test
    public void testSearchParamsModification() {
        InfinityParam param = new InfinityParam();
        
        param.getSearchParams().put("custom_param", "custom_value");
        param.getSearchParams().put("ef", 100); // 修改默认值
        
        assertEquals("custom_value", param.getSearchParams().get("custom_param"));
        assertEquals(100, param.getSearchParams().get("ef"));
        assertEquals(10, param.getSearchParams().get("limit")); // 确保其他默认值不变
    }

    @Test
    public void testIndexParamsModification() {
        InfinityParam.InitParam initParam = new InfinityParam.InitParam();
        
        initParam.getIndexParams().put("custom_index_param", "custom_value");
        initParam.getIndexParams().put("M", 32); // 修改默认值
        
        assertEquals("custom_value", initParam.getIndexParams().get("custom_index_param"));
        assertEquals(32, initParam.getIndexParams().get("M"));
        assertEquals(200, initParam.getIndexParams().get("ef_construction")); // 确保其他默认值不变
    }
}
