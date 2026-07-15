package cn.itcraft.jwsch.common.exception;

public final class ConnectionException extends JwschException {
    
    public ConnectionException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public ConnectionException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public ConnectionException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}