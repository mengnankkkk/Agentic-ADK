package com.alibaba.langengine.docloader.dingtalk.service;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;


public class DingTalkAuthenticationInterceptor implements Interceptor {

    private final String accessToken;

    public DingTalkAuthenticationInterceptor(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // 添加认证头
        Request authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("User-Agent", "DingTalk-DocLoader/1.0")
                .build();
        
        return chain.proceed(authenticatedRequest);
    }
}