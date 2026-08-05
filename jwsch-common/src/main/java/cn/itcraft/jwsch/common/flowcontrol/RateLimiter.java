package cn.itcraft.jwsch.common.flowcontrol;

/**
 * Token bucket rate limiter.
 *
 * <p>Time-based token replenishment, lock-free design (for single EventLoop usage).
 * 
 * <p>How it works:
 * <pre>
 * 1. Bucket capacity is maxTokens
 * 2. Every refillIntervalNanos, refillTokens tokens are added
 * 3. tryAcquire() consumes tokens, returns false if insufficient tokens
 * </pre>
 *
 * <p>Configuration example (10000 msg/s, allowing short burst of 12000):
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
    
    /**
     * Creates a RateLimiter with specified parameters.
     * 
     * @param maxTokens maximum bucket capacity
     * @param refillTokens tokens added per refill interval
     * @param refillIntervalNanos refill interval in nanoseconds
     * @throws IllegalArgumentException if any parameter <= 0
     */
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
     * Attempts to acquire one token.
     *
     * @return true if token acquired, false if insufficient tokens
     */
    public boolean tryAcquire() {
        return tryAcquire(1);
    }
    
    /**
     * Attempts to acquire specified number of tokens.
     *
     * @param permits number of tokens required
     * @return true if tokens acquired, false if insufficient tokens
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
     * Gets current available token count.
     */
    public long getAvailableTokens() {
        refill();
        return availableTokens;
    }
    
    /**
     * Gets current rate (tokens per second).
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
     * Creates a RateLimiter from rate per second.
     *
     * @param ratePerSecond tokens per second
     * @param burstSize bucket capacity (allows burst)
     * @return RateLimiter instance
     */
    public static RateLimiter create(int ratePerSecond, int burstSize) {
        long refillIntervalNanos = 1_000_000_000L;
        return new RateLimiter(burstSize, ratePerSecond, refillIntervalNanos);
    }
}