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

import com.alibaba.langengine.core.embeddings.FakeEmbeddings;
import com.alibaba.langengine.core.indexes.Document;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
public class InfinityTest {

    @Mock
    private InfinityService mockInfinityService;

    @Mock
    private InfinityClient mockInfinityClient;

    private Infinity infinity;
    private FakeEmbeddings fakeEmbeddings;

    @BeforeEach
    public void setUp() {
        fakeEmbeddings = new FakeEmbeddings();
        
        // 创建 Infinity 实例用于测试
        infinity = new Infinity("test_table", "test_db");
        infinity.setEmbedding(fakeEmbeddings);
    }

    @Test
    public void testConstructorWithTableName() {
        Infinity vectorStore = new Infinity("test_table");
        assertEquals("test_table", vectorStore.getTableName());
        assertEquals("default", vectorStore.getDatabaseName());
        assertNotNull(vectorStore.getInfinityService());
    }

    @Test
    public void testConstructorWithTableNameAndDatabase() {
        Infinity vectorStore = new Infinity("test_table", "custom_db");
        assertEquals("test_table", vectorStore.getTableName());
        assertEquals("custom_db", vectorStore.getDatabaseName());
        assertNotNull(vectorStore.getInfinityService());
    }

    @Test
    public void testConstructorWithAllParameters() {
        InfinityParam param = new InfinityParam();
        param.getInitParam().setFieldEmbeddingsDimension(768);
        
        Infinity vectorStore = new Infinity("test_table", "custom_db", param);
        assertEquals("test_table", vectorStore.getTableName());
        assertEquals("custom_db", vectorStore.getDatabaseName());
        assertNotNull(vectorStore.getInfinityService());
    }





    @Test
    public void testAddDocumentsWithEmptyList() {
        Infinity testInfinity = createTestInfinity();
        
        List<Document> emptyDocuments = Lists.newArrayList();
        
        // 测试空文档列表不抛出异常
        assertDoesNotThrow(() -> testInfinity.addDocuments(emptyDocuments));
    }

    @Test
    public void testAddDocumentsWithNullList() {
        Infinity testInfinity = createTestInfinity();
        
        // 测试 null 文档列表不抛出异常
        assertDoesNotThrow(() -> testInfinity.addDocuments(null));
    }

    @Test
    public void testSimilaritySearch() {
        Infinity testInfinity = createTestInfinity();
        
        String query = "test query";
        int k = 5;
        
        List<Document> results = testInfinity.similaritySearch(query, k);
        
        // 由于是 mock 测试，我们主要验证没有异常
        assertNotNull(results);
    }

    @Test
    public void testSimilaritySearchWithEmptyQuery() {
        Infinity testInfinity = createTestInfinity();
        
        String emptyQuery = "";
        int k = 5;
        
        List<Document> results = testInfinity.similaritySearch(emptyQuery, k);
        
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testSimilaritySearchWithNullQuery() {
        Infinity testInfinity = createTestInfinity();
        
        String nullQuery = null;
        int k = 5;
        
        List<Document> results = testInfinity.similaritySearch(nullQuery, k);
        
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testSimilaritySearchWithMaxDistance() {
        Infinity testInfinity = createTestInfinity();
        
        String query = "test query";
        int k = 5;
        Double maxDistance = 0.8;
        
        List<Document> results = testInfinity.similaritySearch(query, k, maxDistance);
        
        assertNotNull(results);
    }



    @Test
    public void testSimilaritySearchByVectorWithEmptyVector() {
        Infinity testInfinity = createTestInfinity();
        
        List<Double> emptyVector = Lists.newArrayList();
        int k = 5;
        
        List<Document> results = testInfinity.similaritySearchByVector(emptyVector, k, null);
        
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }



    @Test
    public void testDeleteWithEmptyList() {
        Infinity testInfinity = createTestInfinity();
        
        List<String> emptyIds = Lists.newArrayList();
        
        // 测试空ID列表不抛出异常
        assertDoesNotThrow(() -> testInfinity.delete(emptyIds));
    }

    @Test
    public void testDeleteWithNullList() {
        Infinity testInfinity = createTestInfinity();
        
        // 测试 null ID列表不抛出异常
        assertDoesNotThrow(() -> testInfinity.delete(null));
    }

    @Test
    public void testClose() {
        Infinity testInfinity = createTestInfinity();
        
        // 测试关闭不抛出异常
        assertDoesNotThrow(() -> testInfinity.close());
    }

    @Test
    public void testAddTexts() {
        Infinity testInfinity = createTestInfinity();
        
        List<String> texts = Arrays.asList("Hello world", "Test document", "Another text");
        
        // 测试添加文本不抛出异常
        assertDoesNotThrow(() -> testInfinity.addTexts(texts));
    }

    @Test
    public void testGetters() {
        Infinity testInfinity = new Infinity("test_table", "test_db");
        
        assertEquals("test_table", testInfinity.getTableName());
        assertEquals("test_db", testInfinity.getDatabaseName());
        assertNotNull(testInfinity.getInfinityService());
    }

    @Test
    public void testEmbeddingSetterAndGetter() {
        Infinity testInfinity = new Infinity("test_table");
        
        assertNull(testInfinity.getEmbedding());
        
        testInfinity.setEmbedding(fakeEmbeddings);
        assertEquals(fakeEmbeddings, testInfinity.getEmbedding());
    }

    /**
     * 创建测试用的 Infinity 实例
     * 由于需要真实的网络连接，这里我们创建一个简化的测试实例
     */
    private Infinity createTestInfinity() {
        // 使用系统属性设置测试环境
        System.setProperty("infinity.server.url", "127.0.0.1");
        System.setProperty("infinity.server.port", "23817");
        
        Infinity testInfinity = new Infinity("test_table", "test_db");
        testInfinity.setEmbedding(fakeEmbeddings);
        return testInfinity;
    }


}
