/**
 * Exception thrown when connection-related errors occur.
 * 
 * <p>Examples include:
 * <ul>
 *   <li>Connection closed unexpectedly</li>
 *   <li>Connection timeout</li>
 *   <li>Connection refused by remote</li>
 *   <li>Connection reset</li>
 *   <li>Too many connections</li>
 *   <li>Heartbeat timeout</li>
 * </ul>
 */
public final class ConnectionException extends JwschException {
    
    /**
     * Creates a ConnectionException with error code.
     * 
     * @param errorCode connection error code (e.g., {@link ErrorCode#CONNECTION_CLOSED})
     */
    public ConnectionException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    /**
     * Creates a ConnectionException with error code and custom message.
     */
    public ConnectionException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    /**
     * Creates a ConnectionException with error code, custom message, and cause.
     */
    public ConnectionException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}