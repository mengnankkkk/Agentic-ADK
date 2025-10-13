package com.alibaba.langengine.docloader.feishu.config;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class FeishuConfig {
    
    /**
     * 默认批次大小
     */
    public static final int DEFAULT_BATCH_SIZE = 50;
    
    /**
     * 最大批次大小
     */
    public static final int MAX_BATCH_SIZE = 100;
    
    /**
     * 默认超时时间（秒）
     */
    public static final long DEFAULT_TIMEOUT = 30L;
    
    /**
     * 默认线程池核心线程数
     */
    public static final int DEFAULT_CORE_POOL_SIZE = 4;
    
    /**
     * 默认线程池最大线程数
     */
    public static final int DEFAULT_MAX_POOL_SIZE = 8;
    
    /**
     * 默认线程池空闲时间（秒）
     */
    public static final long DEFAULT_KEEP_ALIVE_TIME = 60L;
    
    /**
     * 令牌刷新提前时间（秒）
     */
    public static final long TOKEN_REFRESH_ADVANCE_TIME = 300L;
    
    /**
     * 默认飞书域名
     */
    public static final String DEFAULT_DOMAIN = "https://feishu.cn/";
    
    /**
     * 飞书API基础URL
     */
    public static final String API_BASE_URL = "https://open.feishu.cn/open-apis/";
    
    /**
     * 令牌获取URL
     */
    public static final String TOKEN_URL = API_BASE_URL + "auth/v3/tenant_access_token/internal/";
}