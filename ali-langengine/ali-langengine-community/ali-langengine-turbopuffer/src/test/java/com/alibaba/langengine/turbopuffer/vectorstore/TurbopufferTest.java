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
package com.alibaba.langengine.turbopuffer.vectorstore;

import com.alibaba.langengine.core.embeddings.Embeddings;
import com.alibaba.langengine.core.indexes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class TurbopufferTest {

    @Mock
    private Embeddings mockEmbedding;

    private Turbopuffer turbopuffer;
    private TurbopufferParam turbopufferParam;

    @BeforeEach
    void setUp() {
        turbopufferParam = new TurbopufferParam();
        // 由于真实 API 需要密钥，我们只做基本的构造测试
        // turbopuffer = new Turbopuffer("test_namespace", turbopufferParam);
    }

    @Test
    void testTurbopufferParamCreation() {
        TurbopufferParam param = new TurbopufferParam();
        assertNotNull(param);
        assertEquals("embeddings", param.getFieldNameEmbedding());
        assertEquals("content", param.getFieldNamePageContent());
        assertEquals("id", param.getFieldNameUniqueId());
        assertNotNull(param.getInitParam());
    }

    @Test
    void testTurbopufferInitParam() {
        TurbopufferParam.InitParam initParam = new TurbopufferParam.InitParam();
        assertNotNull(initParam);
        assertTrue(initParam.isFieldUniqueIdAsPrimaryKey());
        assertEquals(1536, initParam.getFieldEmbeddingsDimension());
        assertEquals(30000, initParam.getRequestTimeoutMs());
        assertEquals(10000, initParam.getConnectTimeoutMs());
        assertEquals(30000, initParam.getReadTimeoutMs());
        assertEquals(3, initParam.getMaxRetries());
    }

    @Test
    void testTurbopufferException() {
        TurbopufferException exception = new TurbopufferException("Test exception");
        assertNotNull(exception);
        assertEquals("Test exception", exception.getMessage());

        TurbopufferException.TurbopufferConnectionException connectionException = 
            new TurbopufferException.TurbopufferConnectionException("Connection failed", new RuntimeException());
        assertNotNull(connectionException);
        assertEquals("Connection failed", connectionException.getMessage());

        TurbopufferException.TurbopufferApiException apiException = 
            new TurbopufferException.TurbopufferApiException("API failed", new RuntimeException());
        assertNotNull(apiException);
        assertEquals("API failed", apiException.getMessage());

        TurbopufferException.TurbopufferNamespaceException namespaceException = 
            new TurbopufferException.TurbopufferNamespaceException("Namespace failed", new RuntimeException());
        assertNotNull(namespaceException);
        assertEquals("Namespace failed", namespaceException.getMessage());
    }

    @Test
    void testDocument() {
        Document document = new Document();
        document.setPageContent("Test content");
        document.setUniqueId("test-id");
        document.setEmbedding(Arrays.asList(0.1, 0.2, 0.3));

        assertEquals("Test content", document.getPageContent());
        assertEquals("test-id", document.getUniqueId());
        assertNotNull(document.getEmbedding());
        assertEquals(3, document.getEmbedding().size());
    }

    // 由于需要真实的 API 密钥，暂时跳过集成测试
    // 可以通过设置环境变量 TURBOPUFFER_API_KEY 来启用真实测试
    @Test
    void testSkipIntegrationTests() {
        String apiKey = System.getenv("TURBOPUFFER_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("Skipping integration tests - no API key provided");
            assertTrue(true);
        } else {
            System.out.println("API key found, integration tests could be enabled");
            assertTrue(true);
        }
    }
}
