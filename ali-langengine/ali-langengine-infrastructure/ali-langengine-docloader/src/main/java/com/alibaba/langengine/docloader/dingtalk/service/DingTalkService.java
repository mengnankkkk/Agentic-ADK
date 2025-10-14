package com.alibaba.langengine.docloader.dingtalk.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.reactivex.Single;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Data
public class DingTalkService {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    private static final String BASE_URL = "https://oapi.dingtalk.com/";
    private static final int MAX_CONNECTIONS = 20;
    private static final int KEEP_ALIVE_DURATION = 5;
    
    private static final ObjectMapper mapper = defaultObjectMapper();
    private static final AtomicInteger requestCounter = new AtomicInteger(0);
    
    // 线程安全的连接池
    private static final ConnectionPool sharedConnectionPool = 
        new ConnectionPool(MAX_CONNECTIONS, KEEP_ALIVE_DURATION, TimeUnit.MINUTES);

    @JsonIgnore
    private DingTalkApi api;

    @JsonIgnore
    private ExecutorService executorService;

    @JsonIgnore
    private OkHttpClient client;

    /**
     * 访问令牌
     */
    private String accessToken;

    public DingTalkService(String accessToken, Duration timeout) {
        this.accessToken = accessToken;
        setClient(defaultClient(timeout));
        setExecutorService(client.dispatcher().executorService());

        Retrofit retrofit = defaultRetrofit(client, mapper);
        this.api = retrofit.create(DingTalkApi.class);
    }

    /**
     * 获取文档列表（带重试机制）
     */
    public DingTalkResult<List<DingTalkDocInfo>> getDocumentList(String namespace, Integer offset, Integer limit) {
        return executeWithRetry(() -> getApi().getDocumentList(accessToken, namespace, offset, limit), 
            "getDocumentList", namespace, offset, limit);
    }

    /**
     * 获取文档详情（带重试机制）
     */
    public DingTalkResult<DingTalkDocInfo> getDocumentDetail(String namespace, String documentId) {
        return executeWithRetry(() -> getApi().getDocumentDetail(accessToken, namespace, documentId),
            "getDocumentDetail", namespace, documentId);
    }
    
    /**
     * 批量获取文档详情
     */
    public List<DingTalkResult<DingTalkDocInfo>> batchGetDocumentDetails(String namespace, List<String> documentIds) {
        log.info("Batch fetching {} documents", documentIds.size());
        
        return documentIds.parallelStream()
            .map(docId -> {
                try {
                    return getDocumentDetail(namespace, docId);
                } catch (Exception e) {
                    log.warn("Failed to fetch document {}: {}", docId, e.getMessage());
                    return null;
                }
            })
            .filter(result -> result != null && result.getData() != null)
            .collect(Collectors.toList());
    }

    /**
     * 执行API调用（带重试机制）
     */
    private <T> T executeWithRetry(Supplier<Single<T>> apiCallSupplier, String operation, Object... params) {
        int maxRetries = 3;
        long baseDelay = 1000; // 1秒
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                int requestId = requestCounter.incrementAndGet();
                log.debug("[{}] Executing {} (attempt {}/{}) with params: {}", 
                    requestId, operation, attempt, maxRetries, params);
                
                T result = apiCallSupplier.get().blockingGet();
                
                log.debug("[{}] {} completed successfully", requestId, operation);
                return result;
                
            } catch (Exception e) {
                log.warn("[{}] {} failed (attempt {}/{}): {}", 
                    requestCounter.get(), operation, attempt, maxRetries, e.getMessage());
                
                if (attempt == maxRetries) {
                    throw new RuntimeException(String.format("%s failed after %d attempts", operation, maxRetries), e);
                }
                
                // 指数退避
                try {
                    Thread.sleep(baseDelay * (1L << (attempt - 1)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
        
        throw new RuntimeException("Unexpected execution path");
    }
    
    /**
     * 执行API调用
     */
    public static <T> T execute(Single<T> apiCall) {
        try {
            return apiCall.blockingGet();
        } catch (HttpException e) {
            try {
                if (e.response() == null || e.response().errorBody() == null) {
                    throw e;
                }
                String errorBody = e.response().errorBody().string();
                throw new RuntimeException("DingTalk API error: " + errorBody);
            } catch (IOException ex) {
                throw e;
            }
        }
    }

    /**
     * 默认ObjectMapper配置
     */
    public static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return mapper;
    }

    /**
     * 默认HTTP客户端配置（增强版）
     */
    public OkHttpClient defaultClient(Duration timeout) {
        return new OkHttpClient.Builder()
                .connectionPool(sharedConnectionPool)
                .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .connectTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(new DingTalkAuthenticationInterceptor(accessToken))
                .addInterceptor(new DingTalkRateLimitInterceptor())
                .build();
    }
    
    /**
     * 关闭服务，释放资源
     */
    public void shutdown() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
        log.info("DingTalkService shutdown completed");
    }

    /**
     * 默认Retrofit配置
     */
    public Retrofit defaultRetrofit(OkHttpClient client, ObjectMapper mapper) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }
}