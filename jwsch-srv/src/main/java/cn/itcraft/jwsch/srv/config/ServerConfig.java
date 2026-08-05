package cn.itcraft.jwsch.srv.config;

/**
 * Server configuration container for YAML/JSON deserialization.
 *
 * <p>This is a mutable configuration class used for external configuration
 * loading (e.g., from YAML files). It contains WebSocket and TCP server
 * configurations.
 */
public class ServerConfig {
    
    /** Whether the server is enabled */
    private boolean enabled = true;
    /** WebSocket server configuration */
    private WebSocketServerConfig webSocketConfig = new WebSocketServerConfig();
    /** TCP server configuration */
    private TcpServerConfig tcpConfig = new TcpServerConfig();
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public WebSocketServerConfig getWebSocketConfig() {
        return webSocketConfig;
    }
    
    public void setWebSocketConfig(WebSocketServerConfig webSocketConfig) {
        this.webSocketConfig = webSocketConfig;
    }
    
    public TcpServerConfig getTcpConfig() {
        return tcpConfig;
    }
    
    public void setTcpConfig(TcpServerConfig tcpConfig) {
        this.tcpConfig = tcpConfig;
    }
}