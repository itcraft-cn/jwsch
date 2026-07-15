package cn.itcraft.jwsch.common.config;

public final class ConfigValidator {
    
    private ConfigValidator() {
    }
    
    public static void validatePort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 0 and 65535");
        }
    }
    
    public static void validateBacklog(int backlog) {
        if (backlog < 1 || backlog > 65535) {
            throw new IllegalArgumentException("Backlog must be between 1 and 65535");
        }
    }
    
    public static void validateBufferSize(int bufferSize) {
        if (bufferSize < 1024 || bufferSize > 16777216) {
            throw new IllegalArgumentException("Buffer size must be between 1024 and 16777216");
        }
    }
    
    public static void validateTimeout(int timeout) {
        if (timeout < 0 || timeout > 86400) {
            throw new IllegalArgumentException("Timeout must be between 0 and 86400");
        }
    }
}