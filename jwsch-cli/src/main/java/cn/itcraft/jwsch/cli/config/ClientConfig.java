package cn.itcraft.jwsch.cli.config;

/**
 * Client configuration container.
 *
 * <p>Top-level configuration for TCP client, aggregating:
 * <ul>
 *   <li>Enabled status</li>
 *   <li>EventLoop configuration</li>
 *   <li>TCP-specific client configuration</li>
 * </ul>
 *
 * <p>Default values:
 * <ul>
 *   <li>enabled: true</li>
 *   <li>eventLoopConfig: default EventLoopConfig</li>
 *   <li>tcpConfig: default TcpClientConfig</li>
 * </ul>
 *
 * <p>Used by TcpClient for initialization and lifecycle management.
 */
public final class ClientConfig {
    
    private boolean enabled = true;
    private EventLoopConfig eventLoopConfig = new EventLoopConfig();
    private TcpClientConfig tcpConfig = new TcpClientConfig();
    
    /**
     * Returns whether the client is enabled.
     *
     * @return true if client is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Sets client enabled status.
     *
     * @param enabled true to enable client
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Returns event loop configuration.
     *
     * @return event loop configuration
     */
    public EventLoopConfig getEventLoopConfig() {
        return eventLoopConfig;
    }
    
    /**
     * Sets event loop configuration.
     *
     * @param eventLoopConfig event loop configuration
     */
    public void setEventLoopConfig(EventLoopConfig eventLoopConfig) {
        this.eventLoopConfig = eventLoopConfig;
    }
    
    /**
     * Returns TCP client configuration.
     *
     * @return TCP client configuration
     */
    public TcpClientConfig getTcpConfig() {
        return tcpConfig;
    }
    
    /**
     * Sets TCP client configuration.
     *
     * @param tcpConfig TCP client configuration
     */
    public void setTcpConfig(TcpClientConfig tcpConfig) {
        this.tcpConfig = tcpConfig;
    }
}