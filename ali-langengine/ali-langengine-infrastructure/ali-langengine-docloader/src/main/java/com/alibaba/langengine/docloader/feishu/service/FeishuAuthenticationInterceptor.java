package com.alibaba.langengine.docloader.feishu.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;


public class FeishuAuthenticationInterceptor implements Interceptor {

    private static final String TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal/";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final long TOKEN_REFRESH_ADVANCE_TIME = 300; // 提前5分钟刷新

    private final String appId;
    private final String appSecret;
    private final ConcurrentHashMap<String, String> tokenCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> tokenExpireTime = new ConcurrentHashMap<>();
    private final ReentrantLock tokenLock = new ReentrantLock();
    private final OkHttpClient httpClient;

    public FeishuAuthenticationInterceptor(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.httpClient = new OkHttpClient.Builder().build();
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

        // 检查缓存中的令牌是否有效
        if (isTokenValid(cacheKey)) {
            return tokenCache.get(cacheKey);
        }

        tokenLock.lock();
        try {
            // 双重检查
            if (isTokenValid(cacheKey)) {
                return tokenCache.get(cacheKey);
            }

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("app_id", appId);
            requestBody.put("app_secret", appSecret);

            RequestBody body = RequestBody.create(
                    requestBody.toJSONString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(TOKEN_URL)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to get access token: " + response);
                }

                String responseBody = response.body().string();
                JSONObject responseJson = JSON.parseObject(responseBody);

                int code = responseJson.getIntValue("code");
                if (code != 0) {
                    String msg = responseJson.getString("msg");
                    throw new IOException("Failed to get access token: " + msg);
                }

                // 提取令牌
                String token = responseJson.getString("tenant_access_token");
                int expire = responseJson.getIntValue("expire");

                // 缓存令牌
                tokenCache.put(cacheKey, token);
                tokenExpireTime.put(cacheKey, System.currentTimeMillis() + (expire - TOKEN_REFRESH_ADVANCE_TIME) * 1000L);

                return token;
            }

        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * 检查令牌是否有效
     */
    private boolean isTokenValid(String cacheKey) {
        String token = tokenCache.get(cacheKey);
        Long expireTime = tokenExpireTime.get(cacheKey);

        return token != null && expireTime != null && System.currentTimeMillis() < expireTime;
    }
}