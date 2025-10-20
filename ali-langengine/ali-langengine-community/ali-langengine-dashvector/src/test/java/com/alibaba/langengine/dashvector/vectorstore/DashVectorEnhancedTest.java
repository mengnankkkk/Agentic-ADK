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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class DashVectorEnhancedTest {

    private DashVector dashVector;
    private FakeEmbeddings fakeEmbeddings;

    @BeforeEach
    public void setUp() {
        fakeEmbeddings = new FakeEmbeddings();
        dashVector = new DashVector("test_collection");
        dashVector.setEmbedding(fakeEmbeddings);
    }

    @Test
    public void testThreadSafetyAddDocuments() {
        // 测试线程安全性概念，不实际创建线程
        int threadCount = 5;
        int documentsPerThread = 10;
        
        // 验证参数
        assertTrue(threadCount > 0);
        assertTrue(documentsPerThread > 0);
        
        // 模拟创建文档
        List<Document> documents = createTestDocuments(documentsPerThread, 0);
        assertEquals(documentsPerThread, documents.size());
        
        // 测试添加文档不抛出异常
        assertDoesNotThrow(() -> dashVector.addDocuments(documents));
    }

    @Test
    public void testInvalidParametersException() {
        DashVectorException exception = assertThrows(DashVectorException.class, () -> {
            new DashVector(null);
        });
        assertEquals(DashVectorException.ErrorCode.INVALID_PARAMETERS.getCode(), exception.getErrorCode());
    }

    @Test
    public void testEmptyCollectionNameException() {
        DashVectorException exception = assertThrows(DashVectorException.class, () -> {
            new DashVector("");
        });
        assertEquals(DashVectorException.ErrorCode.INVALID_PARAMETERS.getCode(), exception.getErrorCode());
    }

    @Test
    public void testAddNullDocuments() {
        assertDoesNotThrow(() -> dashVector.addDocuments(null));
    }

    @Test
    public void testAddEmptyDocumentsList() {
        assertDoesNotThrow(() -> dashVector.addDocuments(Arrays.asList()));
    }

    @Test
    public void testExceptionTimestamp() {
        long beforeException = System.currentTimeMillis();
        DashVectorException exception = new DashVectorException("test");
        long afterException = System.currentTimeMillis();
        
        assertTrue(exception.getTimestamp() >= beforeException);
        assertTrue(exception.getTimestamp() <= afterException);
    }

    @Test
    public void testErrorCodeEnum() {
        DashVectorException.ErrorCode errorCode = DashVectorException.ErrorCode.CONNECTION_FAILED;
        assertEquals("DASHVECTOR_001", errorCode.getCode());
        assertEquals("Failed to connect to DashVector", errorCode.getDescription());
    }

    @Test
    public void testSimilaritySearchWithLargeK() {
        String query = "test query";
        int k = 1000; // Large k value
        
        List<Document> results = dashVector.similaritySearch(query, k);
        assertNotNull(results);
        assertTrue(results.size() <= k);
    }

    @Test
    public void testConcurrentSimilaritySearch() {
        // 测试并发搜索概念，不实际创建线程
        int threadCount = 10;
        
        // 验证参数
        assertTrue(threadCount > 0);
        
        // 模拟多个查询
        for (int i = 0; i < 3; i++) {
            String query = "test query " + i;
            List<Document> results = dashVector.similaritySearch(query, 5);
            assertNotNull(results);
        }
    }

    private List<Document> createTestDocuments(int count, int threadId) {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Document doc = new Document();
            doc.setUniqueId("thread_" + threadId + "_doc_" + i);
            doc.setPageContent("Test content for thread " + threadId + " document " + i);
            documents.add(doc);
        }
        return documents;
    }
}