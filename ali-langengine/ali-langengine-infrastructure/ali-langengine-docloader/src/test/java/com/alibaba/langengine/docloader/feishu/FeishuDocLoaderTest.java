package com.alibaba.langengine.docloader.feishu;

import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.docloader.feishu.service.FeishuDocInfo;
import com.alibaba.langengine.docloader.feishu.service.FeishuResult;
import com.alibaba.langengine.docloader.feishu.service.FeishuService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class FeishuDocLoaderTest {

    @Mock
    private FeishuService mockService;
    
    private FeishuDocLoader loader;

    @BeforeEach
    void setUp() {
        
        loader = new FeishuDocLoader.Builder()
                .appId("test_app_id")
                .appSecret("test_app_secret")
                .appToken("test_token")
                .timeout(30L)
                .batchSize(20)
                .returnHtml(false)
                .build();
        
        // 注入mock service
        loader.setService(mockService);
    }

    @Test
    void testLoadSingleDocument() {
        // 准备mock数据
        FeishuDocInfo mockDocInfo = createMockDocInfo("doc1", "Test Document", "Test content");
        FeishuResult<FeishuDocInfo> mockResult = new FeishuResult<>();
        mockResult.setData(mockDocInfo);
        
        when(mockService.getDocumentDetail(anyString(), eq("doc1")))
                .thenReturn(mockResult);
        
        // 设置单文档模式
        loader.setDocumentId("doc1");
        
        // 执行测试
        List<Document> documents = loader.load();
        
        // 验证结果
        assertEquals(1, documents.size());
        Document doc = documents.get(0);
        assertEquals("doc1", doc.getUniqueId());
        assertEquals("Test Document", doc.getMetadata().get("title"));
        assertEquals("Test content", doc.getPageContent());
        
        verify(mockService, times(1)).getDocumentDetail("test_token", "doc1");
    }

    @Test
    void testLoadBatchDocuments() {
        // 准备mock数据
        List<FeishuDocInfo> mockDocInfos = Arrays.asList(
                createMockDocInfo("doc1", "Document 1", "Content 1"),
                createMockDocInfo("doc2", "Document 2", "Content 2")
        );
        
        FeishuResult<List<FeishuDocInfo>> mockListResult = new FeishuResult<>();
        mockListResult.setData(mockDocInfos);
        
        // Mock第一次调用返回文档列表
        when(mockService.getDocumentList(anyString(), anyInt(), anyInt()))
                .thenReturn(mockListResult)
                .thenReturn(createEmptyResult()); // 第二次调用返回空列表
        
        // Mock文档详情调用
        FeishuResult<FeishuDocInfo> mockDetailResult1 = new FeishuResult<>();
        mockDetailResult1.setData(mockDocInfos.get(0));
        FeishuResult<FeishuDocInfo> mockDetailResult2 = new FeishuResult<>();
        mockDetailResult2.setData(mockDocInfos.get(1));
        
        when(mockService.getDocumentDetail(anyString(), eq("doc1")))
                .thenReturn(mockDetailResult1);
        when(mockService.getDocumentDetail(anyString(), eq("doc2")))
                .thenReturn(mockDetailResult2);
        
        // 设置批量模式
        loader.setDocumentId(null);
        loader.setBatchSize(10);
        
        // 执行测试
        List<Document> documents = loader.load();
        
        // 验证结果
        assertEquals(2, documents.size());
        assertEquals("doc1", documents.get(0).getUniqueId());
        assertEquals("doc2", documents.get(1).getUniqueId());
        
        verify(mockService, atLeast(1)).getDocumentList(anyString(), anyInt(), anyInt());
        verify(mockService, times(1)).getDocumentDetail("test_token", "doc1");
        verify(mockService, times(1)).getDocumentDetail("test_token", "doc2");
    }

    @Test
    void testBuilderPattern() {
        FeishuDocLoader customLoader = new FeishuDocLoader.Builder()
                .appId("test_app_id")
                .appSecret("test_app_secret")
                .appToken("test_token")
                .documentId("test_doc_id")
                .timeout(60L)
                .batchSize(50)
                .domain("https://custom.feishu.cn/")
                .returnHtml(true)
                .build();

        assertEquals("test_token", customLoader.getAppToken());
        assertEquals("test_doc_id", customLoader.getDocumentId());
        assertEquals(50, customLoader.getBatchSize());
        assertEquals("https://custom.feishu.cn/", customLoader.getDomain());
        assertTrue(customLoader.isReturnHtml());
    }
    
    @Test
    void testValidationFailure() {
        loader.setAppToken(null);
        
        assertThrows(IllegalStateException.class, () -> loader.load());
    }
    
    private FeishuDocInfo createMockDocInfo(String id, String title, String content) {
        FeishuDocInfo docInfo = new FeishuDocInfo();
        docInfo.setDocumentId(id);
        docInfo.setTitle(title);
        docInfo.setContent(content);
        // docInfo.setOwner("test_owner"); // 移除不兼容的类型设置
        docInfo.setCreatedAt("2024-01-01T00:00:00Z");
        docInfo.setUpdatedAt("2024-01-01T00:00:00Z");
        docInfo.setDocumentType("docx");
        return docInfo;
    }
    
    private FeishuResult<List<FeishuDocInfo>> createEmptyResult() {
        FeishuResult<List<FeishuDocInfo>> result = new FeishuResult<>();
        result.setData(java.util.Collections.emptyList());
        return result;
    }
}