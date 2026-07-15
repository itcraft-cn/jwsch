package cn.itcraft.jwsch.srv.flowcontrol;

/**
 * 速率限制超出异常。
 *
 * <p>当入站流量超过限速阈值时抛出。
 */
public final class RateLimitExceededException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public RateLimitExceededException(String message) {
        super(message);
    }
    
    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}