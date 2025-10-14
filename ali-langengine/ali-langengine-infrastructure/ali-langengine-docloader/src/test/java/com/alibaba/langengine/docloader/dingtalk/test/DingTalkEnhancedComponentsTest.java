package com.alibaba.langengine.docloader.dingtalk.test;

import com.alibaba.langengine.docloader.dingtalk.circuitbreaker.SimpleCircuitBreaker;
import com.alibaba.langengine.docloader.dingtalk.config.DingTalkConfig;
import com.alibaba.langengine.docloader.dingtalk.exception.*;
import com.alibaba.langengine.docloader.dingtalk.metrics.DingTalkMetrics;
import com.alibaba.langengine.docloader.dingtalk.ratelimit.TokenBucketRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;


public class DingTalkEnhancedComponentsTest {

    @Test
    public void testConfigDefaultValues() {
        DingTalkConfig config = DingTalkConfig.defaultConfig();

        assertNotNull(config);
        assertEquals("https://oapi.dingtalk.com/", config.getBaseUrl());
        assertEquals(3, config.getMaxRetries());
        assertEquals(10.0, config.getRateLimitPermitsPerSecond());
        assertTrue(config.isCircuitBreakerEnabled());
    }

    @Test
    public void testConfigBuilder() {
        DingTalkConfig config = DingTalkConfig.builder()
                .maxRetries(5)
                .batchSize(100)
                .rateLimitPermitsPerSecond(20.0)
                .circuitBreakerEnabled(false)
                .build();

        assertEquals(5, config.getMaxRetries());
        assertEquals(100, config.getBatchSize());
        assertEquals(20.0, config.getRateLimitPermitsPerSecond());
        assertFalse(config.isCircuitBreakerEnabled());
    }

    @Test
    public void testConfigValidation_InvalidBatchSize() {
        DingTalkConfig config = DingTalkConfig.builder()
                .batchSize(150)  // 超过最大值100
                .build();

        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    public void testConfigValidation_InvalidRetries() {
        DingTalkConfig config = DingTalkConfig.builder()
                .maxRetries(15)  // 超过最大值10
                .build();

        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    public void testHighPerformanceConfig() {
        DingTalkConfig config = DingTalkConfig.highPerformanceConfig();

        assertEquals(50, config.getMaxConnections());
        assertEquals(20.0, config.getRateLimitPermitsPerSecond());
        assertEquals(40, config.getRateLimitBurstCapacity());
    }

    @Test
    public void testConservativeConfig() {
        DingTalkConfig config = DingTalkConfig.conservativeConfig();

        assertEquals(5, config.getMaxConnections());
        assertEquals(2.0, config.getRateLimitPermitsPerSecond());
        assertEquals(5, config.getRateLimitBurstCapacity());
    }


    @Test
    public void testRateLimiter_InitialState() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(
                10.0, 20, Duration.ofSeconds(5)
        );

        // 初始时令牌桶是满的
        assertEquals(20.0, limiter.getAvailableTokens(), 0.01);
    }

    @Test
    public void testRateLimiter_Acquire() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(
                10.0, 20, Duration.ofSeconds(5)
        );

        // 获取令牌
        assertTrue(limiter.acquire());
        assertTrue(limiter.getAvailableTokens() < 20.0);
    }

    @Test
    public void testRateLimiter_TryAcquire() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(
                10.0, 5, Duration.ofSeconds(1)
        );

        // 快速消耗所有令牌
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire());
        }

        // 令牌耗尽，应该失败
        assertFalse(limiter.tryAcquire());
    }

    @Test
    public void testRateLimiter_Refill() throws InterruptedException {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(
                10.0, 10, Duration.ofSeconds(5)
        );

        // 消耗所有令牌
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire();
        }

        double afterConsumption = limiter.getAvailableTokens();
        assertTrue(afterConsumption < 1.0);

        // 等待令牌补充
        Thread.sleep(500);  // 0.5秒应该补充约5个令牌

        double afterRefill = limiter.getAvailableTokens();
        assertTrue(afterRefill > afterConsumption);
    }

    @Test
    public void testRateLimiter_Reset() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(
                10.0, 20, Duration.ofSeconds(5)
        );

        // 消耗令牌
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire();
        }

        // 重置
        limiter.reset();

        // 应该恢复到满容量
        assertEquals(20.0, limiter.getAvailableTokens(), 0.01);
    }

    @Test
    public void testRateLimiter_Stats() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(
                10.0, 20, Duration.ofSeconds(5)
        );

        TokenBucketRateLimiter.RateLimiterStats stats = limiter.getStats();

        assertEquals(10.0, stats.permitsPerSecond);
        assertEquals(20, stats.maxCapacity);
        assertNotNull(stats.toString());
    }


    private DingTalkMetrics metrics;

    @BeforeEach
    public void setupMetrics() {
        metrics = new DingTalkMetrics();
    }

    @Test
    public void testMetrics_InitialState() {
        assertEquals(0, metrics.getTotalRequests().sum());
        assertEquals(0, metrics.getSuccessfulRequests().sum());
        assertEquals(0, metrics.getFailedRequests().sum());
        assertEquals(100.0, metrics.getSuccessRate(), 0.01);
    }

    @Test
    public void testMetrics_RecordSuccess() {
        metrics.recordRequestStart();
        metrics.recordRequestSuccess(100);

        assertEquals(1, metrics.getTotalRequests().sum());
        assertEquals(1, metrics.getSuccessfulRequests().sum());
        assertEquals(100.0, metrics.getSuccessRate(), 0.01);
        assertEquals(100.0, metrics.getAverageLatencyMs(), 0.01);
    }

    @Test
    public void testMetrics_RecordFailure() {
        metrics.recordRequestStart();
        metrics.recordRequestFailure("NetworkError", 200);

        assertEquals(1, metrics.getTotalRequests().sum());
        assertEquals(1, metrics.getFailedRequests().sum());
        assertEquals(0.0, metrics.getSuccessRate(), 0.01);
    }

    @Test
    public void testMetrics_SuccessRate() {
        // 模拟10次请求：8次成功，2次失败
        for (int i = 0; i < 8; i++) {
            metrics.recordRequestStart();
            metrics.recordRequestSuccess(100);
        }
        for (int i = 0; i < 2; i++) {
            metrics.recordRequestStart();
            metrics.recordRequestFailure("Error", 100);
        }

        assertEquals(10, metrics.getTotalRequests().sum());
        assertEquals(8, metrics.getSuccessfulRequests().sum());
        assertEquals(2, metrics.getFailedRequests().sum());
        assertEquals(80.0, metrics.getSuccessRate(), 0.01);
        assertEquals(20.0, metrics.getFailureRate(), 0.01);
    }

    @Test
    public void testMetrics_DocumentTracking() {
        metrics.recordDocumentLoaded();
        metrics.recordDocumentLoaded();
        metrics.recordDocumentSkipped();
        metrics.recordDocumentFailed();

        assertEquals(2, metrics.getDocumentsLoaded().sum());
        assertEquals(1, metrics.getDocumentsSkipped().sum());
        assertEquals(1, metrics.getDocumentsFailed().sum());
    }

    @Test
    public void testMetrics_Retry() {
        metrics.recordRetry(true);
        metrics.recordRetry(true);
        metrics.recordRetry(false);

        assertEquals(3, metrics.getTotalRetries().sum());
        assertEquals(2, metrics.getSuccessfulRetries().sum());
        assertEquals(66.67, metrics.getRetrySuccessRate(), 0.1);
    }

    @Test
    public void testMetrics_Summary() {
        metrics.recordRequestStart();
        metrics.recordRequestSuccess(100);
        metrics.recordDocumentLoaded();

        DingTalkMetrics.MetricsSummary summary = metrics.getSummary();

        assertNotNull(summary);
        assertEquals(1, summary.getTotalRequests());
        assertEquals(1, summary.getSuccessfulRequests());
        assertEquals(1, summary.getDocumentsLoaded());
        assertNotNull(summary.toString());
    }

    @Test
    public void testMetrics_Reset() {
        metrics.recordRequestStart();
        metrics.recordRequestSuccess(100);
        metrics.recordDocumentLoaded();

        metrics.reset();

        assertEquals(0, metrics.getTotalRequests().sum());
        assertEquals(0, metrics.getDocumentsLoaded().sum());
    }


    private SimpleCircuitBreaker circuitBreaker;
    private DingTalkConfig cbConfig;

    @BeforeEach
    public void setupCircuitBreaker() {
        cbConfig = DingTalkConfig.builder()
                .circuitBreakerEnabled(true)
                .circuitBreakerFailureThreshold(50.0)
                .circuitBreakerMinimumRequests(5)
                .circuitBreakerWaitDurationMs(1000)
                .circuitBreakerPermittedCallsInHalfOpen(3)
                .build();

        circuitBreaker = new SimpleCircuitBreaker(cbConfig);
    }

    @Test
    public void testCircuitBreaker_InitialState() {
        assertEquals("CLOSED", circuitBreaker.getCurrentState());

        // 初始状态应该允许请求
        assertDoesNotThrow(() -> circuitBreaker.checkState());
    }

    @Test
    public void testCircuitBreaker_RecordSuccess() {
        circuitBreaker.recordSuccess();

        assertEquals("CLOSED", circuitBreaker.getCurrentState());
        assertEquals(0.0, circuitBreaker.getFailureRate(), 0.01);
    }

    @Test
    public void testCircuitBreaker_OpenOnFailureThreshold() {
        // 记录足够的失败以触发熔断器打开
        for (int i = 0; i < 3; i++) {
            circuitBreaker.recordSuccess();
        }
        for (int i = 0; i < 4; i++) {
            circuitBreaker.recordFailure();
        }

        // 失败率 = 4/7 = 57% > 50%，应该打开
        assertEquals("OPEN", circuitBreaker.getCurrentState());
    }

    @Test
    public void testCircuitBreaker_RejectWhenOpen() {
        // 强制打开熔断器
        for (int i = 0; i < 3; i++) {
            circuitBreaker.recordSuccess();
        }
        for (int i = 0; i < 4; i++) {
            circuitBreaker.recordFailure();
        }

        assertEquals("OPEN", circuitBreaker.getCurrentState());

        // 应该拒绝请求
        assertThrows(DingTalkCircuitBreakerException.class,
                () -> circuitBreaker.checkState());
    }

    @Test
    public void testCircuitBreaker_TransitionToHalfOpen() throws InterruptedException {
        // 打开熔断器
        for (int i = 0; i < 3; i++) {
            circuitBreaker.recordSuccess();
        }
        for (int i = 0; i < 4; i++) {
            circuitBreaker.recordFailure();
        }

        assertEquals("OPEN", circuitBreaker.getCurrentState());

        // 等待超过等待时间
        Thread.sleep(1100);

        // 下次检查应该转换到 HALF_OPEN
        assertDoesNotThrow(() -> circuitBreaker.checkState());
        assertEquals("HALF_OPEN", circuitBreaker.getCurrentState());
    }

    @Test
    public void testCircuitBreaker_Reset() {
        // 打开熔断器
        for (int i = 0; i < 3; i++) {
            circuitBreaker.recordSuccess();
        }
        for (int i = 0; i < 4; i++) {
            circuitBreaker.recordFailure();
        }

        assertEquals("OPEN", circuitBreaker.getCurrentState());

        // 手动重置
        circuitBreaker.reset();

        assertEquals("CLOSED", circuitBreaker.getCurrentState());
        assertEquals(0.0, circuitBreaker.getFailureRate(), 0.01);
    }

    @Test
    public void testCircuitBreaker_Stats() {
        circuitBreaker.recordSuccess();
        circuitBreaker.recordSuccess();
        circuitBreaker.recordFailure();

        SimpleCircuitBreaker.CircuitBreakerStats stats = circuitBreaker.getStats();

        assertEquals("CLOSED", stats.state);
        assertEquals(2, stats.successCount);
        assertEquals(1, stats.failureCount);
        assertEquals(33.33, stats.failureRate, 0.1);
        assertNotNull(stats.toString());
    }


    @Test
    public void testDingTalkException_Basic() {
        DingTalkException ex = new DingTalkException("Test error");

        assertEquals("Test error", ex.getMessage());
        assertNull(ex.getErrorCode());
        assertNull(ex.getHttpStatusCode());
    }

    @Test
    public void testDingTalkException_WithDetails() {
        DingTalkException ex = new DingTalkException(
                "API error", "ERR_001", 500
        );

        assertEquals("API error", ex.getMessage());
        assertEquals("ERR_001", ex.getErrorCode());
        assertEquals(500, ex.getHttpStatusCode());
    }

    @Test
    public void testAuthenticationException() {
        DingTalkAuthenticationException ex =
                new DingTalkAuthenticationException("Invalid token");

        assertEquals("Invalid token", ex.getMessage());
        assertEquals("AUTH_ERROR", ex.getErrorCode());
        assertEquals(401, ex.getHttpStatusCode());
    }

    @Test
    public void testRateLimitException() {
        DingTalkRateLimitException ex =
                new DingTalkRateLimitException("Too many requests", 60L);

        assertEquals("Too many requests", ex.getMessage());
        assertEquals(60L, ex.getRetryAfterSeconds());
        assertEquals(429, ex.getHttpStatusCode());
    }

    @Test
    public void testResourceNotFoundException() {
        DingTalkResourceNotFoundException ex =
                new DingTalkResourceNotFoundException("Document", "doc-123");

        assertTrue(ex.getMessage().contains("Document"));
        assertTrue(ex.getMessage().contains("doc-123"));
        assertEquals("Document", ex.getResourceType());
        assertEquals("doc-123", ex.getResourceId());
        assertEquals(404, ex.getHttpStatusCode());
    }

    @Test
    public void testCircuitBreakerException() {
        DingTalkCircuitBreakerException ex =
                new DingTalkCircuitBreakerException("Service down", "OPEN");

        assertEquals("Service down", ex.getMessage());
        assertEquals("OPEN", ex.getCircuitBreakerState());
        assertEquals(503, ex.getHttpStatusCode());
    }
}
