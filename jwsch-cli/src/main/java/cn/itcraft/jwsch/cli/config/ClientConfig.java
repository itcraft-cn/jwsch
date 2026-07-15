package cn.itcraft.jwsch.cli.config;

public final class ClientConfig {
    
    private boolean enabled = true;
    private EventLoopConfig eventLoopConfig = new EventLoopConfig();
    private TcpClientConfig tcpConfig = new TcpClientConfig();
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public EventLoopConfig getEventLoopConfig() {
        return eventLoopConfig;
    }
    
    public void setEventLoopConfig(EventLoopConfig eventLoopConfig) {
        this.eventLoopConfig = eventLoopConfig;
    }
    
    public TcpClientConfig getTcpConfig() {
        return tcpConfig;
    }
    
    public void setTcpConfig(TcpClientConfig tcpConfig) {
        this.tcpConfig = tcpConfig;
    }
}