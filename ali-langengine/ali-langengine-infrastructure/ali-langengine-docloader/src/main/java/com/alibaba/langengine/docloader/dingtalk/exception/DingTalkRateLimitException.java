package com.alibaba.langengine.docloader.dingtalk.exception;


public class DingTalkRateLimitException extends DingTalkException {

    private final Long retryAfterSeconds;

    public DingTalkRateLimitException(String message) {
        super(message, "RATE_LIMIT_EXCEEDED", 429);
        this.retryAfterSeconds = null;
    }

    public DingTalkRateLimitException(String message, Long retryAfterSeconds) {
        super(message, "RATE_LIMIT_EXCEEDED", 429);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public DingTalkRateLimitException(String message, Throwable cause) {
        super(message, "RATE_LIMIT_EXCEEDED", 429, cause);
        this.retryAfterSeconds = null;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    @Override
    public String toString() {
        String base = super.toString();
        if (retryAfterSeconds != null) {
            return base + " [retryAfter=" + retryAfterSeconds + "s]";
        }
        return base;
    }
}
