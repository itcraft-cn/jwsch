package cn.itcraft.jwsch.common.exception;

public final class RegistryException extends JwschException {
    
    public RegistryException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public RegistryException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public RegistryException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}