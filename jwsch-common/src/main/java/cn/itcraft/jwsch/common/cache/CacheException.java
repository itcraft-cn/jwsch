package cn.itcraft.jwsch.common.cache;

/**
 * Exception thrown by cache operations.
 *
 * <p>Common causes:
 * <ul>
 *   <li>CacheLoader failure during get-with-loader operations</li>
 *   <li>Cache initialization errors</li>
 *   <li>Cache eviction failures</li>
 * </ul>
 */
public final class CacheException extends RuntimeException {
    
    /**
     * Creates a CacheException with message.
     *
     * @param message error message
     */
    public CacheException(String message) {
        super(message);
    }
    
    /**
     * Creates a CacheException with message and cause.
     *
     * @param message error message
     * @param cause underlying cause
     */
    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }
}