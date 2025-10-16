package com.alibaba.langengine.docloader.wework.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.reactivex.Single;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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
public class WeWorkService {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    private static final String BASE_URL = "https://qyapi.weixin.qq.com/";
    private static final int MAX_CONNECTIONS = 20;
    private static final int KEEP_ALIVE_DURATION = 5;
    
    private static final ObjectMapper mapper = defaultObjectMapper();
    private static final AtomicInteger requestCounter = new AtomicInteger(0);
    
    // 线程安全的连接池
    private static final ConnectionPool sharedConnectionPool = 
        new ConnectionPool(MAX_CONNECTIONS, KEEP_ALIVE_DURATION, TimeUnit.MINUTES);

    @JsonIgnore
    private WeWorkApi api;

    @JsonIgnore
    private ExecutorService executorService;

    @JsonIgnore
    private OkHttpClient client;

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 构造函数
     * 
     * @param accessToken 访问令牌
     * @param timeout 超时配置
     */
    public WeWorkService(String accessToken, Duration timeout) {
        this.accessToken = accessToken;
        setClient(defaultClient(timeout));
        setExecutorService(client.dispatcher().executorService());

        Retrofit retrofit = defaultRetrofit(client, mapper);
        this.api = retrofit.create(WeWorkApi.class);
    }

    /**
     * 获取文档列表（带重试机制）
     * 
     * @param namespace 命名空间
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 文档列表结果
     */
    public WeWorkResult<List<WeWorkDocInfo>> getDocumentList(String namespace, Integer offset, Integer limit) {
        return executeWithRetry(() -> getApi().getDocumentList(accessToken, namespace, offset, limit), 
            "getDocumentList", namespace, offset, limit);
    }

    /**
     * 获取文档详情（带重试机制）
     * 
     * @param namespace 命名空间
     * @param documentId 文档ID
     * @return 文档详情结果
     */
    public WeWorkResult<WeWorkDocInfo> getDocumentDetail(String namespace, String documentId) {
        return executeWithRetry(() -> getApi().getDocumentDetail(accessToken, namespace, documentId),
            "getDocumentDetail", namespace, documentId);
    }
    
    /**
     * 批量获取文档详情
     * 
     * @param namespace 命名空间
     * @param documentIds 文档ID列表
     * @return 文档详情列表
     */
    public List<WeWorkResult<WeWorkDocInfo>> batchGetDocumentDetails(String namespace, List<String> documentIds) {
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
     * 实现指数退避重试策略
     * 
     * @param apiCallSupplier API调用供应商
     * @param operation 操作名称
     * @param params 参数
     * @return 执行结果
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
     * 
     * @param apiCall API调用
     * @return 执行结果
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
                throw new RuntimeException("WeWork API error: " + errorBody);
            } catch (IOException ex) {
                throw e;
            }
        }
    }

    /**
     * 默认ObjectMapper配置
     * 
     * @return 配置好的ObjectMapper
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
     * 
     * @param timeout 超时配置
     * @return 配置好的OkHttpClient
     */
    public OkHttpClient defaultClient(Duration timeout) {
        return new OkHttpClient.Builder()
                .connectionPool(sharedConnectionPool)
                .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .connectTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(new WeWorkAuthenticationInterceptor(accessToken))
                .addInterceptor(new WeWorkRateLimitInterceptor())
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
        log.info("WeWorkService shutdown completed");
    }

    /**
     * 默认Retrofit配置
     * 
     * @param client HTTP客户端
     * @param mapper JSON映射器
     * @return 配置好的Retrofit
     */
    public Retrofit defaultRetrofit(OkHttpClient client, ObjectMapper mapper) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }

    /**
     * 企业微信认证拦截器
     */
    private static class WeWorkAuthenticationInterceptor implements Interceptor {
        private final String accessToken;

        public WeWorkAuthenticationInterceptor(String accessToken) {
            this.accessToken = accessToken;
        }

        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            
            // 添加认证头
            Request.Builder requestBuilder = original.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .header("User-Agent", "WeWork-DocLoader/1.0");

            return chain.proceed(requestBuilder.build());
        }
    }

    /**
     * 企业微信限流拦截器
     */
    private static class WeWorkRateLimitInterceptor implements Interceptor {
        private static final long MIN_INTERVAL = 100; // 最小间隔100ms
        private static volatile long lastRequestTime = 0;

        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            synchronized (WeWorkRateLimitInterceptor.class) {
                long currentTime = System.currentTimeMillis();
                long timeSinceLastRequest = currentTime - lastRequestTime;
                
                if (timeSinceLastRequest < MIN_INTERVAL) {
                    try {
                        Thread.sleep(MIN_INTERVAL - timeSinceLastRequest);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted during rate limiting", e);
                    }
                }
                
                lastRequestTime = System.currentTimeMillis();
            }
            
            return chain.proceed(chain.request());
        }
    }
}
