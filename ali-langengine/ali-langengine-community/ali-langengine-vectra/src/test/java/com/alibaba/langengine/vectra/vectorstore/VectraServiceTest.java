package com.alibaba.langengine.vectra.vectorstore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VectraServiceTest {

    @Test
    void testVectraParamDefaults() {
        VectraParam param = new VectraParam();
        
        assertEquals("page_content", param.getFieldNamePageContent());
        assertEquals("content_id", param.getFieldNameUniqueId());
        assertEquals("vector", param.getFieldNameVector());
        assertEquals("metadata", param.getFieldNameMetadata());
        assertEquals(30, param.getConnectTimeout());
        assertEquals(60, param.getReadTimeout());
        assertEquals(60, param.getWriteTimeout());
        
        VectraParam.CollectionParam collectionParam = param.getCollectionParam();
        assertEquals(1536, collectionParam.getVectorDimension());
        assertEquals("cosine", collectionParam.getMetricType());
        assertTrue(collectionParam.isAutoCreateCollection());
        assertEquals("hnsw", collectionParam.getIndexType());
    }
}