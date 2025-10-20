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
package com.alibaba.langengine.docloader.yuque.service;

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

/**
 * 语雀服务
 *
 * @author xiaoxuan.lp
 */
@Data
public class YuqueService {

    /**
     * 基础地址
     * 语雀开放API地址
     */
    private static final String BASE_URL = "https://www.yuque.com";

    private static final ObjectMapper mapper = defaultObjectMapper();

    /**
     * api
     */
    @JsonIgnore
    private YuqueApi api;

    @JsonIgnore
    private ExecutorService executorService;

    @JsonIgnore
    private OkHttpClient client;

    /**
     * 授权token
     */
    private String token;

    public YuqueService(String token, Duration timeout) {
        setToken(token);
        setClient(defaultClient(timeout));
        setExecutorService(client.dispatcher().executorService());

        Retrofit retrofit = defaultRetrofit(client, mapper);
        this.api = retrofit.create(YuqueApi.class);
    }

    public YuqueResult<List<YuqueDocInfo>> getDocs(String namespace, Integer offset, Integer limit, String optionalProperties) {
        return execute(getApi().getDocs(namespace, offset, limit, optionalProperties));
    }

    public YuqueResult<YuqueDocInfo> getDocDetail(String namespace, String slug) {
        return execute(getApi().getDocDetail(namespace, slug));
    }

    public static <T> T execute(Single<T> apiCall) {
        try {
            return apiCall.blockingGet();
        } catch (HttpException e) {
            try {
                if (e.response() == null || e.response().errorBody() == null) {
                    throw e;
                }
                String errorBody = e.response().errorBody().string();
                throw new RuntimeException(errorBody);
            } catch (IOException ex) {
                throw e;
            }
        }
    }

    public static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
        return mapper;
    }

    public OkHttpClient defaultClient(Duration timeout) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(5, 1, TimeUnit.SECONDS))
                .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
        builder.addInterceptor(new AuthenticationInterceptor(token));
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
