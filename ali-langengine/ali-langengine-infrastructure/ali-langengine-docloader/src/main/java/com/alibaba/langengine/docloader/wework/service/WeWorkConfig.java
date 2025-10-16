package com.alibaba.langengine.docloader.wework.service;


public class WeWorkConfig {

    /**
     * 默认批次大小
     */
    public static final int DEFAULT_BATCH_SIZE = 50;

    /**
     * 最大批次大小
     */
    public static final int MAX_BATCH_SIZE = 100;

    /**
     * 最小批次大小
     */
    public static final int MIN_BATCH_SIZE = 1;

    /**
     * 默认超时时间（秒）
     */
    public static final long DEFAULT_TIMEOUT_SECONDS = 60L;

    /**
     * 默认域名
     */
    public static final String DEFAULT_DOMAIN = "https://work.weixin.qq.com/";

    /**
     * API基础URL
     */
    public static final String API_BASE_URL = "https://qyapi.weixin.qq.com/";

    /**
     * 默认连接池大小
     */
    public static final int DEFAULT_CONNECTION_POOL_SIZE = 20;

    /**
     * 默认连接保持时间（分钟）
     */
    public static final int DEFAULT_KEEP_ALIVE_MINUTES = 5;

    /**
     * 默认重试次数
     */
    public static final int DEFAULT_RETRY_COUNT = 3;

    /**
     * 默认重试基础延迟（毫秒）
     */
    public static final long DEFAULT_RETRY_BASE_DELAY_MS = 1000L;

    /**
     * 默认限流间隔（毫秒）
     */
    public static final long DEFAULT_RATE_LIMIT_INTERVAL_MS = 100L;

    /**
     * 默认并发线程数
     */
    public static final int DEFAULT_CONCURRENT_THREADS = 10;

    /**
     * 默认线程池核心线程数
     */
    public static final int DEFAULT_CORE_POOL_SIZE = 4;

    /**
     * 默认线程池最大线程数
     */
    public static final int DEFAULT_MAX_POOL_SIZE = 10;

    /**
     * 默认线程池空闲时间（秒）
     */
    public static final long DEFAULT_KEEP_ALIVE_TIME = 60L;

    /**
     * 令牌刷新提前时间（秒）
     */
    public static final long TOKEN_REFRESH_ADVANCE_TIME = 300L;

    /**
     * 默认分块大小（并发处理）
     */
    public static final int DEFAULT_CHUNK_SIZE = 10;

    /**
     * 请求超时时间（毫秒）
     */
    public static final long REQUEST_TIMEOUT_MS = 30000L;

    /**
     * 连接超时时间（毫秒）
     */
    public static final long CONNECT_TIMEOUT_MS = 10000L;

    /**
     * 私有构造函数，防止实例化
     */
    private WeWorkConfig() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
