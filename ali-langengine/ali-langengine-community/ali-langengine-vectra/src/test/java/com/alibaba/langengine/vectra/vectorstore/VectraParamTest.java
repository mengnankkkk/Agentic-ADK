package com.alibaba.langengine.vectra.vectorstore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VectraParamTest {

    @Test
    void testDefaultValues() {
        VectraParam param = new VectraParam();
        
        assertEquals("page_content", param.getFieldNamePageContent());
        assertEquals("content_id", param.getFieldNameUniqueId());
        assertEquals("vector", param.getFieldNameVector());
        assertEquals("metadata", param.getFieldNameMetadata());
        assertEquals(30, param.getConnectTimeout());
        assertEquals(60, param.getReadTimeout());
        assertEquals(60, param.getWriteTimeout());
    }

    @Test
    void testCollectionParamDefaults() {
        VectraParam.CollectionParam collectionParam = new VectraParam.CollectionParam();
        
        assertEquals(1536, collectionParam.getVectorDimension());
        assertEquals("cosine", collectionParam.getMetricType());
        assertTrue(collectionParam.isAutoCreateCollection());
        assertEquals("hnsw", collectionParam.getIndexType());
    }

    @Test
    void testHnswParamDefaults() {
        VectraParam.CollectionParam.HnswParam hnswParam = new VectraParam.CollectionParam.HnswParam();
        
        assertEquals(16, hnswParam.getM());
        assertEquals(200, hnswParam.getEfConstruction());
    }
}