package cn.itcraft.jwsch.srv.config;

public final class HealthConfig {
    
    private final boolean enabled;
    private final int port;
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