package cn.itcraft.jwsch.common.exception;

/**
 * Exception thrown when routing operations fail.
 * 
 * <p>Examples include:
 * <ul>
 *   <li>No route found for destination</li>
 *   <li>Routing table inconsistency</li>
 *   <li>Load balancing failures</li>
 *   <li>Route timeout</li>
 * </ul>
 */
public final class RouteException extends JwschException {
    
    /**
     * Creates a RouteException with error code.
     * 
     * @param errorCode routing error code (e.g., {@link ErrorCode#ROUTE_FAILED})
     */
    public RouteException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    /**
     * Creates a RouteException with error code and custom message.
     */
    public RouteException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    /**
     * Creates a RouteException with error code, custom message, and cause.
     */
    public RouteException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}