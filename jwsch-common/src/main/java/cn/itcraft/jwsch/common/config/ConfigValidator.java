/**
 * Configuration validation utilities.
 * 
 * <p>Provides static methods to validate common configuration values
 * with meaningful error messages.
 */
public final class ConfigValidator {
    
    private ConfigValidator() {
    }
    
    /**
     * Validates TCP/UDP port number.
     * 
     * @param port port number to validate
     * @throws IllegalArgumentException if port < 0 or > 65535
     */
    public static void validatePort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 0 and 65535");
        }
    }
    
    /**
     * Validates server socket backlog size.
     * 
     * @param backlog backlog size to validate
     * @throws IllegalArgumentException if backlog < 1 or > 65535
     */
    public static void validateBacklog(int backlog) {
        if (backlog < 1 || backlog > 65535) {
            throw new IllegalArgumentException("Backlog must be between 1 and 65535");
        }
    }
    
    /**
     * Validates buffer size for network operations.
     * 
     * @param bufferSize buffer size in bytes to validate
     * @throws IllegalArgumentException if bufferSize < 1024 or > 16MB
     */
    public static void validateBufferSize(int bufferSize) {
        if (bufferSize < 1024 || bufferSize > 16777216) {
            throw new IllegalArgumentException("Buffer size must be between 1024 and 16777216");
        }
    }
    
    /**
     * Validates timeout value in seconds.
     * 
     * @param timeout timeout in seconds to validate
     * @throws IllegalArgumentException if timeout < 0 or > 86400 (24 hours)
     */
    public static void validateTimeout(int timeout) {
        if (timeout < 0 || timeout > 86400) {
            throw new IllegalArgumentException("Timeout must be between 0 and 86400");
        }
    }
}