package com.alibaba.langengine.docloader.dingtalk.ratelimit;

import com.alibaba.langengine.docloader.dingtalk.config.DingTalkConfig;
import com.alibaba.langengine.docloader.dingtalk.exception.DingTalkRateLimitException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;


@Slf4j
public class EnhancedDingTalkRateLimitInterceptor implements Interceptor {

    private final TokenBucketRateLimiter rateLimiter;
    private final boolean throwOnLimit;

    public EnhancedDingTalkRateLimitInterceptor(DingTalkConfig config) {
        this(config, false);
    }

    public EnhancedDingTalkRateLimitInterceptor(DingTalkConfig config, boolean throwOnLimit) {
        this.rateLimiter = new TokenBucketRateLimiter(
            config.getRateLimitPermitsPerSecond(),
            config.getRateLimitBurstCapacity(),
            config.getRateLimitAcquireTimeout()
        );
        this.throwOnLimit = throwOnLimit;
        log.info("Rate limiter initialized: {}", rateLimiter.getStats());
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        // 获取令牌
        boolean acquired = rateLimiter.acquire();

        if (!acquired) {
            String message = "Rate limit exceeded for request: " + request.url();
            log.warn(message);

            if (throwOnLimit) {
                throw new DingTalkRateLimitException(message);
            } else {
                // 返回429响应
                return new Response.Builder()
                    .request(request)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(429)
                    .message("Too Many Requests")
                    .body(okhttp3.ResponseBody.create(
                        null,
                        "{\"error\":\"Rate limit exceeded\"}"
                    ))
                    .build();
            }
        }

        // 执行请求
        Response response = chain.proceed(request);

        // 处理服务端429响应
        if (response.code() == 429) {
            log.warn("Server returned 429 for: {}", request.url());

            // 解析Retry-After头
            String retryAfterHeader = response.header("Retry-After");
            if (retryAfterHeader != null) {
                try {
                    long retryAfterSeconds = Long.parseLong(retryAfterHeader);
                    log.info("Server requested retry after {} seconds", retryAfterSeconds);

                    // 可以选择在这里等待或重置限流器
                    // rateLimiter.reset();
                } catch (NumberFormatException e) {
                    log.warn("Invalid Retry-After header: {}", retryAfterHeader);
                }
            }
        }

        // 定期记录限流器状态
        if (log.isDebugEnabled() && Math.random() < 0.01) { // 1%概率记录
            log.debug("Rate limiter stats: {}", rateLimiter.getStats());
        }

        return response;
    }

    /**
     * 获取限流器统计信息
     */
    public TokenBucketRateLimiter.RateLimiterStats getStats() {
        return rateLimiter.getStats();
    }

    /**
     * 重置限流器
     */
    public void reset() {
        rateLimiter.reset();
    }
}
