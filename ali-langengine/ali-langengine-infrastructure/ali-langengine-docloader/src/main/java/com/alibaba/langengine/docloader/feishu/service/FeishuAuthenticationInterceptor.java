package com.alibaba.langengine.docloader.feishu.service;

import com.alibaba.langengine.docloader.feishu.config.FeishuConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;


@Slf4j
public class FeishuAuthenticationInterceptor implements Interceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final String appId;
    private final String appSecret;
    private final ConcurrentHashMap<String, String> tokenCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> tokenExpireTime = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock tokenLock = new ReentrantReadWriteLock();
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FeishuAuthenticationInterceptor(String appId, String appSecret) {
        if (appId == null || appId.trim().isEmpty()) {
            throw new IllegalArgumentException("appId cannot be null or empty");
        }
        if (appSecret == null || appSecret.trim().isEmpty()) {
            throw new IllegalArgumentException("appSecret cannot be null or empty");
        }
        
        this.appId = appId;
        this.appSecret = appSecret;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // 如果是获取token的请求，直接执行
        if (originalRequest.url().toString().contains("tenant_access_token")) {
            return chain.proceed(originalRequest);
        }

        // 获取访问令牌
        String accessToken = getAccessToken();

        // 添加认证头
        Request authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", BEARER_PREFIX + accessToken)
                .build();

        return chain.proceed(authenticatedRequest);
    }

    /**
     * 获取访问令牌
     */
    private String getAccessToken() throws IOException {
        String cacheKey = "tenant_access_token";

        // 使用读锁检查缓存
        tokenLock.readLock().lock();
        try {
            if (isTokenValid(cacheKey)) {
                return tokenCache.get(cacheKey);
            }
        } finally {
            tokenLock.readLock().unlock();
        }

        // 使用写锁获取新令牌
        tokenLock.writeLock().lock();
        try {
            // 双重检查
            if (isTokenValid(cacheKey)) {
                return tokenCache.get(cacheKey);
            }

            return fetchNewToken(cacheKey);
        } finally {
            tokenLock.writeLock().unlock();
        }
    }
    
    /**
     * 获取新令牌
     */
    private String fetchNewToken(String cacheKey) throws IOException {
        try {
            // 构建请求体
            String requestJson = objectMapper.writeValueAsString(
                java.util.Map.of("app_id", appId, "app_secret", appSecret)
            );

            RequestBody body = RequestBody.create(
                requestJson,
                MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(FeishuConfig.TOKEN_URL)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorMsg = String.format("HTTP %d: %s", response.code(), response.message());
                    log.error("Failed to get access token: {}", errorMsg);
                    throw new IOException("Failed to get access token: " + errorMsg);
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new IOException("Empty response body");
                }

                String responseStr = responseBody.string();
                JsonNode responseJson = objectMapper.readTree(responseStr);

                int code = responseJson.path("code").asInt(-1);
                if (code != 0) {
                    String msg = responseJson.path("msg").asText("Unknown error");
                    log.error("API error getting access token: code={}, msg={}", code, msg);
                    throw new IOException("Failed to get access token: " + msg);
                }

                // 提取令牌
                String token = responseJson.path("tenant_access_token").asText();
                int expire = responseJson.path("expire").asInt(7200);

                if (token == null || token.isEmpty()) {
                    throw new IOException("Empty token received from API");
                }

                // 缓存令牌
                long expireTime = System.currentTimeMillis() + (expire - FeishuConfig.TOKEN_REFRESH_ADVANCE_TIME) * 1000L;
                tokenCache.put(cacheKey, token);
                tokenExpireTime.put(cacheKey, expireTime);

                log.debug("Successfully obtained new access token, expires in {} seconds", expire);
                return token;
            }
        } catch (Exception e) {
            log.error("Error fetching new token: {}", e.getMessage(), e);
            throw new IOException("Failed to fetch access token", e);
        }
    }

    /**
     * 检查令牌是否有效
     */
    private boolean isTokenValid(String cacheKey) {
        String token = tokenCache.get(cacheKey);
        Long expireTime = tokenExpireTime.get(cacheKey);

        boolean valid = token != null && !token.trim().isEmpty() && 
                       expireTime != null && System.currentTimeMillis() < expireTime;
        
        if (!valid && token != null) {
            log.debug("Token expired or invalid, will refresh");
        }
        
        return valid;
    }
}