package cn.itcraft.jwsch.common.exception;

/**
 * Base exception class for jwsch framework.
 * 
 * <p>All jwsch-specific exceptions extend this class.
 * Contains an {@link ErrorCode} for programmatic error handling.
 * 
 * <p>Use this exception for recoverable errors where callers can
 * inspect errorCode and take appropriate action.
 */
public class JwschException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    /**
     * Creates a JwschException with the specified error code.
     * 
     * <p>The exception message defaults to the error code's description.
     * 
     * @param errorCode error code
     */
    public JwschException(ErrorCode errorCode) {
        super(errorCode.getDesc());
        this.errorCode = errorCode;
    }
    
    /**
     * Creates a JwschException with error code and custom message.
     * 
     * @param errorCode error code
     * @param message custom error message
     */
    public JwschException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * Creates a JwschException with error code, custom message, and cause.
     * 
     * @param errorCode error code
     * @param message custom error message
     * @param cause underlying cause
     */
    public JwschException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * Returns the error code associated with this exception.
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}