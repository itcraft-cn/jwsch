package cn.itcraft.jwsch.common.exception;

public final class RouteException extends JwschException {
    
    public RouteException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public RouteException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public RouteException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}