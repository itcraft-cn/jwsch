package cn.itcraft.jwsch.srv.config;

public class ServerConfig {
    
    private boolean enabled = true;
    private WebSocketServerConfig webSocketConfig = new WebSocketServerConfig();
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