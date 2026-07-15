package cn.itcraft.jwsch.cli.config;

import cn.itcraft.jwsch.common.config.TcpConfig;
import cn.itcraft.jwsch.common.ssl.SslConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * TCP client configuration.
 *
 * <p>Extends TcpConfig with client-specific settings:
 * <ul>
 *   <li>Cluster configuration: nodes, basePort, portRange, selectorType</li>
 *   <li>Connection management: idleTime, heartbeatTimeout, retryTimes</li>
 *   <li>SSL configuration</li>
 * </ul>
 *
 * <p>Cluster mode configuration:
 * <pre>
 * nodes: ["server1", "server2"]
 * basePort: 9090
 * portRange: 2
 * selectorType: "round-robin"
 * </pre>
 *
 * <p>This expands to addresses: server1:9090, server1:9091, server2:9090, server2:9091
 */
public final class TcpClientConfig extends TcpConfig {
    
    private static final int DEFAULT_IDLE_TIME = 30;
    private static final int DEFAULT_HEARTBEAT_TIMEOUT = 30;
    private static final int DEFAULT_RETRY_TIMES = 3;
    private static final int DEFAULT_RECONNECT_DELAY_SECONDS = 5;
    
    private boolean keepalive = true;
    private SslConfig sslConfig;
    private int idleTime = DEFAULT_IDLE_TIME;
    private int heartbeatTimeout = DEFAULT_HEARTBEAT_TIMEOUT;
    private int retryTimes = DEFAULT_RETRY_TIMES;
    
    private List<String> nodes = new ArrayList<>();
    private int basePort = 9090;
    private int portRange = 1;
    private String selectorType = "random";
    private int reconnectDelaySeconds = DEFAULT_RECONNECT_DELAY_SECONDS;
    
    public boolean isKeepalive() {
        return keepalive;
    }
    
    public void setKeepalive(boolean keepalive) {
        this.keepalive = keepalive;
    }
    
    public SslConfig getSslConfig() {
        return sslConfig;
    }
    
    public void setSslConfig(SslConfig sslConfig) {
        this.sslConfig = sslConfig;
    }
    
    public boolean isSslEnabled() {
        return sslConfig != null && sslConfig.isEnabled();
    }
    
    public int getIdleTime() {
        return idleTime;
    }
    
    public void setIdleTime(int idleTime) {
        if (idleTime > 0) {
            this.idleTime = idleTime;
        }
    }
    
    public int getHeartbeatTimeout() {
        return heartbeatTimeout;
    }
    
    public void setHeartbeatTimeout(int heartbeatTimeout) {
        if (heartbeatTimeout > 0) {
            this.heartbeatTimeout = heartbeatTimeout;
        }
    }
    
    public int getRetryTimes() {
        return retryTimes;
    }
    
    public void setRetryTimes(int retryTimes) {
        if (retryTimes > 0) {
            this.retryTimes = retryTimes;
        }
    }
    
    public List<String> getNodes() {
        return nodes;
    }
    
    public void setNodes(List<String> nodes) {
        this.nodes = nodes != null ? nodes : new ArrayList<>();
    }
    
    public int getBasePort() {
        return basePort;
    }
    
    public void setBasePort(int basePort) {
        if (basePort > 0) {
            this.basePort = basePort;
        }
    }
    
    public int getPortRange() {
        return portRange;
    }
    
    public void setPortRange(int portRange) {
        if (portRange > 0) {
            this.portRange = portRange;
        }
    }
    
    public String getSelectorType() {
        return selectorType;
    }
    
    public void setSelectorType(String selectorType) {
        this.selectorType = selectorType;
    }
    
    public int getReconnectDelaySeconds() {
        return reconnectDelaySeconds;
    }
    
    public void setReconnectDelaySeconds(int reconnectDelaySeconds) {
        if (reconnectDelaySeconds > 0) {
            this.reconnectDelaySeconds = reconnectDelaySeconds;
        }
    }
    
    /**
     * Check if cluster mode is enabled.
     *
     * <p>Cluster mode is enabled when nodes list is not empty.
     */
    public boolean isClusterEnabled() {
        return nodes != null && !nodes.isEmpty();
    }
}