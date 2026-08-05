package cn.itcraft.jwsch.srv.config;

/**
 * Metrics endpoint configuration.
 *
 * <p>Uses Builder pattern to create immutable configuration:
 * <pre>
 * MetricsConfig config = MetricsConfig.builder()
 *     .enabled(true)
 *     .port(8082)
 *     .path("/metrics")
 *     .build();
 * </pre>
 */
public final class MetricsConfig {
    
    /** Whether metrics endpoint is enabled */
    private final boolean enabled;
    /** Metrics server port */
    private final int port;
    /** Metrics endpoint path */
    private final String path;
    
    private MetricsConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.port = builder.port;
        this.path = builder.path;
    }
    
    public boolean isEnabled() { return enabled; }
    public int getPort() { return port; }
    public String getPath() { return path; }
    
    public static final class Builder {
        private boolean enabled = false;
        private int port = 8082;
        private String path = "/metrics";
        
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder port(int port) { this.port = port; return this; }
        public Builder path(String path) { this.path = path; return this; }
        
        public MetricsConfig build() { return new MetricsConfig(this); }
    }
    
    public static Builder builder() { return new Builder(); }
}