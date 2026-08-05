package cn.itcraft.jwsch.srv.flowcontrol;

/**
 * Rate limit exceeded exception.
 *
 * <p>Thrown when inbound traffic exceeds the rate limiting threshold.
 * This exception is used in the L1 (inbound rate limiting) layer
 * of the flow control system.
 */
public final class RateLimitExceededException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates a RateLimitExceededException with the specified detail message.
     *
     * @param message the detail message
     */
    public RateLimitExceededException(String message) {
        super(message);
    }
    
    /**
     * Creates a RateLimitExceededException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}