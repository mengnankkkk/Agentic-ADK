package com.alibaba.langengine.docloader.dingtalk.ratelimit;

import com.alibaba.langengine.docloader.dingtalk.exception.DingTalkRateLimitException;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j
public class TokenBucketRateLimiter {

    private final double permitsPerSecond;
    private final int maxBurstCapacity;
    private final Duration acquireTimeout;

    private double availableTokens;
    private long lastRefillTimestamp;
    private final Lock lock = new ReentrantLock();

    public TokenBucketRateLimiter(double permitsPerSecond, int maxBurstCapacity, Duration acquireTimeout) {
        if (permitsPerSecond <= 0) {
            throw new IllegalArgumentException("Permits per second must be positive");
        }
        if (maxBurstCapacity <= 0) {
            throw new IllegalArgumentException("Max burst capacity must be positive");
        }

        this.permitsPerSecond = permitsPerSecond;
        this.maxBurstCapacity = maxBurstCapacity;
        this.acquireTimeout = acquireTimeout;
        this.availableTokens = maxBurstCapacity;
        this.lastRefillTimestamp = System.nanoTime();
    }

    /**
     * 获取1个令牌，阻塞直到获取成功或超时
     *
     * @return true if acquired, false if timeout
     */
    public boolean acquire() {
        return acquire(1);
    }

    /**
     * 获取指定数量的令牌
     *
     * @param permits 需要的令牌数
     * @return true if acquired, false if timeout
     */
    public boolean acquire(int permits) {
        if (permits <= 0) {
            return true;
        }

        long deadline = System.nanoTime() + acquireTimeout.toNanos();

        while (true) {
            lock.lock();
            try {
                refillTokens();

                if (availableTokens >= permits) {
                    availableTokens -= permits;
                    log.trace("Acquired {} permits, remaining: {}", permits, availableTokens);
                    return true;
                }

                // 计算需要等待的时间
                double tokensNeeded = permits - availableTokens;
                long waitTimeNanos = (long) (tokensNeeded / permitsPerSecond * 1_000_000_000);

                // 检查是否会超时
                if (System.nanoTime() + waitTimeNanos > deadline) {
                    log.warn("Rate limit acquisition timeout, needed {} permits but only {} available",
                        permits, availableTokens);
                    return false;
                }

            } finally {
                lock.unlock();
            }

            // 等待一小段时间后重试
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    /**
     * 尝试获取令牌，不阻塞
     *
     * @return true if acquired immediately, false otherwise
     */
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    /**
     * 尝试获取指定数量的令牌，不阻塞
     *
     * @param permits 需要的令牌数
     * @return true if acquired immediately, false otherwise
     */
    public boolean tryAcquire(int permits) {
        if (permits <= 0) {
            return true;
        }

        lock.lock();
        try {
            refillTokens();

            if (availableTokens >= permits) {
                availableTokens -= permits;
                log.trace("Acquired {} permits immediately, remaining: {}", permits, availableTokens);
                return true;
            }

            log.trace("Cannot acquire {} permits immediately, only {} available", permits, availableTokens);
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取令牌或抛出异常
     *
     * @throws DingTalkRateLimitException if cannot acquire within timeout
     */
    public void acquireOrThrow() {
        if (!acquire()) {
            throw new DingTalkRateLimitException(
                "Rate limit exceeded: cannot acquire permit within " + acquireTimeout.toMillis() + "ms"
            );
        }
    }

    /**
     * 补充令牌
     */
    private void refillTokens() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastRefillTimestamp;

        if (elapsedNanos > 0) {
            double newTokens = (elapsedNanos / 1_000_000_000.0) * permitsPerSecond;
            availableTokens = Math.min(maxBurstCapacity, availableTokens + newTokens);
            lastRefillTimestamp = now;

            log.trace("Refilled {} tokens, now available: {}/{}", newTokens, availableTokens, maxBurstCapacity);
        }
    }

    /**
     * 获取当前可用令牌数
     */
    public double getAvailableTokens() {
        lock.lock();
        try {
            refillTokens();
            return availableTokens;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 重置限流器
     */
    public void reset() {
        lock.lock();
        try {
            availableTokens = maxBurstCapacity;
            lastRefillTimestamp = System.nanoTime();
            log.info("Rate limiter reset");
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取限流器统计信息
     */
    public RateLimiterStats getStats() {
        lock.lock();
        try {
            refillTokens();
            return new RateLimiterStats(
                permitsPerSecond,
                maxBurstCapacity,
                availableTokens,
                (maxBurstCapacity - availableTokens) / maxBurstCapacity * 100
            );
        } finally {
            lock.unlock();
        }
    }

    /**
     * 限流器统计信息
     */
    public static class RateLimiterStats {
        public final double permitsPerSecond;
        public final int maxCapacity;
        public final double availableTokens;
        public final double usagePercent;

        public RateLimiterStats(double permitsPerSecond, int maxCapacity,
                               double availableTokens, double usagePercent) {
            this.permitsPerSecond = permitsPerSecond;
            this.maxCapacity = maxCapacity;
            this.availableTokens = availableTokens;
            this.usagePercent = usagePercent;
        }

        @Override
        public String toString() {
            return String.format("RateLimiter[rate=%.1f/s, capacity=%d, available=%.1f, usage=%.1f%%]",
                permitsPerSecond, maxCapacity, availableTokens, usagePercent);
        }
    }
}
