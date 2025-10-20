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

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 飞书API认证拦截器
 *
 * @author Libres-coder
 */
public class FeishuAuthenticationInterceptor implements Interceptor {

    private final Supplier<String> tokenSupplier;

    public FeishuAuthenticationInterceptor(Supplier<String> tokenSupplier) {
        Objects.requireNonNull(tokenSupplier, "Token supplier required");
        this.tokenSupplier = tokenSupplier;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        // 获取当前的访问令牌
        String token = tokenSupplier.get();
        
        Request request = chain.request()
                .newBuilder()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json; charset=utf-8")
                .build();
        return chain.proceed(request);
    }
}
