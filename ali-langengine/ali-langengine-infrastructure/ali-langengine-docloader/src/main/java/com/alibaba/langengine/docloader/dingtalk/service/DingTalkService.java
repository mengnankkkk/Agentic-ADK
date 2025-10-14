package com.alibaba.langengine.docloader.dingtalk.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.reactivex.Single;
import lombok.Data;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;


@Data
public class DingTalkService {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    private static final String BASE_URL = "https://oapi.dingtalk.com/";
    
    private static final ObjectMapper mapper = defaultObjectMapper();

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
     * 获取文档列表
     */
    public DingTalkResult<List<DingTalkDocInfo>> getDocumentList(String namespace, Integer offset, Integer limit) {
        return execute(getApi().getDocumentList(accessToken, namespace, offset, limit));
    }

    /**
     * 获取文档详情
     */
    public DingTalkResult<DingTalkDocInfo> getDocumentDetail(String namespace, String documentId) {
        return execute(getApi().getDocumentDetail(accessToken, namespace, documentId));
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
     * 默认HTTP客户端配置
     */
    public OkHttpClient defaultClient(Duration timeout) {
        return new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(5, 1, TimeUnit.SECONDS))
                .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .connectTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .addInterceptor(new DingTalkAuthenticationInterceptor(accessToken))
                .build();
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