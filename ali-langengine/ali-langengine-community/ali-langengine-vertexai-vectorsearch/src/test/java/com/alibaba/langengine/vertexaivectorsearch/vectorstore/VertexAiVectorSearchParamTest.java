package com.alibaba.langengine.vertexaivectorsearch.vectorstore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VertexAiVectorSearchParamTest {

    @Test
    void testDefaultValues() {
        VertexAiVectorSearchParam param = new VertexAiVectorSearchParam();
        
        assertEquals("id", param.getFieldNameUniqueId());
        assertEquals("embedding", param.getFieldNameEmbedding());
        assertEquals("content", param.getFieldNamePageContent());
        assertEquals("metadata", param.getFieldNameMetadata());
        assertEquals("vector_search_index", param.getIndexDisplayName());
        assertEquals("vector_search_endpoint", param.getEndpointDisplayName());
        assertEquals(10, param.getNeighborsCount());
        assertNotNull(param.getIndexInitParam());
    }

    @Test
    void testSettersAndGetters() {
        VertexAiVectorSearchParam param = new VertexAiVectorSearchParam();
        
        param.setFieldNameUniqueId("custom_id");
        param.setFieldNameEmbedding("custom_embedding");
        param.setFieldNamePageContent("custom_content");
        param.setFieldNameMetadata("custom_metadata");
        param.setIndexDisplayName("custom_index");
        param.setEndpointDisplayName("custom_endpoint");
        param.setNeighborsCount(20);
        
        assertEquals("custom_id", param.getFieldNameUniqueId());
        assertEquals("custom_embedding", param.getFieldNameEmbedding());
        assertEquals("custom_content", param.getFieldNamePageContent());
        assertEquals("custom_metadata", param.getFieldNameMetadata());
        assertEquals("custom_index", param.getIndexDisplayName());
        assertEquals("custom_endpoint", param.getEndpointDisplayName());
        assertEquals(20, param.getNeighborsCount());
    }

    @Test
    void testIndexInitParamDefaults() {
        VertexAiVectorSearchParam.IndexInitParam initParam = new VertexAiVectorSearchParam.IndexInitParam();
        
        assertEquals(1536, initParam.getDimensions());
        assertEquals("COSINE_DISTANCE", initParam.getDistanceMeasureType());
        assertEquals("TREE_AH", initParam.getAlgorithmConfig());
        assertEquals(500, initParam.getLeafNodeEmbeddingCount());
        assertEquals(0.1, initParam.getFractionLeafNodesToSearchPercent());
        assertTrue(initParam.isEnableAutoScaling());
        assertEquals(1, initParam.getMinReplicaCount());
        assertEquals(10, initParam.getMaxReplicaCount());
    }

    @Test
    void testIndexInitParamSettersAndGetters() {
        VertexAiVectorSearchParam.IndexInitParam initParam = new VertexAiVectorSearchParam.IndexInitParam();
        
        initParam.setDimensions(768);
        initParam.setDistanceMeasureType("DOT_PRODUCT_DISTANCE");
        initParam.setAlgorithmConfig("BRUTE_FORCE");
        initParam.setLeafNodeEmbeddingCount(1000);
        initParam.setFractionLeafNodesToSearchPercent(0.2);
        initParam.setEnableAutoScaling(false);
        initParam.setMinReplicaCount(2);
        initParam.setMaxReplicaCount(20);
        
        assertEquals(768, initParam.getDimensions());
        assertEquals("DOT_PRODUCT_DISTANCE", initParam.getDistanceMeasureType());
        assertEquals("BRUTE_FORCE", initParam.getAlgorithmConfig());
        assertEquals(1000, initParam.getLeafNodeEmbeddingCount());
        assertEquals(0.2, initParam.getFractionLeafNodesToSearchPercent());
        assertFalse(initParam.isEnableAutoScaling());
        assertEquals(2, initParam.getMinReplicaCount());
        assertEquals(20, initParam.getMaxReplicaCount());
    }
}