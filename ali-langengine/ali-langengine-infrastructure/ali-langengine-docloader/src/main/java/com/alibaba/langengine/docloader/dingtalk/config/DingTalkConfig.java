package com.alibaba.langengine.docloader.dingtalk.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DingTalkConfig {

    /**
     * API基础URL
     */
    @Builder.Default
    private String baseUrl = "https://oapi.dingtalk.com/";

    /**
     * 钉钉文档域名
     */
    @Builder.Default
    private String documentDomain = "https://docs.dingtalk.com/";

    // ========== 超时配置 ==========

    /**
     * 连接超时时间
     */
    @Builder.Default
    private Duration connectTimeout = Duration.ofSeconds(30);

    /**
     * 读取超时时间
     */
    @Builder.Default
    private Duration readTimeout = Duration.ofSeconds(60);

    /**
     * 写入超时时间
     */
    @Builder.Default
    private Duration writeTimeout = Duration.ofSeconds(60);

    // ========== 重试配置 ==========

    /**
     * 最大重试次数
     */
    @Builder.Default
    private int maxRetries = 3;

    /**
     * 重试基础延迟（毫秒）
     */
    @Builder.Default
    private long retryBaseDelayMs = 1000;

    /**
     * 重试最大延迟（毫秒）
     */
    @Builder.Default
    private long retryMaxDelayMs = 10000;

    /**
     * 是否启用指数退避
     */
    @Builder.Default
    private boolean exponentialBackoff = true;

    // ========== 限流配置 ==========

    /**
     * 令牌桶容量（每秒请求数）
     */
    @Builder.Default
    private double rateLimitPermitsPerSecond = 10.0;

    /**
     * 令牌桶最大突发容量
     */
    @Builder.Default
    private int rateLimitBurstCapacity = 20;

    /**
     * 获取令牌超时时间
     */
    @Builder.Default
    private Duration rateLimitAcquireTimeout = Duration.ofSeconds(5);

    // ========== 连接池配置 ==========

    /**
     * 最大连接数
     */
    @Builder.Default
    private int maxConnections = 20;

    /**
     * 连接保活时间（分钟）
     */
    @Builder.Default
    private int keepAliveMinutes = 5;

    // ========== 批处理配置 ==========

    /**
     * 批量加载时的批次大小
     */
    @Builder.Default
    private int batchSize = 50;

    /**
     * 并发处理块大小
     */
    @Builder.Default
    private int concurrentChunkSize = 10;

    /**
     * 单个文档获取超时时间
     */
    @Builder.Default
    private Duration documentFetchTimeout = Duration.ofSeconds(30);

    // ========== 熔断器配置 ==========

    /**
     * 是否启用熔断器
     */
    @Builder.Default
    private boolean circuitBreakerEnabled = true;

    /**
     * 熔断器失败率阈值（百分比）
     */
    @Builder.Default
    private double circuitBreakerFailureThreshold = 50.0;

    /**
     * 熔断器最小请求数
     */
    @Builder.Default
    private int circuitBreakerMinimumRequests = 10;

    /**
     * 熔断器等待时间（毫秒）
     */
    @Builder.Default
    private long circuitBreakerWaitDurationMs = 30000;

    /**
     * 熔断器半开状态允许的请求数
     */
    @Builder.Default
    private int circuitBreakerPermittedCallsInHalfOpen = 5;

    // ========== 监控配置 ==========

    /**
     * 是否启用度量指标
     */
    @Builder.Default
    private boolean metricsEnabled = true;

    /**
     * 度量指标保留时间（分钟）
     */
    @Builder.Default
    private int metricsRetentionMinutes = 60;

    // ========== 安全配置 ==========

    /**
     * 是否启用日志脱敏
     */
    @Builder.Default
    private boolean logMaskingEnabled = true;

    /**
     * Token显示长度（其余*号）
     */
    @Builder.Default
    private int tokenVisibleLength = 8;

    // ========== 内容处理配置 ==========

    /**
     * 是否返回HTML内容
     */
    @Builder.Default
    private boolean returnHtml = false;

    /**
     * 是否跳过空内容文档
     */
    @Builder.Default
    private boolean skipEmptyDocuments = true;

    /**
     * 失败文档记录最大数量
     */
    @Builder.Default
    private int maxFailedDocumentsTracked = 1000;

    /**
     * 验证配置参数的合法性
     */
    public void validate() {
        if (batchSize <= 0 || batchSize > 100) {
            throw new IllegalArgumentException("Batch size must be between 1 and 100");
        }
        if (maxRetries < 0 || maxRetries > 10) {
            throw new IllegalArgumentException("Max retries must be between 0 and 10");
        }
        if (rateLimitPermitsPerSecond <= 0 || rateLimitPermitsPerSecond > 1000) {
            throw new IllegalArgumentException("Rate limit must be between 0 and 1000 permits/second");
        }
        if (circuitBreakerFailureThreshold < 0 || circuitBreakerFailureThreshold > 100) {
            throw new IllegalArgumentException("Circuit breaker failure threshold must be between 0 and 100");
        }
    }

    /**
     * 创建默认配置
     */
    public static DingTalkConfig defaultConfig() {
        return DingTalkConfig.builder().build();
    }

    /**
     * 创建高性能配置
     */
    public static DingTalkConfig highPerformanceConfig() {
        return DingTalkConfig.builder()
                .maxConnections(50)
                .rateLimitPermitsPerSecond(20.0)
                .rateLimitBurstCapacity(40)
                .concurrentChunkSize(20)
                .maxRetries(5)
                .build();
    }

    /**
     * 创建保守配置（适合受限环境）
     */
    public static DingTalkConfig conservativeConfig() {
        return DingTalkConfig.builder()
                .maxConnections(5)
                .rateLimitPermitsPerSecond(2.0)
                .rateLimitBurstCapacity(5)
                .concurrentChunkSize(3)
                .maxRetries(2)
                .circuitBreakerEnabled(true)
                .build();
    }
}
