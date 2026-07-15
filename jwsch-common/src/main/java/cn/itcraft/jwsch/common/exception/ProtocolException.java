package cn.itcraft.jwsch.common.exception;

public final class ProtocolException extends JwschException {
    
    public ProtocolException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public ProtocolException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public ProtocolException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}