package com.alibaba.langengine.trawex;

import com.alibaba.langengine.core.util.WorkPropertiesUtils;

/**
 * Trawex Travel Technology Solutions API 配置类
 * 
 * @author AIDC-AI
 */
public class TrawexConfiguration {

    /**
     * Trawex API 基础 URL
     * 默认: https://api.trawex.com/v1
     */
    public static String TRAWEX_API_BASE_URL = WorkPropertiesUtils.getOrDefault(
        "trawex.api.base.url", "https://api.trawex.com/v1");
    
    /**
     * Trawex API Key
     * 从 Trawex 管理面板获取
     */
    public static String TRAWEX_API_KEY = WorkPropertiesUtils.get("trawex.api.key");
    
    /**
     * Trawex API Secret
     */
    public static String TRAWEX_API_SECRET = WorkPropertiesUtils.get("trawex.api.secret");
    
    /**
     * 请求超时时间（秒）
     * 默认: 30
     */
    public static int TRAWEX_REQUEST_TIMEOUT = Integer.parseInt(
        WorkPropertiesUtils.getOrDefault("trawex.request.timeout", "30"));
    
    /**
     * 默认语言代码
     * 默认: en-US
     */
    public static String TRAWEX_DEFAULT_LANGUAGE = WorkPropertiesUtils.getOrDefault(
        "trawex.default.language", "en-US");
    
    /**
     * 默认货币代码
     * 默认: USD
     */
    public static String TRAWEX_DEFAULT_CURRENCY = WorkPropertiesUtils.getOrDefault(
        "trawex.default.currency", "USD");
    
    /**
     * 是否使用沙箱环境
     * 默认: false
     */
    public static boolean TRAWEX_USE_SANDBOX = Boolean.parseBoolean(
        WorkPropertiesUtils.getOrDefault("trawex.use.sandbox", "false"));
    
    /**
     * 用户代理字符串
     */
    public static String TRAWEX_USER_AGENT = WorkPropertiesUtils.getOrDefault(
        "trawex.user.agent", "Ali-LangEngine-Trawex/1.0");
    
    /**
     * 最大重试次数
     * 默认: 3
     */
    public static int TRAWEX_MAX_RETRIES = Integer.parseInt(
        WorkPropertiesUtils.getOrDefault("trawex.max.retries", "3"));
    
    /**
     * 重试间隔（毫秒）
     * 默认: 1000
     */
    public static int TRAWEX_RETRY_INTERVAL = Integer.parseInt(
        WorkPropertiesUtils.getOrDefault("trawex.retry.interval", "1000"));
    
    /**
     * 默认酒店搜索结果数量
     * 默认: 10
     */
    public static int TRAWEX_DEFAULT_RESULT_SIZE = Integer.parseInt(
        WorkPropertiesUtils.getOrDefault("trawex.default.result.size", "10"));
}
