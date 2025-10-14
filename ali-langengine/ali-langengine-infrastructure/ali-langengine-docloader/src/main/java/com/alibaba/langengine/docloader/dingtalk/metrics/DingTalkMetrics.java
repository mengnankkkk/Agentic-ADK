package com.alibaba.langengine.docloader.dingtalk.metrics;

import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;


@Data
public class DingTalkMetrics {

    // 请求计数
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successfulRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();

    // 文档加载统计
    private final LongAdder documentsLoaded = new LongAdder();
    private final LongAdder documentsSkipped = new LongAdder();
    private final LongAdder documentsFailed = new LongAdder();

    // 延迟统计（毫秒）
    private final LongAdder totalLatencyMs = new LongAdder();
    private final AtomicLong maxLatencyMs = new AtomicLong(0);
    private final AtomicLong minLatencyMs = new AtomicLong(Long.MAX_VALUE);

    // 错误分类
    private final ConcurrentHashMap<String, LongAdder> errorsByType = new ConcurrentHashMap<>();

    // 重试统计
    private final LongAdder totalRetries = new LongAdder();
    private final LongAdder successfulRetries = new LongAdder();

    // 限流统计
    private final LongAdder rateLimitHits = new LongAdder();

    // 熔断器统计
    private final LongAdder circuitBreakerOpens = new LongAdder();
    private final LongAdder circuitBreakerRejects = new LongAdder();

    // 时间戳
    private final Instant startTime = Instant.now();
    private volatile Instant lastRequestTime = Instant.now();

    /**
     * 记录请求开始
     */
    public void recordRequestStart() {
        totalRequests.increment();
        lastRequestTime = Instant.now();
    }

    /**
     * 记录请求成功
     */
    public void recordRequestSuccess(long latencyMs) {
        successfulRequests.increment();
        recordLatency(latencyMs);
    }

    /**
     * 记录请求失败
     */
    public void recordRequestFailure(String errorType, long latencyMs) {
        failedRequests.increment();
        recordLatency(latencyMs);
        recordError(errorType);
    }

    /**
     * 记录延迟
     */
    private void recordLatency(long latencyMs) {
        totalLatencyMs.add(latencyMs);

        // 更新最大延迟
        long currentMax;
        do {
            currentMax = maxLatencyMs.get();
            if (latencyMs <= currentMax) break;
        } while (!maxLatencyMs.compareAndSet(currentMax, latencyMs));

        // 更新最小延迟
        long currentMin;
        do {
            currentMin = minLatencyMs.get();
            if (latencyMs >= currentMin) break;
        } while (!minLatencyMs.compareAndSet(currentMin, latencyMs));
    }

    /**
     * 记录错误
     */
    private void recordError(String errorType) {
        errorsByType.computeIfAbsent(errorType, k -> new LongAdder()).increment();
    }

    /**
     * 记录文档加载成功
     */
    public void recordDocumentLoaded() {
        documentsLoaded.increment();
    }

    /**
     * 记录文档跳过
     */
    public void recordDocumentSkipped() {
        documentsSkipped.increment();
    }

    /**
     * 记录文档加载失败
     */
    public void recordDocumentFailed() {
        documentsFailed.increment();
    }

    /**
     * 记录重试
     */
    public void recordRetry(boolean successful) {
        totalRetries.increment();
        if (successful) {
            successfulRetries.increment();
        }
    }

    /**
     * 记录限流命中
     */
    public void recordRateLimitHit() {
        rateLimitHits.increment();
    }

    /**
     * 记录熔断器打开
     */
    public void recordCircuitBreakerOpen() {
        circuitBreakerOpens.increment();
    }

    /**
     * 记录熔断器拒绝
     */
    public void recordCircuitBreakerReject() {
        circuitBreakerRejects.increment();
    }

    /**
     * 获取成功率（百分比）
     */
    public double getSuccessRate() {
        long total = totalRequests.sum();
        if (total == 0) return 100.0;
        return (double) successfulRequests.sum() / total * 100.0;
    }

    /**
     * 获取失败率（百分比）
     */
    public double getFailureRate() {
        return 100.0 - getSuccessRate();
    }

    /**
     * 获取平均延迟（毫秒）
     */
    public double getAverageLatencyMs() {
        long total = totalRequests.sum();
        if (total == 0) return 0.0;
        return (double) totalLatencyMs.sum() / total;
    }

    /**
     * 获取请求速率（每秒）
     */
    public double getRequestRate() {
        Duration uptime = Duration.between(startTime, Instant.now());
        if (uptime.toSeconds() == 0) return 0.0;
        return (double) totalRequests.sum() / uptime.toSeconds();
    }

    /**
     * 获取重试成功率（百分比）
     */
    public double getRetrySuccessRate() {
        long total = totalRetries.sum();
        if (total == 0) return 100.0;
        return (double) successfulRetries.sum() / total * 100.0;
    }

    /**
     * 获取运行时长
     */
    public Duration getUptime() {
        return Duration.between(startTime, Instant.now());
    }

    /**
     * 重置所有指标
     */
    public void reset() {
        totalRequests.reset();
        successfulRequests.reset();
        failedRequests.reset();
        documentsLoaded.reset();
        documentsSkipped.reset();
        documentsFailed.reset();
        totalLatencyMs.reset();
        maxLatencyMs.set(0);
        minLatencyMs.set(Long.MAX_VALUE);
        errorsByType.clear();
        totalRetries.reset();
        successfulRetries.reset();
        rateLimitHits.reset();
        circuitBreakerOpens.reset();
        circuitBreakerRejects.reset();
    }

    /**
     * 获取摘要信息
     */
    public MetricsSummary getSummary() {
        return new MetricsSummary(
            totalRequests.sum(),
            successfulRequests.sum(),
            failedRequests.sum(),
            getSuccessRate(),
            getAverageLatencyMs(),
            minLatencyMs.get() == Long.MAX_VALUE ? 0 : minLatencyMs.get(),
            maxLatencyMs.get(),
            documentsLoaded.sum(),
            documentsSkipped.sum(),
            documentsFailed.sum(),
            totalRetries.sum(),
            rateLimitHits.sum(),
            circuitBreakerOpens.sum(),
            circuitBreakerRejects.sum(),
            getUptime().toSeconds(),
            new ConcurrentHashMap<>(errorsByType)
        );
    }

    /**
     * 度量指标摘要
     */
    @Data
    public static class MetricsSummary {
        private final long totalRequests;
        private final long successfulRequests;
        private final long failedRequests;
        private final double successRate;
        private final double avgLatencyMs;
        private final long minLatencyMs;
        private final long maxLatencyMs;
        private final long documentsLoaded;
        private final long documentsSkipped;
        private final long documentsFailed;
        private final long totalRetries;
        private final long rateLimitHits;
        private final long circuitBreakerOpens;
        private final long circuitBreakerRejects;
        private final long uptimeSeconds;
        private final ConcurrentHashMap<String, LongAdder> errorsByType;

        @Override
        public String toString() {
            return String.format(
                "Metrics[requests=%d, success_rate=%.2f%%, avg_latency=%.2fms, " +
                "docs_loaded=%d, docs_failed=%d, retries=%d, uptime=%ds]",
                totalRequests, successRate, avgLatencyMs,
                documentsLoaded, documentsFailed, totalRetries, uptimeSeconds
            );
        }
    }
}
