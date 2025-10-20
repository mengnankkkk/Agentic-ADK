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
package com.alibaba.langengine.docloader.feishu.service;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 飞书服务
 *
 * @author Libres-coder
 */
@Slf4j
@Data
public class FeishuService {


    /**
     * 飞书开放平台API基础地址
     */
    private static final String BASE_URL = "https://open.feishu.cn";

    private static final ObjectMapper mapper = defaultObjectMapper();

    /**
     * api
     */
    @JsonIgnore
    private FeishuApi api;

    @JsonIgnore
    private FeishuApi authApi;

    @JsonIgnore
    private ExecutorService executorService;

    @JsonIgnore
    private OkHttpClient client;

    @JsonIgnore
    private OkHttpClient authClient;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 应用密钥
     */
    private String appSecret;

    /**
     * 访问令牌
     */
    @JsonIgnore
    private String accessToken;

    /**
     * 令牌过期时间（毫秒）
     */
    @JsonIgnore
    private long tokenExpireTime;

    public FeishuService(String appId, String appSecret, Duration timeout) {
        this.appId = appId;
        this.appSecret = appSecret;

        this.authClient = defaultAuthClient(timeout);
        Retrofit authRetrofit = defaultRetrofit(authClient, mapper);
        this.authApi = authRetrofit.create(FeishuApi.class);

        this.client = defaultClient(timeout);
        this.executorService = client.dispatcher().executorService();
        Retrofit retrofit = defaultRetrofit(client, mapper);
        this.api = retrofit.create(FeishuApi.class);

        refreshAccessToken();
    }

    /**
     * 获取访问令牌，如果令牌过期则自动刷新
     *
     * @return 访问令牌
     */
    public synchronized String getAccessToken() {
        if (accessToken == null || System.currentTimeMillis() >= tokenExpireTime) {
            refreshAccessToken();
        }
        return accessToken;
    }

    /**
     * 刷新访问令牌
     */
    private void refreshAccessToken() {
        FeishuAuthRequest authRequest = new FeishuAuthRequest(appId, appSecret);
        FeishuResult<FeishuAccessToken> result = execute(authApi.getTenantAccessToken(authRequest));
        
        if (result.getCode() != 0) {
            throw new RuntimeException("Failed to get access token: " + result.getMsg());
        }
        
        FeishuAccessToken tokenData = result.getData();
        this.accessToken = tokenData.getTenantAccessToken();
        this.tokenExpireTime = System.currentTimeMillis() + (tokenData.getExpire() - 300) * 1000L;
        log.info("Feishu access token refreshed, expires in {} seconds", tokenData.getExpire());
    }

    /**
     * 获取文档原始内容
     *
     * @param docToken 文档token
     * @return 文档内容
     */
    public FeishuResult<FeishuDocContent> getDocxRawContent(String docToken) {
        return execute(api.getDocxRawContent(docToken));
    }

    /**
     * 获取文档元信息
     *
     * @param docToken 文档token
     * @return 文档元信息
     */
    public FeishuResult<FeishuDocMeta> getDocMeta(String docToken) {
        return execute(api.getDocMeta(docToken));
    }

    /**
     * 获取旧版文档块内容
     *
     * @param docToken 文档token
     * @return 文档块内容
     */
    public FeishuResult<FeishuDocBlocks> getDocBlocks(String docToken) {
        return execute(api.getDocBlocks(docToken));
    }

    /**
     * 获取知识库列表
     *
     * @param pageToken 分页标记
     * @param pageSize 分页大小
     * @return 知识库列表
     */
    public FeishuResult<FeishuSpaceList> getSpaces(String pageToken, Integer pageSize) {
        return execute(api.getSpaces(pageToken, pageSize));
    }

    /**
     * 获取知识库节点列表
     *
     * @param spaceId 知识库ID
     * @param pageToken 分页标记
     * @param pageSize 分页大小
     * @return 节点列表
     */
    public FeishuResult<FeishuNodeList> getSpaceNodes(String spaceId, String pageToken, Integer pageSize) {
        return execute(api.getSpaceNodes(spaceId, pageToken, pageSize));
    }

    /**
     * 执行API调用
     *
     * @param apiCall API调用
     * @param <T> 返回类型
     * @return 结果
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
                log.error("Feishu API error: {}", errorBody);
                throw new RuntimeException(errorBody);
            } catch (IOException ex) {
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return mapper;
    }

    /**
     * 创建用于认证的客户端（不包含认证拦截器）
     */
    public OkHttpClient defaultAuthClient(Duration timeout) {
        return new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(5, 1, TimeUnit.SECONDS))
                .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * 创建用于业务调用的客户端（包含认证拦截器）
     */
    public OkHttpClient defaultClient(Duration timeout) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(5, 1, TimeUnit.SECONDS))
                .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
        builder.addInterceptor(new FeishuAuthenticationInterceptor(this::getAccessToken));
        return builder.build();
    }

    public Retrofit defaultRetrofit(OkHttpClient client, ObjectMapper mapper) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }
}
