package com.alibaba.langengine.docloader.dingtalk.service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


@Slf4j
public class DingTalkRateLimitInterceptor implements Interceptor {

    private final Semaphore semaphore;
    private final long minIntervalMs;
    private volatile long lastRequestTime = 0;

    public DingTalkRateLimitInterceptor() {
        this.semaphore = new Semaphore(10); // 最大并发10个请求
        this.minIntervalMs = 100; // 最小间隔100ms
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        try {
            // 获取信号量
            if (!semaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                throw new IOException("Rate limit exceeded: too many concurrent requests");
            }

            // 控制请求间隔
            synchronized (this) {
                long now = System.currentTimeMillis();
                long elapsed = now - lastRequestTime;
                if (elapsed < minIntervalMs) {
                    try {
                        Thread.sleep(minIntervalMs - elapsed);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted during rate limiting", e);
                    }
                }
                lastRequestTime = System.currentTimeMillis();
            }

            Request request = chain.request();
            Response response = chain.proceed(request);

            // 检查响应状态
            if (response.code() == 429) {
                log.warn("Rate limit hit, backing off");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during rate limiting", e);
        } finally {
            semaphore.release();
        }
    }
}