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
package com.alibaba.langengine.turbopuffer.vectorstore;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;


@Slf4j
public class RetryHelper {
    
    private final int maxRetries;
    private final long baseDelayMs;
    private final long maxDelayMs;
    private final double jitterFactor;
    
    public RetryHelper(int maxRetries) {
        this(maxRetries, 1000, 30000, 0.1);
    }
    
    public RetryHelper(int maxRetries, long baseDelayMs, long maxDelayMs, double jitterFactor) {
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.jitterFactor = jitterFactor;
    }
    
    /**
     * 执行带重试的操作
     */
    public <T> T execute(Supplier<T> operation, String operationName) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                
                if (attempt == maxRetries) {
                    log.error("Operation '{}' failed after {} attempts", operationName, attempt + 1);
                    break;
                }
                
                if (!isRetriableException(e)) {
                    log.error("Operation '{}' failed with non-retriable exception", operationName, e);
                    break;
                }
                
                long delay = calculateDelay(attempt);
                log.warn("Operation '{}' failed on attempt {}, retrying in {}ms", 
                        operationName, attempt + 1, delay, e);
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
        
        throw lastException;
    }
    
    /**
     * 执行带重试的操作（无返回值）
     */
    public void executeVoid(Runnable operation, String operationName) throws Exception {
        execute(() -> {
            operation.run();
            return null;
        }, operationName);
    }
    
    /**
     * 计算延迟时间（指数退避 + 抖动）
     */
    private long calculateDelay(int attempt) {
        // 指数退避
        long delay = Math.min(baseDelayMs * (1L << attempt), maxDelayMs);
        
        // 添加抖动
        if (jitterFactor > 0) {
            double jitter = 1 + (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * jitterFactor;
            delay = (long) (delay * jitter);
        }
        
        return Math.max(delay, 100); // 最小延迟100ms
    }
    
    /**
     * 判断异常是否可重试
     */
    private boolean isRetriableException(Exception e) {
        // 网络相关异常通常可重试
        if (e instanceof java.net.SocketTimeoutException ||
            e instanceof java.net.ConnectException ||
            e instanceof java.net.SocketException ||
            e instanceof java.io.IOException) {
            return true;
        }
        
        // Turbopuffer 特定异常
        if (e instanceof TurbopufferException) {
            TurbopufferException te = (TurbopufferException) e;
            // API 异常可能可重试，连接异常通常可重试
            return te instanceof TurbopufferException.TurbopufferApiException ||
                   te instanceof TurbopufferException.TurbopufferConnectionException;
        }
        
        // 包含特定关键词的异常可重试
        String message = e.getMessage();
        if (message != null) {
            message = message.toLowerCase();
            return message.contains("timeout") ||
                   message.contains("connection") ||
                   message.contains("network") ||
                   message.contains("502") ||
                   message.contains("503") ||
                   message.contains("504");
        }
        
        return false;
    }
}
