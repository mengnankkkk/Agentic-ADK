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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


@Slf4j
public class CircuitBreaker {
    
    public enum State {
        CLOSED,    // 熔断器关闭，正常通行
        OPEN,      // 熔断器开启，拒绝请求
        HALF_OPEN  // 熔断器半开，允许部分请求通过
    }
    
    private final TurbopufferParam.CircuitBreakerConfig config;
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    
    public CircuitBreaker(TurbopufferParam.CircuitBreakerConfig config) {
        this.config = config;
    }
    
    /**
     * 检查是否允许请求通过
     */
    public boolean allowRequest() {
        if (!config.enabled) {
            return true;
        }
        
        State currentState = state.get();
        
        switch (currentState) {
            case CLOSED:
                return true;
                
            case OPEN:
                // 检查是否到了恢复时间
                if (System.currentTimeMillis() - lastFailureTime.get() > config.recoveryTimeoutMs) {
                    state.compareAndSet(State.OPEN, State.HALF_OPEN);
                    log.info("Circuit breaker state changed to HALF_OPEN");
                    return true;
                }
                return false;
                
            case HALF_OPEN:
                // 半开状态下允许少量请求通过
                return successCount.get() < 3;
                
            default:
                return true;
        }
    }
    
    /**
     * 记录成功
     */
    public void recordSuccess() {
        if (!config.enabled) {
            return;
        }
        
        State currentState = state.get();
        successCount.incrementAndGet();
        
        if (currentState == State.HALF_OPEN) {
            // 半开状态下连续成功，恢复到关闭状态
            if (successCount.get() >= 3) {
                reset();
                state.set(State.CLOSED);
                log.info("Circuit breaker recovered to CLOSED state");
            }
        } else if (currentState == State.CLOSED) {
            // 正常状态下重置失败计数
            failureCount.set(0);
        }
    }
    
    /**
     * 记录失败
     */
    public void recordFailure() {
        if (!config.enabled) {
            return;
        }
        
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        
        if (failures >= config.failureThreshold) {
            state.set(State.OPEN);
            log.warn("Circuit breaker opened due to {} failures", failures);
        }
    }
    
    /**
     * 重置熔断器
     */
    public void reset() {
        failureCount.set(0);
        successCount.set(0);
        lastFailureTime.set(0);
    }
    
    /**
     * 获取当前状态
     */
    public State getState() {
        return state.get();
    }
    
    /**
     * 获取失败次数
     */
    public int getFailureCount() {
        return failureCount.get();
    }
}
