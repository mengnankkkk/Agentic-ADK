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

import com.alibaba.langengine.core.embeddings.FakeEmbeddings;
import com.alibaba.langengine.core.indexes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class DashVectorTest {

    private DashVector dashVector;
    private FakeEmbeddings fakeEmbeddings;

    @BeforeEach
    public void setUp() {
        fakeEmbeddings = new FakeEmbeddings();
        
        // 创建DashVector实例
        dashVector = new DashVector("test_collection");
        dashVector.setEmbedding(fakeEmbeddings);
    }

    @Test
    public void testConstructor() {
        DashVector vector = new DashVector("test_collection");
        assertNotNull(vector);
        assertEquals("test_collection", vector.getCollection());
    }

    @Test
    public void testConstructorWithParam() {
        DashVectorParam param = new DashVectorParam();
        param.setDimension(768);
        param.setMetric("euclidean");
        
        DashVector vector = new DashVector("test_collection", param);
        assertNotNull(vector);
        assertEquals("test_collection", vector.getCollection());
    }

    @Test
    public void testAddDocuments() {
        Document doc1 = new Document();
        doc1.setPageContent("Hello world");
        doc1.setUniqueId("doc1");

        Document doc2 = new Document();
        doc2.setPageContent("Test document");
        doc2.setUniqueId("doc2");

        List<Document> documents = Arrays.asList(doc1, doc2);
        
        // 测试添加文档不抛出异常
        assertDoesNotThrow(() -> dashVector.addDocuments(documents));
    }

    @Test
    public void testAddDocumentsWithEmptyList() {
        // 测试空列表不抛出异常
        assertDoesNotThrow(() -> dashVector.addDocuments(Arrays.asList()));
    }

    @Test
    public void testAddDocumentsWithNullList() {
        // 测试null列表不抛出异常
        assertDoesNotThrow(() -> dashVector.addDocuments(null));
    }

    @Test
    public void testSimilaritySearch() {
        String query = "test query";
        int k = 5;
        
        List<Document> results = dashVector.similaritySearch(query, k);
        assertNotNull(results);
    }

    @Test
    public void testSimilaritySearchWithMaxDistance() {
        String query = "test query";
        int k = 5;
        Double maxDistance = 0.8;
        
        List<Document> results = dashVector.similaritySearch(query, k, maxDistance);
        assertNotNull(results);
    }

    @Test
    public void testSimilaritySearchWithType() {
        String query = "test query";
        int k = 5;
        Integer type = 1;
        
        List<Document> results = dashVector.similaritySearch(query, k, type);
        assertNotNull(results);
    }

    @Test
    public void testSimilaritySearchWithAllParams() {
        String query = "test query";
        int k = 5;
        Double maxDistance = 0.8;
        Integer type = 1;
        
        List<Document> results = dashVector.similaritySearch(query, k, maxDistance, type);
        assertNotNull(results);
    }

    @Test
    public void testInit() {
        // 测试初始化不抛出异常
        assertDoesNotThrow(() -> dashVector.init());
    }

    @Test
    public void testGetDashVectorService() {
        DashVectorService service = dashVector.getDashVectorService();
        assertNotNull(service);
    }

    @Test
    public void testSetEmbedding() {
        FakeEmbeddings newEmbedding = new FakeEmbeddings();
        dashVector.setEmbedding(newEmbedding);
        assertEquals(newEmbedding, dashVector.getEmbedding());
    }

}