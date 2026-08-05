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
    
    /**
     * Returns whether TCP keepalive is enabled.
     *
     * @return true if keepalive is enabled
     */
    public boolean isKeepalive() {
        return keepalive;
    }
    
    /**
     * Sets TCP keepalive enabled status.
     *
     * @param keepalive true to enable keepalive
     */
    public void setKeepalive(boolean keepalive) {
        this.keepalive = keepalive;
    }
    
    /**
     * Returns SSL configuration.
     *
     * @return SSL configuration, may be null if SSL is disabled
     */
    public SslConfig getSslConfig() {
        return sslConfig;
    }
    
    /**
     * Sets SSL configuration.
     *
     * @param sslConfig SSL configuration, null to disable SSL
     */
    public void setSslConfig(SslConfig sslConfig) {
        this.sslConfig = sslConfig;
    }
    
    /**
     * Checks if SSL is enabled.
     *
     * @return true if SSL configuration exists and is enabled
     */
    public boolean isSslEnabled() {
        return sslConfig != null && sslConfig.isEnabled();
    }
    
    /**
     * Returns idle time in seconds before sending heartbeat.
     *
     * @return idle time in seconds
     */
    public int getIdleTime() {
        return idleTime;
    }
    
    /**
     * Sets idle time in seconds before sending heartbeat.
     *
     * @param idleTime idle time in seconds, must be positive
     */
    public void setIdleTime(int idleTime) {
        if (idleTime > 0) {
            this.idleTime = idleTime;
        }
    }
    
    /**
     * Returns heartbeat timeout in seconds.
     *
     * @return heartbeat timeout in seconds
     */
    public int getHeartbeatTimeout() {
        return heartbeatTimeout;
    }
    
    /**
     * Sets heartbeat timeout in seconds.
     *
     * @param heartbeatTimeout timeout in seconds, must be positive
     */
    public void setHeartbeatTimeout(int heartbeatTimeout) {
        if (heartbeatTimeout > 0) {
            this.heartbeatTimeout = heartbeatTimeout;
        }
    }
    
    /**
     * Returns heartbeat retry count before closing connection.
     *
     * @return retry count
     */
    public int getRetryTimes() {
        return retryTimes;
    }
    
    /**
     * Sets heartbeat retry count before closing connection.
     *
     * @param retryTimes retry count, must be positive
     */
    public void setRetryTimes(int retryTimes) {
        if (retryTimes > 0) {
            this.retryTimes = retryTimes;
        }
    }
    
    /**
     * Returns cluster node hostnames or IP addresses.
     *
     * @return list of node addresses
     */
    public List<String> getNodes() {
        return nodes;
    }
    
    /**
     * Sets cluster node hostnames or IP addresses.
     *
     * @param nodes list of node addresses, null creates empty list
     */
    public void setNodes(List<String> nodes) {
        this.nodes = nodes != null ? nodes : new ArrayList<>();
    }
    
    /**
     * Returns base port number for cluster nodes.
     *
     * @return base port number
     */
    public int getBasePort() {
        return basePort;
    }
    
    /**
     * Sets base port number for cluster nodes.
     *
     * @param basePort base port number, must be positive
     */
    public void setBasePort(int basePort) {
        if (basePort > 0) {
            this.basePort = basePort;
        }
    }
    
    /**
     * Returns port range for cluster nodes.
     *
     * <p>Each node will have ports from basePort to basePort+portRange-1.
     *
     * @return port range
     */
    public int getPortRange() {
        return portRange;
    }
    
    /**
     * Sets port range for cluster nodes.
     *
     * @param portRange port range, must be positive
     */
    public void setPortRange(int portRange) {
        if (portRange > 0) {
            this.portRange = portRange;
        }
    }
    
    /**
     * Returns node selector type.
     *
     * @return selector type string: "random", "round-robin", "priority", "single"
     */
    public String getSelectorType() {
        return selectorType;
    }
    
    /**
     * Sets node selector type.
     *
     * @param selectorType selector type string: "random", "round-robin", "priority", "single"
     */
    public void setSelectorType(String selectorType) {
        this.selectorType = selectorType;
    }
    
    /**
     * Returns reconnect delay in seconds after disconnection.
     *
     * @return reconnect delay in seconds
     */
    public int getReconnectDelaySeconds() {
        return reconnectDelaySeconds;
    }
    
    /**
     * Sets reconnect delay in seconds after disconnection.
     *
     * @param reconnectDelaySeconds reconnect delay in seconds, must be positive
     */
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