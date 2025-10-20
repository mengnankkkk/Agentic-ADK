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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 钉钉服务
 *
 * @author Libres-coder
 */
@Slf4j
@Data
public class DingTalkService {

    /**
     * 钉钉开放平台API基础地址
     */
    private static final String BASE_URL = "https://oapi.dingtalk.com";

    private static final ObjectMapper mapper = defaultObjectMapper();

    /**
     * api
     */
    @JsonIgnore
    private DingTalkApi api;

    @JsonIgnore
    private ExecutorService executorService;

    @JsonIgnore
    private OkHttpClient client;

    /**
     * 应用Key
     */
    private String appKey;

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

    public DingTalkService(String appKey, String appSecret, Duration timeout) {
        this.appKey = appKey;
        this.appSecret = appSecret;

        this.client = defaultClient(timeout);
        this.executorService = client.dispatcher().executorService();
        Retrofit retrofit = defaultRetrofit(client, mapper);
        this.api = retrofit.create(DingTalkApi.class);

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
        DingTalkResult<DingTalkAccessToken> result = execute(api.getAccessToken(appKey, appSecret));
        
        if (result.getErrCode() != 0) {
            throw new RuntimeException("Failed to get access token: " + result.getErrMsg());
        }
        
        this.accessToken = result.getAccessToken();
        this.tokenExpireTime = System.currentTimeMillis() + (result.getExpiresIn() - 300) * 1000L;
        log.info("DingTalk access token refreshed, expires in {} seconds", result.getExpiresIn());
    }

    /**
     * 获取知识库列表
     *
     * @param userId 用户ID
     * @return 知识库列表
     */
    public DingTalkResult<DingTalkWorkspaceList> getWorkspaceList(String userId) {
        return execute(api.getWorkspaceList(getAccessToken(), new DingTalkUserRequest(userId)));
    }

    /**
     * 获取文档列表
     *
     * @param workspaceId 知识库ID
     * @param maxResults 最大结果数
     * @param nextToken 分页标记
     * @return 文档列表
     */
    public DingTalkResult<DingTalkDocList> getDocList(String workspaceId, Integer maxResults, String nextToken) {
        return execute(api.getDocList(getAccessToken(), 
            new DingTalkDocListRequest(workspaceId, maxResults, nextToken)));
    }

    /**
     * 获取文档内容
     *
     * @param docId 文档ID
     * @return 文档内容
     */
    public DingTalkResult<DingTalkDocContent> getDocContent(String docId) {
        return execute(api.getDocContent(getAccessToken(), new DingTalkDocRequest(docId)));
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
                log.error("DingTalk API error: {}", errorBody);
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

    public OkHttpClient defaultClient(Duration timeout) {
        return new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(5, 1, TimeUnit.SECONDS))
                .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .build();
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
