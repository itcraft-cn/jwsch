package cn.itcraft.jwsch.srv.config;

/**
 * Health check endpoint configuration.
 *
 * <p>Uses Builder pattern to create immutable configuration:
 * <pre>
 * HealthConfig config = HealthConfig.builder()
 *     .enabled(true)
 *     .port(8081)
 *     .host("0.0.0.0")
 *     .build();
 * </pre>
 */
public final class HealthConfig {
    
    /** Whether health check endpoint is enabled */
    private final boolean enabled;
    /** Health check server port */
    private final int port;
    /** Health check server bind host */
    private final String host;
    
    private HealthConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.port = builder.port;
        this.host = builder.host;
    }
    
    public boolean isEnabled() { return enabled; }
    public int getPort() { return port; }
    public String getHost() { return host; }
    
    public static final class Builder {
        private boolean enabled = false;
        private int port = 8081;
        private String host = "0.0.0.0";
        
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder port(int port) { this.port = port; return this; }
        public Builder host(String host) { this.host = host; return this; }
        
        public HealthConfig build() { return new HealthConfig(this); }
    }
    
    public static Builder builder() { return new Builder(); }
}