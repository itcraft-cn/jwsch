package cn.itcraft.jwsch.srv.config;

public final class MetricsConfig {
    
    private final boolean enabled;
    private final int port;
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