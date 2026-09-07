package cn.itcraft.jwsch.common.exception;

/**
 * Exception thrown when protocol-level errors occur.
 * 
 * <p>Examples include:
 * <ul>
 *   <li>Invalid packet format (magic, length, etc.)</li>
 *   <li>Decoding/encoding failures</li>
 *   <li>Invalid command or version</li>
 *   <li>Protocol constraint violations</li>
 * </ul>
 */
public final class ProtocolException extends JwschException {
    
    /**
     * Creates a ProtocolException with error code.
     * 
     * @param errorCode protocol error code (e.g., {@link ErrorCode#INVALID_MAGIC})
     */
    public ProtocolException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    /**
     * Creates a ProtocolException with error code and custom message.
     */
    public ProtocolException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    /**
     * Creates a ProtocolException with error code, custom message, and cause.
     */
    public ProtocolException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}