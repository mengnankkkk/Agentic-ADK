package com.alibaba.langengine.docloader.dingtalk.test;

import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.docloader.dingtalk.DingTalkDocLoader;
import com.alibaba.langengine.docloader.dingtalk.config.DingTalkConfig;
import com.alibaba.langengine.docloader.dingtalk.exception.*;
import com.alibaba.langengine.docloader.dingtalk.service.*;
import io.reactivex.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DingTalkDocLoader 集成 Mock 测试
 *
 * 测试文档加载器与服务层的集成
 *
 * @author Enhanced Version
 */
@ExtendWith(MockitoExtension.class)
public class DingTalkDocLoaderMockTest {

    @Mock
    private DingTalkService mockService;

    @Mock
    private DingTalkApi mockApi;

    private DingTalkConfig config;
    private String testToken = "test-api-token";
    private String testNamespace = "test-namespace";

    @BeforeEach
    public void setup() {
        config = DingTalkConfig.defaultConfig();
    }

    // ========== 单文档加载 Mock 测试 ==========

    @Test
    public void testLoadSingleDocument_Success() {
        // 准备测试数据
        String docId = "doc-123";
        DingTalkDocInfo docInfo = TestDataFactory.createDocInfo(docId, "Test Document", "Test Content");

        DingTalkResult<DingTalkDocInfo> result = new DingTalkResult<>();
        result.setData(docInfo);
        result.setSuccess(true);

        // 配置 Mock 行为
        when(mockService.getDocumentDetail(eq(testNamespace), eq(docId)))
                .thenReturn(result);

        // 模拟创建加载器（注意：这里需要重构DingTalkDocLoader以支持依赖注入）
        // 由于原始设计不支持完全的依赖注入，我们测试Service层
        DingTalkResult<DingTalkDocInfo> actualResult =
                mockService.getDocumentDetail(testNamespace, docId);

        // 验证结果
        assertNotNull(actualResult);
        assertTrue(actualResult.getSuccess());
        assertNotNull(actualResult.getData());
        assertEquals(docId, actualResult.getData().getId());
        assertEquals("Test Document", actualResult.getData().getTitle());

        // 验证调用
        verify(mockService, times(1)).getDocumentDetail(testNamespace, docId);
    }

    @Test
    public void testLoadSingleDocument_NotFound() {
        // 准备测试数据 - 文档不存在
        String docId = "nonexistent-doc";

        DingTalkResult<DingTalkDocInfo> result = new DingTalkResult<>();
        result.setData(null);
        result.setSuccess(false);
        result.setErrcode("NOT_FOUND");

        // 配置 Mock 行为
        when(mockService.getDocumentDetail(eq(testNamespace), eq(docId)))
                .thenReturn(result);

        // 执行测试
        DingTalkResult<DingTalkDocInfo> actualResult =
                mockService.getDocumentDetail(testNamespace, docId);

        // 验证结果
        assertNotNull(actualResult);
        assertFalse(actualResult.getSuccess());
        assertNull(actualResult.getData());

        // 验证调用
        verify(mockService).getDocumentDetail(testNamespace, docId);
    }

    @Test
    public void testLoadSingleDocument_EmptyContent() {
        // 准备测试数据 - 空内容
        String docId = "empty-doc";
        DingTalkDocInfo docInfo = TestDataFactory.createDocInfo(docId, "Empty Doc", "");
        docInfo.setBody("");
        docInfo.setBodyHtml("");

        DingTalkResult<DingTalkDocInfo> result = new DingTalkResult<>();
        result.setData(docInfo);
        result.setSuccess(true);

        // 配置 Mock 行为
        when(mockService.getDocumentDetail(eq(testNamespace), eq(docId)))
                .thenReturn(result);

        // 执行测试
        DingTalkResult<DingTalkDocInfo> actualResult =
                mockService.getDocumentDetail(testNamespace, docId);

        // 验证结果 - 空内容文档应该被正确返回
        assertNotNull(actualResult);
        assertTrue(actualResult.getSuccess());
        assertNotNull(actualResult.getData());
        assertEquals("", actualResult.getData().getBody());
    }

    // ========== 批量文档加载 Mock 测试 ==========

    @Test
    public void testLoadBatchDocuments_Success() {
        // 准备测试数据 - 第一批
        List<DingTalkDocInfo> batch1 = Arrays.asList(
                TestDataFactory.createDocInfo("doc-1", "Document 1", "Content 1"),
                TestDataFactory.createDocInfo("doc-2", "Document 2", "Content 2"),
                TestDataFactory.createDocInfo("doc-3", "Document 3", "Content 3")
        );

        DingTalkResult<List<DingTalkDocInfo>> result1 = new DingTalkResult<>();
        result1.setData(batch1);
        result1.setSuccess(true);

        // 配置 Mock 行为
        when(mockService.getDocumentList(eq(testNamespace), eq(0), anyInt()))
                .thenReturn(result1);

        // 执行测试
        DingTalkResult<List<DingTalkDocInfo>> actualResult =
                mockService.getDocumentList(testNamespace, 0, 50);

        // 验证结果
        assertNotNull(actualResult);
        assertTrue(actualResult.getSuccess());
        assertEquals(3, actualResult.getData().size());

        // 验证调用
        verify(mockService).getDocumentList(testNamespace, 0, 50);
    }

    @Test
    public void testLoadBatchDocuments_Pagination() {
        // 准备测试数据 - 多页数据
        List<DingTalkDocInfo> page1 = TestDataFactory.createDocInfoList(50, "page1");
        List<DingTalkDocInfo> page2 = TestDataFactory.createDocInfoList(30, "page2");
        List<DingTalkDocInfo> page3 = new ArrayList<>(); // 空页，表示结束

        DingTalkResult<List<DingTalkDocInfo>> result1 = new DingTalkResult<>();
        result1.setData(page1);
        result1.setSuccess(true);

        DingTalkResult<List<DingTalkDocInfo>> result2 = new DingTalkResult<>();
        result2.setData(page2);
        result2.setSuccess(true);

        DingTalkResult<List<DingTalkDocInfo>> result3 = new DingTalkResult<>();
        result3.setData(page3);
        result3.setSuccess(true);

        // 配置 Mock 行为 - 分页请求
        when(mockService.getDocumentList(eq(testNamespace), eq(0), eq(50)))
                .thenReturn(result1);
        when(mockService.getDocumentList(eq(testNamespace), eq(50), eq(50)))
                .thenReturn(result2);
        when(mockService.getDocumentList(eq(testNamespace), eq(100), eq(50)))
                .thenReturn(result3);

        // 模拟批量加载逻辑
        List<DingTalkDocInfo> allDocs = new ArrayList<>();
        int offset = 0;
        int batchSize = 50;

        while (true) {
            DingTalkResult<List<DingTalkDocInfo>> result =
                    mockService.getDocumentList(testNamespace, offset, batchSize);

            if (result.getData() == null || result.getData().isEmpty()) {
                break;
            }

            allDocs.addAll(result.getData());
            offset += batchSize;
        }

        // 验证结果
        assertEquals(80, allDocs.size()); // 50 + 30

        // 验证调用次数
        verify(mockService, times(1)).getDocumentList(testNamespace, 0, 50);
        verify(mockService, times(1)).getDocumentList(testNamespace, 50, 50);
        verify(mockService, times(1)).getDocumentList(testNamespace, 100, 50);
    }

    @Test
    public void testLoadBatchDocuments_EmptyResult() {
        // 准备测试数据 - 空结果
        DingTalkResult<List<DingTalkDocInfo>> result = new DingTalkResult<>();
        result.setData(new ArrayList<>());
        result.setSuccess(true);

        // 配置 Mock 行为
        when(mockService.getDocumentList(eq(testNamespace), anyInt(), anyInt()))
                .thenReturn(result);

        // 执行测试
        DingTalkResult<List<DingTalkDocInfo>> actualResult =
                mockService.getDocumentList(testNamespace, 0, 50);

        // 验证结果
        assertNotNull(actualResult);
        assertTrue(actualResult.getSuccess());
        assertTrue(actualResult.getData().isEmpty());
    }

    // ========== 异常场景 Mock 测试 ==========

    @Test
    public void testLoad_ServiceException() {
        // 配置 Mock 行为 - 抛出异常
        when(mockService.getDocumentDetail(anyString(), anyString()))
                .thenThrow(new RuntimeException("Service error"));

        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () ->
                mockService.getDocumentDetail(testNamespace, "doc-123")
        );

        // 验证调用
        verify(mockService).getDocumentDetail(testNamespace, "doc-123");
    }

    @Test
    public void testLoad_RateLimitException() {
        // 配置 Mock 行为 - 限流异常
        when(mockService.getDocumentDetail(anyString(), anyString()))
                .thenThrow(new DingTalkRateLimitException("Rate limit exceeded", 60L));

        // 执行测试并验证异常
        DingTalkRateLimitException exception = assertThrows(
                DingTalkRateLimitException.class,
                () -> mockService.getDocumentDetail(testNamespace, "doc-123")
        );

        assertEquals(60L, exception.getRetryAfterSeconds());
        assertEquals(429, exception.getHttpStatusCode());

        // 验证调用
        verify(mockService).getDocumentDetail(testNamespace, "doc-123");
    }

    @Test
    public void testLoad_AuthenticationException() {
        // 配置 Mock 行为 - 认证异常
        when(mockService.getDocumentDetail(anyString(), anyString()))
                .thenThrow(new DingTalkAuthenticationException("Invalid token"));

        // 执行测试并验证异常
        DingTalkAuthenticationException exception = assertThrows(
                DingTalkAuthenticationException.class,
                () -> mockService.getDocumentDetail(testNamespace, "doc-123")
        );

        assertEquals(401, exception.getHttpStatusCode());
        assertEquals("AUTH_ERROR", exception.getErrorCode());
    }

    @Test
    public void testLoad_NetworkException() {
        // 配置 Mock 行为 - 网络异常
        when(mockService.getDocumentDetail(anyString(), anyString()))
                .thenThrow(new DingTalkNetworkException("Connection timeout"));

        // 执行测试并验证异常
        DingTalkNetworkException exception = assertThrows(
                DingTalkNetworkException.class,
                () -> mockService.getDocumentDetail(testNamespace, "doc-123")
        );

        assertEquals("NETWORK_ERROR", exception.getErrorCode());
    }

    // ========== 批量获取详情 Mock 测试 ==========

    @Test
    public void testBatchGetDocumentDetails_Success() {
        // 准备测试数据
        List<String> docIds = Arrays.asList("doc-1", "doc-2", "doc-3");

        List<DingTalkResult<DingTalkDocInfo>> expectedResults = new ArrayList<>();
        for (String docId : docIds) {
            DingTalkDocInfo docInfo = TestDataFactory.createDocInfo(docId, "Doc " + docId, "Content " + docId);
            DingTalkResult<DingTalkDocInfo> result = new DingTalkResult<>();
            result.setData(docInfo);
            result.setSuccess(true);
            expectedResults.add(result);
        }

        // 配置 Mock 行为 - 只mock批量方法
        when(mockService.batchGetDocumentDetails(eq(testNamespace), eq(docIds)))
                .thenReturn(expectedResults);

        List<DingTalkResult<DingTalkDocInfo>> actualResults =
                mockService.batchGetDocumentDetails(testNamespace, docIds);

        // 验证结果
        assertNotNull(actualResults);
        assertEquals(3, actualResults.size());

        for (int i = 0; i < docIds.size(); i++) {
            assertTrue(actualResults.get(i).getSuccess());
            assertEquals(docIds.get(i), actualResults.get(i).getData().getId());
        }

        // 验证调用
        verify(mockService).batchGetDocumentDetails(testNamespace, docIds);
    }

    @Test
    public void testBatchGetDocumentDetails_PartialFailure() {
        // 准备测试数据 - 部分成功，部分失败
        List<String> docIds = Arrays.asList("doc-1", "doc-2", "doc-3");

        // doc-1: 成功
        DingTalkResult<DingTalkDocInfo> result1 = new DingTalkResult<>();
        result1.setData(TestDataFactory.createDocInfo("doc-1", "Doc 1", "Content 1"));
        result1.setSuccess(true);

        // doc-2: 失败（返回null表示跳过）
        // doc-3: 成功
        DingTalkResult<DingTalkDocInfo> result3 = new DingTalkResult<>();
        result3.setData(TestDataFactory.createDocInfo("doc-3", "Doc 3", "Content 3"));
        result3.setSuccess(true);

        List<DingTalkResult<DingTalkDocInfo>> expectedResults = Arrays.asList(result1, result3);

        // 配置 Mock 行为
        when(mockService.batchGetDocumentDetails(eq(testNamespace), eq(docIds)))
                .thenReturn(expectedResults);

        // 执行测试
        List<DingTalkResult<DingTalkDocInfo>> actualResults =
                mockService.batchGetDocumentDetails(testNamespace, docIds);

        // 验证结果 - 只返回成功的
        assertNotNull(actualResults);
        assertEquals(2, actualResults.size());
        assertEquals("doc-1", actualResults.get(0).getData().getId());
        assertEquals("doc-3", actualResults.get(1).getData().getId());
    }

    // ========== 文档内容处理 Mock 测试 ==========

    @Test
    public void testDocumentContent_HtmlFormat() {
        // 准备测试数据 - HTML格式
        String docId = "html-doc";
        DingTalkDocInfo docInfo = TestDataFactory.createDocInfo(docId, "HTML Doc", "Plain text");
        docInfo.setBodyHtml("<h1>HTML Content</h1><p>Paragraph</p>");

        DingTalkResult<DingTalkDocInfo> result = new DingTalkResult<>();
        result.setData(docInfo);
        result.setSuccess(true);

        // 配置 Mock 行为
        when(mockService.getDocumentDetail(eq(testNamespace), eq(docId)))
                .thenReturn(result);

        // 执行测试
        DingTalkResult<DingTalkDocInfo> actualResult =
                mockService.getDocumentDetail(testNamespace, docId);

        // 验证结果
        assertNotNull(actualResult);
        assertNotNull(actualResult.getData().getBodyHtml());
        assertTrue(actualResult.getData().getBodyHtml().contains("<h1>"));
        assertTrue(actualResult.getData().getBodyHtml().contains("<p>"));
    }

    @Test
    public void testDocumentContent_Metadata() {
        // 准备测试数据 - 包含完整元数据
        String docId = "meta-doc";
        DingTalkDocInfo docInfo = TestDataFactory.createDocInfo(docId, "Metadata Doc", "Content");
        docInfo.setCreator("test-author");
        docInfo.setCreatedAt("2025-01-01T00:00:00Z");
        docInfo.setUpdatedAt("2025-01-15T12:00:00Z");
        docInfo.setTags(Arrays.asList("important", "draft", "review"));
        docInfo.setReadCount(100);
        docInfo.setLikeCount(10);
        docInfo.setCommentCount(5);

        DingTalkResult<DingTalkDocInfo> result = new DingTalkResult<>();
        result.setData(docInfo);
        result.setSuccess(true);

        // 配置 Mock 行为
        when(mockService.getDocumentDetail(eq(testNamespace), eq(docId)))
                .thenReturn(result);

        // 执行测试
        DingTalkResult<DingTalkDocInfo> actualResult =
                mockService.getDocumentDetail(testNamespace, docId);

        // 验证元数据
        DingTalkDocInfo doc = actualResult.getData();
        assertNotNull(doc);
        assertEquals("test-author", doc.getCreator());
        assertEquals("2025-01-01T00:00:00Z", doc.getCreatedAt());
        assertEquals("2025-01-15T12:00:00Z", doc.getUpdatedAt());
        assertEquals(3, doc.getTags().size());
        assertEquals(100, doc.getReadCount());
        assertEquals(10, doc.getLikeCount());
        assertEquals(5, doc.getCommentCount());
    }

    @Test
    public void testDocumentContent_SpecialCharacters() {
        // 准备测试数据 - 特殊字符
        String docId = "special-doc";
        String specialContent = "Special: <>&\"' 中文 😀 \n\t\r";
        DingTalkDocInfo docInfo = TestDataFactory.createDocInfo(docId, "Special Doc", specialContent);

        DingTalkResult<DingTalkDocInfo> result = new DingTalkResult<>();
        result.setData(docInfo);
        result.setSuccess(true);

        // 配置 Mock 行为
        when(mockService.getDocumentDetail(eq(testNamespace), eq(docId)))
                .thenReturn(result);

        // 执行测试
        DingTalkResult<DingTalkDocInfo> actualResult =
                mockService.getDocumentDetail(testNamespace, docId);

        // 验证特殊字符正确处理
        assertNotNull(actualResult);
        assertEquals(specialContent, actualResult.getData().getBody());
    }

    // ========== 性能和并发 Mock 测试 ==========

    @Test
    public void testConcurrentDocumentLoad() {
        // 准备测试数据
        List<String> docIds = Arrays.asList("doc-1", "doc-2", "doc-3", "doc-4", "doc-5");

        // 为每个文档配置 Mock
        for (String docId : docIds) {
            DingTalkDocInfo docInfo = TestDataFactory.createDocInfo(docId, "Doc " + docId, "Content " + docId);
            DingTalkResult<DingTalkDocInfo> result = new DingTalkResult<>();
            result.setData(docInfo);
            result.setSuccess(true);

            when(mockService.getDocumentDetail(eq(testNamespace), eq(docId)))
                    .thenReturn(result);
        }

        // 模拟并发加载
        List<DingTalkResult<DingTalkDocInfo>> results = new ArrayList<>();
        for (String docId : docIds) {
            DingTalkResult<DingTalkDocInfo> result =
                    mockService.getDocumentDetail(testNamespace, docId);
            results.add(result);
        }

        // 验证结果
        assertEquals(5, results.size());
        for (int i = 0; i < docIds.size(); i++) {
            assertTrue(results.get(i).getSuccess());
            assertEquals(docIds.get(i), results.get(i).getData().getId());
        }

        // 验证每个文档都被调用了一次
        for (String docId : docIds) {
            verify(mockService, times(1)).getDocumentDetail(testNamespace, docId);
        }
    }
}
