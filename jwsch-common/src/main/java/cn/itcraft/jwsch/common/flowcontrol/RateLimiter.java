package cn.itcraft.jwsch.common.flowcontrol;

/**
 * 令牌桶限速器。
 *
 * <p>基于时间的令牌补充，无锁设计（单EventLoop使用）。
 * 
 * <p>工作原理：
 * <pre>
 * 1. 桶容量为 maxTokens
 * 2. 每 refillIntervalNanos 补充 refillTokens 个令牌
 * 3. tryAcquire() 消耗令牌，无令牌时返回 false
 * </pre>
 *
 * <p>配置示例（10000 msg/s，允许短时突发12000）：
 * <pre>
 * RateLimiter limiter = new RateLimiter(12000, 1000, 100_000_000L);
 * </pre>
 */
public final class RateLimiter {
    
    private final long maxTokens;
    private final long refillTokens;
    private final long refillIntervalNanos;
    
    private long availableTokens;
    private long lastRefillNanos;
    
    public RateLimiter(long maxTokens, long refillTokens, long refillIntervalNanos) {
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
        if (refillTokens <= 0) {
            throw new IllegalArgumentException("refillTokens must be positive");
        }
        if (refillIntervalNanos <= 0) {
            throw new IllegalArgumentException("refillIntervalNanos must be positive");
        }
        
        this.maxTokens = maxTokens;
        this.refillTokens = refillTokens;
        this.refillIntervalNanos = refillIntervalNanos;
        this.availableTokens = maxTokens;
        this.lastRefillNanos = System.nanoTime();
    }
    
    /**
     * 尝试获取令牌。
     *
     * @return true 表示获取成功，false 表示无可用令牌
     */
    public boolean tryAcquire() {
        return tryAcquire(1);
    }
    
    /**
     * 尝试获取指定数量令牌。
     *
     * @param permits 需要的令牌数
     * @return true 表示获取成功，false 表示无可用令牌
     */
    public boolean tryAcquire(int permits) {
        if (permits <= 0) {
            return true;
        }
        
        refill();
        
        if (availableTokens >= permits) {
            availableTokens -= permits;
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取当前可用令牌数。
     */
    public long getAvailableTokens() {
        refill();
        return availableTokens;
    }
    
    /**
     * 获取当前速率（令牌/秒）。
     */
    public double getCurrentRate() {
        return (double) refillTokens * 1_000_000_000L / refillIntervalNanos;
    }
    
    private void refill() {
        long now = System.nanoTime();
        long elapsed = now - lastRefillNanos;
        
        if (elapsed >= refillIntervalNanos) {
            long refillCycles = elapsed / refillIntervalNanos;
            long tokensToAdd = refillCycles * refillTokens;
            
            availableTokens = Math.min(maxTokens, availableTokens + tokensToAdd);
            lastRefillNanos = now;
        }
    }
    
    /**
     * 从速率创建限速器。
     *
     * @param ratePerSecond 每秒令牌数
     * @param burstSize 桶容量（允许突发）
     * @return 限速器实例
     */
    public static RateLimiter create(int ratePerSecond, int burstSize) {
        long refillIntervalNanos = 1_000_000_000L;
        return new RateLimiter(burstSize, ratePerSecond, refillIntervalNanos);
    }
}