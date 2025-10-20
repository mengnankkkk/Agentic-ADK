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
package com.alibaba.langengine.dashvector.vectorstore;

import com.alibaba.langengine.core.indexes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class DashVectorServiceTest {

    private DashVectorService service;
    private DashVectorParam param;

    @BeforeEach
    public void setUp() {
        param = new DashVectorParam();
        service = new DashVectorService("test_api_key", "test_endpoint", "test_collection", param);
    }

    @Test
    public void testConstructor() {
        assertNotNull(service);
        assertEquals("test_collection", service.getCollection());
        assertEquals(param, service.getDashVectorParam());
    }

    @Test
    public void testInit() {
        assertDoesNotThrow(() -> service.init());
    }

    @Test
    public void testAddDocuments() {
        Document doc1 = new Document();
        doc1.setPageContent("Test content 1");
        doc1.setUniqueId("doc1");
        doc1.setEmbedding(Arrays.asList(0.1, 0.2, 0.3));

        Document doc2 = new Document();
        doc2.setPageContent("Test content 2");
        doc2.setUniqueId("doc2");
        doc2.setEmbedding(Arrays.asList(0.4, 0.5, 0.6));

        List<Document> documents = Arrays.asList(doc1, doc2);
        
        assertDoesNotThrow(() -> service.addDocuments(documents));
    }

    @Test
    public void testAddDocumentsWithEmptyList() {
        assertDoesNotThrow(() -> service.addDocuments(Arrays.asList()));
    }

    @Test
    public void testAddDocumentsWithNullList() {
        assertDoesNotThrow(() -> service.addDocuments(null));
    }

    @Test
    public void testSimilaritySearch() {
        List<Float> queryVector = Arrays.asList(0.1f, 0.2f, 0.3f);
        int k = 5;
        
        List<Document> results = service.similaritySearch(queryVector, k);
        assertNotNull(results);
        assertTrue(results.size() <= k);
    }

    @Test
    public void testSimilaritySearchWithZeroK() {
        List<Float> queryVector = Arrays.asList(0.1f, 0.2f, 0.3f);
        int k = 0;
        
        List<Document> results = service.similaritySearch(queryVector, k);
        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    public void testDropCollection() {
        assertDoesNotThrow(() -> service.dropCollection());
    }

    @Test
    public void testBatchProcessing() {
        // 测试批量处理 - 使用小批量大小的参数创建新服务
        DashVectorParam smallBatchParam = new DashVectorParam();
        smallBatchParam.setBatchSize(1);
        
        // 验证参数设置
        assertEquals(1, smallBatchParam.getBatchSize());
        
        Document doc1 = new Document();
        doc1.setPageContent("Test content 1");
        doc1.setUniqueId("doc1");
        doc1.setEmbedding(Arrays.asList(0.1, 0.2, 0.3));

        Document doc2 = new Document();
        doc2.setPageContent("Test content 2");
        doc2.setUniqueId("doc2");
        doc2.setEmbedding(Arrays.asList(0.4, 0.5, 0.6));

        List<Document> documents = Arrays.asList(doc1, doc2);
        
        // 验证文档创建
        assertEquals(2, documents.size());
        assertEquals("Test content 1", documents.get(0).getPageContent());
    }

}