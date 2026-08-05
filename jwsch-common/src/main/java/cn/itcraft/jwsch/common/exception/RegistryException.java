/**
 * Exception thrown when service registry operations fail.
 * 
 * <p>Examples include:
 * <ul>
 *   <li>Service registration/deregistration failures</li>
 *   <li>Service discovery failures</li>
 *   <li>Registry connection issues</li>
 *   <li>Subscription management failures</li>
 * </ul>
 */
public final class RegistryException extends JwschException {
    
    /**
     * Creates a RegistryException with error code.
     * 
     * @param errorCode registry error code (e.g., {@link ErrorCode#REGISTER_FAILED})
     */
    public RegistryException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    /**
     * Creates a RegistryException with error code and custom message.
     */
    public RegistryException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    /**
     * Creates a RegistryException with error code, custom message, and cause.
     */
    public RegistryException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}