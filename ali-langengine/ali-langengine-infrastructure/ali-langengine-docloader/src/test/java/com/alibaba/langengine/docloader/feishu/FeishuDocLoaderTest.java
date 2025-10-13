package com.alibaba.langengine.docloader.feishu;

import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.docloader.feishu.service.FeishuDocInfo;
import com.alibaba.langengine.docloader.feishu.service.FeishuResult;
import com.alibaba.langengine.docloader.feishu.service.FeishuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FeishuDocLoaderTest {

    @Mock
    private FeishuService mockService;

    private FeishuDocLoader loader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        loader = new FeishuDocLoader("test_token");
        loader.setService(mockService);
    }

    @Test
    void testLoadSingleDocument() {
        // Arrange
        String docId = "test_doc_id";
        FeishuDocInfo docInfo = createMockDocInfo(docId, "Test Title", "Test content");
        FeishuResult<FeishuDocInfo> result = new FeishuResult<>();
        result.setCode(0);
        result.setData(docInfo);

        when(mockService.getDocumentDetail(anyString(), eq(docId))).thenReturn(result);

        loader.setDocumentId(docId);

        // Act
        List<Document> documents = loader.load();

        // Assert
        assertEquals(1, documents.size());
        assertEquals(docId, documents.get(0).getUniqueId());
        assertEquals("Test content", documents.get(0).getPageContent());
        verify(mockService).getDocumentDetail("test_token", docId);
    }

    @Test
    void testLoadBatchDocuments() {
        // Arrange
        List<FeishuDocInfo> docInfos = Arrays.asList(
            createMockDocInfo("doc1", "Title 1", "Content 1"),
            createMockDocInfo("doc2", "Title 2", "Content 2")
        );

        FeishuResult<List<FeishuDocInfo>> listResult = new FeishuResult<>();
        listResult.setCode(0);
        listResult.setData(docInfos);

        FeishuResult<FeishuDocInfo> detailResult1 = new FeishuResult<>();
        detailResult1.setCode(0);
        detailResult1.setData(docInfos.get(0));

        FeishuResult<FeishuDocInfo> detailResult2 = new FeishuResult<>();
        detailResult2.setCode(0);
        detailResult2.setData(docInfos.get(1));

        when(mockService.getDocumentList(anyString(), anyInt(), anyInt()))
            .thenReturn(listResult)
            .thenReturn(new FeishuResult<>());
        when(mockService.getDocumentDetail(anyString(), eq("doc1"))).thenReturn(detailResult1);
        when(mockService.getDocumentDetail(anyString(), eq("doc2"))).thenReturn(detailResult2);

        loader.setDocumentId(null);

        // Act
        List<Document> documents = loader.load();

        // Assert
        assertEquals(2, documents.size());
        verify(mockService, atLeastOnce()).getDocumentList(anyString(), anyInt(), anyInt());
    }

    @Test
    void testLoadEmptyResult() {
        // Arrange
        when(mockService.getDocumentDetail(anyString(), anyString())).thenReturn(new FeishuResult<>());
        loader.setDocumentId("empty_doc");

        // Act
        List<Document> documents = loader.load();

        // Assert
        assertTrue(documents.isEmpty());
    }

    @Test
    void testBuilderPattern() {
        // Act
        FeishuDocLoader builtLoader = new FeishuDocLoader.Builder()
            .appToken("token")
            .batchSize(10)
            .returnHtml(true)
            .build();

        // Assert
        assertEquals("token", builtLoader.getAppToken());
        assertEquals(10, builtLoader.getBatchSize());
        assertTrue(builtLoader.isReturnHtml());
    }

    private FeishuDocInfo createMockDocInfo(String docId, String title, String content) {
        FeishuDocInfo docInfo = new FeishuDocInfo();
        docInfo.setDocumentId(docId);
        docInfo.setTitle(title);
        docInfo.setContent(content);
        docInfo.setCreatedAt("2024-01-01T00:00:00Z");
        docInfo.setUpdatedAt("2024-01-01T00:00:00Z");
        docInfo.setDocumentType("doc");
        return docInfo;
    }
}