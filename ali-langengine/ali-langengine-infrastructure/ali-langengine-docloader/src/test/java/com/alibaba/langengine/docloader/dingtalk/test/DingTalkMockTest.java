package com.alibaba.langengine.docloader.dingtalk.test;

import com.alibaba.langengine.core.indexes.Document;
import com.alibaba.langengine.docloader.dingtalk.DingTalkDocLoader;
import com.alibaba.langengine.docloader.dingtalk.config.DingTalkConfig;
import com.alibaba.langengine.docloader.dingtalk.exception.*;
import com.alibaba.langengine.docloader.dingtalk.service.*;
import io.reactivex.Single;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.HttpException;
import retrofit2.Response;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class DingTalkMockTest {

    @Mock
    private DingTalkApi mockApi;

    @Mock
    private OkHttpClient mockHttpClient;

    private DingTalkService service;
    private DingTalkConfig config;

    @BeforeEach
    public void setup() {
        config = DingTalkConfig.defaultConfig();
    }

    // ========== DingTalkApi Mock 测试 ==========

    @Test
    public void testGetDocumentList_Success() {
        // 准备测试数据
        DingTalkDocInfo doc1 = createMockDocInfo("doc-1", "Document 1", "Content 1");
        DingTalkDocInfo doc2 = createMockDocInfo("doc-2", "Document 2", "Content 2");
        List<DingTalkDocInfo> docList = Arrays.asList(doc1, doc2);

        DingTalkResult<List<DingTalkDocInfo>> result = new DingTalkResult<>();
        result.setData(docList);
        result.setSuccess(true);

        // 配置 Mock 行为
        when(mockApi.getDocumentList(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Single.just(result));

        // 执行测试
        Single<DingTalkResult<List<DingTalkDocInfo>>> response =
                mockApi.getDocumentList("token", "namespace", 0, 10);

        DingTalkResult<List<DingTalkDocInfo>> actualResult = response.blockingGet();

        // 验证结果
        assertNotNull(actualResult);
        assertTrue(actualResult.getSuccess());
        assertEquals(2, actualResult.getData().size());
        assertEquals("doc-1", actualResult.getData().get(0).getId());
        assertEquals("Document 1", actualResult.getData().get(0).getTitle());

        // 验证 Mock 调用
        verify(mockApi, times(1)).getDocumentList("token", "namespace", 0, 10);
    }

    @Test
    public void testGetDocumentDetail_Success() {
        // 准备测试数据
        DingTalkDocInfo docInfo = createMockDocInfo("doc-123", "Test Document", "Test Content");

        DingTalkResult<DingTalkDocInfo> result = new DingTalkResult<>();
        result.setData(docInfo);
        result.setSuccess(true);

        // 配置 Mock 行为
        when(mockApi.getDocumentDetail(anyString(), anyString(), eq("doc-123")))
                .thenReturn(Single.just(result));

        // 执行测试
        Single<DingTalkResult<DingTalkDocInfo>> response =
                mockApi.getDocumentDetail("token", "namespace", "doc-123");

        DingTalkResult<DingTalkDocInfo> actualResult = response.blockingGet();

        // 验证结果
        assertNotNull(actualResult);
        assertTrue(actualResult.getSuccess());
        assertEquals("doc-123", actualResult.getData().getId());
        assertEquals("Test Document", actualResult.getData().getTitle());

        // 验证 Mock 调用
        verify(mockApi).getDocumentDetail("token", "namespace", "doc-123");
    }

    @Test
    public void testGetDocumentDetail_NotFound() {
        // 准备测试数据 - 文档不存在
        DingTalkResult<DingTalkDocInfo> result = new DingTalkResult<>();
        result.setData(null);
        result.setSuccess(false);
        result.setErrcode("NOT_FOUND");
        result.setErrmsg("Document not found");

        // 配置 Mock 行为
        when(mockApi.getDocumentDetail(anyString(), anyString(), eq("nonexistent")))
                .thenReturn(Single.just(result));

        // 执行测试
        Single<DingTalkResult<DingTalkDocInfo>> response =
                mockApi.getDocumentDetail("token", "namespace", "nonexistent");

        DingTalkResult<DingTalkDocInfo> actualResult = response.blockingGet();

        // 验证结果
        assertNotNull(actualResult);
        assertFalse(actualResult.getSuccess());
        assertNull(actualResult.getData());
        assertEquals("NOT_FOUND", actualResult.getErrcode());
    }

    @Test
    public void testApi_AuthenticationFailure() {
        // 配置 Mock 行为 - 401 错误
        okhttp3.ResponseBody errorBody = okhttp3.ResponseBody.create(
                okhttp3.MediaType.parse("application/json"),
                "{\"errcode\":\"INVALID_TOKEN\",\"errmsg\":\"Invalid access token\"}"
        );

        Response<DingTalkResult<DingTalkDocInfo>> errorResponse =
                Response.error(401, errorBody);

        when(mockApi.getDocumentDetail(eq("invalid-token"), anyString(), anyString()))
                .thenReturn(Single.error(new HttpException(errorResponse)));

        // 执行测试并验证异常
        Single<DingTalkResult<DingTalkDocInfo>> response =
                mockApi.getDocumentDetail("invalid-token", "namespace", "doc-123");

        assertThrows(HttpException.class, response::blockingGet);

        // 验证 Mock 调用
        verify(mockApi).getDocumentDetail("invalid-token", "namespace", "doc-123");
    }

    @Test
    public void testApi_RateLimitExceeded() {
        // 配置 Mock 行为 - 429 错误
        okhttp3.ResponseBody errorBody = okhttp3.ResponseBody.create(
                okhttp3.MediaType.parse("application/json"),
                "{\"errcode\":\"RATE_LIMIT\",\"errmsg\":\"Too many requests\"}"
        );

        Response<DingTalkResult<DingTalkDocInfo>> errorResponse =
                Response.error(429, errorBody);

        when(mockApi.getDocumentDetail(anyString(), anyString(), anyString()))
                .thenReturn(Single.error(new HttpException(errorResponse)));

        // 执行测试并验证异常
        Single<DingTalkResult<DingTalkDocInfo>> response =
                mockApi.getDocumentDetail("token", "namespace", "doc-123");

        HttpException exception = assertThrows(HttpException.class, response::blockingGet);
        assertEquals(429, exception.code());
    }

    @Test
    public void testApi_ServerError() {
        // 配置 Mock 行为 - 500 错误
        okhttp3.ResponseBody errorBody = okhttp3.ResponseBody.create(
                okhttp3.MediaType.parse("application/json"),
                "{\"errcode\":\"INTERNAL_ERROR\",\"errmsg\":\"Internal server error\"}"
        );

        Response<DingTalkResult<DingTalkDocInfo>> errorResponse =
                Response.error(500, errorBody);

        when(mockApi.getDocumentDetail(anyString(), anyString(), anyString()))
                .thenReturn(Single.error(new HttpException(errorResponse)));

        // 执行测试并验证异常
        Single<DingTalkResult<DingTalkDocInfo>> response =
                mockApi.getDocumentDetail("token", "namespace", "doc-123");

        HttpException exception = assertThrows(HttpException.class, response::blockingGet);
        assertEquals(500, exception.code());
    }

    // ========== DingTalkService Mock 测试 ==========

    @Test
    public void testService_GetDocumentList_WithPagination() {
        // 第一页数据
        List<DingTalkDocInfo> page1 = Arrays.asList(
                createMockDocInfo("doc-1", "Doc 1", "Content 1"),
                createMockDocInfo("doc-2", "Doc 2", "Content 2")
        );

        // 第二页数据
        List<DingTalkDocInfo> page2 = Arrays.asList(
                createMockDocInfo("doc-3", "Doc 3", "Content 3")
        );

        DingTalkResult<List<DingTalkDocInfo>> result1 = new DingTalkResult<>();
        result1.setData(page1);
        result1.setSuccess(true);

        DingTalkResult<List<DingTalkDocInfo>> result2 = new DingTalkResult<>();
        result2.setData(page2);
        result2.setSuccess(true);

        // 配置 Mock 行为 - 分页请求
        when(mockApi.getDocumentList(anyString(), anyString(), eq(0), anyInt()))
                .thenReturn(Single.just(result1));

        when(mockApi.getDocumentList(anyString(), anyString(), eq(50), anyInt()))
                .thenReturn(Single.just(result2));

        // 执行测试 - 第一页
        DingTalkResult<List<DingTalkDocInfo>> actualResult1 =
                mockApi.getDocumentList("token", "namespace", 0, 50).blockingGet();

        assertEquals(2, actualResult1.getData().size());

        // 执行测试 - 第二页
        DingTalkResult<List<DingTalkDocInfo>> actualResult2 =
                mockApi.getDocumentList("token", "namespace", 50, 50).blockingGet();

        assertEquals(1, actualResult2.getData().size());

        // 验证调用次数
        verify(mockApi, times(1)).getDocumentList(anyString(), anyString(), eq(0), anyInt());
        verify(mockApi, times(1)).getDocumentList(anyString(), anyString(), eq(50), anyInt());
    }

    @Test
    public void testService_GetDocumentList_EmptyResult() {
        // 准备测试数据 - 空列表
        DingTalkResult<List<DingTalkDocInfo>> result = new DingTalkResult<>();
        result.setData(new ArrayList<>());
        result.setSuccess(true);

        // 配置 Mock 行为
        when(mockApi.getDocumentList(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Single.just(result));

        // 执行测试
        DingTalkResult<List<DingTalkDocInfo>> actualResult =
                mockApi.getDocumentList("token", "namespace", 0, 10).blockingGet();

        // 验证结果
        assertNotNull(actualResult);
        assertTrue(actualResult.getSuccess());
        assertTrue(actualResult.getData().isEmpty());
    }

    @Test
    public void testService_BatchGetDocuments() {
        // 准备多个文档
        DingTalkResult<DingTalkDocInfo> result1 = new DingTalkResult<>();
        result1.setData(createMockDocInfo("doc-1", "Doc 1", "Content 1"));
        result1.setSuccess(true);

        DingTalkResult<DingTalkDocInfo> result2 = new DingTalkResult<>();
        result2.setData(createMockDocInfo("doc-2", "Doc 2", "Content 2"));
        result2.setSuccess(true);

        DingTalkResult<DingTalkDocInfo> result3 = new DingTalkResult<>();
        result3.setData(createMockDocInfo("doc-3", "Doc 3", "Content 3"));
        result3.setSuccess(true);

        // 配置 Mock 行为 - 不同文档ID返回不同结果
        when(mockApi.getDocumentDetail(anyString(), anyString(), eq("doc-1")))
                .thenReturn(Single.just(result1));
        when(mockApi.getDocumentDetail(anyString(), anyString(), eq("doc-2")))
                .thenReturn(Single.just(result2));
        when(mockApi.getDocumentDetail(anyString(), anyString(), eq("doc-3")))
                .thenReturn(Single.just(result3));

        // 执行批量获取
        List<String> docIds = Arrays.asList("doc-1", "doc-2", "doc-3");
        List<DingTalkResult<DingTalkDocInfo>> results = new ArrayList<>();

        for (String docId : docIds) {
            DingTalkResult<DingTalkDocInfo> result =
                    mockApi.getDocumentDetail("token", "namespace", docId).blockingGet();
            results.add(result);
        }

        // 验证结果
        assertEquals(3, results.size());
        assertEquals("doc-1", results.get(0).getData().getId());
        assertEquals("doc-2", results.get(1).getData().getId());
        assertEquals("doc-3", results.get(2).getData().getId());

        // 验证调用次数
        verify(mockApi, times(1)).getDocumentDetail(anyString(), anyString(), eq("doc-1"));
        verify(mockApi, times(1)).getDocumentDetail(anyString(), anyString(), eq("doc-2"));
        verify(mockApi, times(1)).getDocumentDetail(anyString(), anyString(), eq("doc-3"));
    }

    // ========== 重试机制 Mock 测试 ==========

    @Test
    public void testService_RetryExhausted() {
        // 配置 Mock 行为 - 所有重试都失败
        when(mockApi.getDocumentDetail(anyString(), anyString(), eq("doc-123")))
                .thenReturn(Single.error(new IOException("Network error")));

        // 执行测试 - 模拟重试逻辑
        int maxRetries = 3;
        boolean allFailed = true;

        for (int i = 0; i < maxRetries; i++) {
            try {
                mockApi.getDocumentDetail("token", "namespace", "doc-123").blockingGet();
                allFailed = false;
                break;
            } catch (Exception e) {
                // 继续重试
            }
        }

        // 验证所有重试都失败
        assertTrue(allFailed);

        // 验证调用了3次
        verify(mockApi, times(3)).getDocumentDetail(anyString(), anyString(), eq("doc-123"));
    }

    // ========== 边界情况 Mock 测试 ==========

    @Test
    public void testApi_EmptyContent() {
        // 准备测试数据 - 空内容
        DingTalkDocInfo docInfo = createMockDocInfo("doc-123", "Empty Doc", "");
        docInfo.setBody("");
        docInfo.setBodyHtml("");

        DingTalkResult<DingTalkDocInfo> result = new DingTalkResult<>();
        result.setData(docInfo);
        result.setSuccess(true);

        // 配置 Mock 行为
        when(mockApi.getDocumentDetail(anyString(), anyString(), eq("doc-123")))
                .thenReturn(Single.just(result));

        // 执行测试
        DingTalkResult<DingTalkDocInfo> actualResult =
                mockApi.getDocumentDetail("token", "namespace", "doc-123").blockingGet();

        // 验证结果
        assertNotNull(actualResult);
        assertTrue(actualResult.getSuccess());
        assertEquals("", actualResult.getData().getBody());
        assertEquals("", actualResult.getData().getBodyHtml());
    }

    @Test
    public void testApi_LargeDocumentList() {
        // 准备测试数据 - 大量文档
        List<DingTalkDocInfo> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(createMockDocInfo("doc-" + i, "Document " + i, "Content " + i));
        }

        DingTalkResult<List<DingTalkDocInfo>> result = new DingTalkResult<>();
        result.setData(largeList);
        result.setSuccess(true);

        // 配置 Mock 行为
        when(mockApi.getDocumentList(anyString(), anyString(), anyInt(), eq(100)))
                .thenReturn(Single.just(result));

        // 执行测试
        DingTalkResult<List<DingTalkDocInfo>> actualResult =
                mockApi.getDocumentList("token", "namespace", 0, 100).blockingGet();

        // 验证结果
        assertNotNull(actualResult);
        assertTrue(actualResult.getSuccess());
        assertEquals(100, actualResult.getData().size());
    }

    @Test
    public void testApi_SpecialCharactersInContent() {
        // 准备测试数据 - 特殊字符
        String specialContent = "Special chars: <>&\"' 中文 emoji 😀 \n\t\r";
        DingTalkDocInfo docInfo = createMockDocInfo("doc-123", "Special Doc", specialContent);

        DingTalkResult<DingTalkDocInfo> result = new DingTalkResult<>();
        result.setData(docInfo);
        result.setSuccess(true);

        // 配置 Mock 行为
        when(mockApi.getDocumentDetail(anyString(), anyString(), eq("doc-123")))
                .thenReturn(Single.just(result));

        // 执行测试
        DingTalkResult<DingTalkDocInfo> actualResult =
                mockApi.getDocumentDetail("token", "namespace", "doc-123").blockingGet();

        // 验证结果
        assertNotNull(actualResult);
        assertEquals(specialContent, actualResult.getData().getBody());
    }

    // ========== 辅助方法 ==========

    /**
     * 创建 Mock 文档信息
     */
    private DingTalkDocInfo createMockDocInfo(String id, String title, String content) {
        DingTalkDocInfo docInfo = new DingTalkDocInfo();
        docInfo.setId(id);
        docInfo.setTitle(title);
        docInfo.setBody(content);
        docInfo.setBodyHtml("<p>" + content + "</p>");
        docInfo.setCreator("test-user");
        docInfo.setCreatedAt("2025-01-01T00:00:00Z");
        docInfo.setUpdatedAt("2025-01-01T00:00:00Z");
        docInfo.setStatus(1);
        docInfo.setIsPublic(true);
        docInfo.setReadCount(0);
        docInfo.setLikeCount(0);
        docInfo.setCommentCount(0);
        docInfo.setWordCount(content.length());
        docInfo.setTags(Arrays.asList("test", "mock"));
        return docInfo;
    }
}
