package cn.itcraft.jwsch.srv.cluster;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

/**
 * Cluster configuration for jwsch server.
 * 
 * <p>Configuration is split into two parts:
 * <ul>
 *   <li>Cluster-wide settings (from config file): enabled, node-prefix, base-port, port-range, nodes</li>
 *   <li>Node-specific settings (from env/JVM): advertise-host</li>
 * </ul>
 * 
 * <p>Node ID format: {node-prefix}-{advertise-host}-{bind-port}
 * 
 * <p>Advertise host priority: JVM param > env var > auto-detect
 * <ul>
 *   <li>JVM param: -Djwsch.advertise.host=192.168.1.10</li>
 *   <li>Env var: JWSCH_ADVERTISE_HOST=192.168.1.10</li>
 *   <li>Auto-detect: first non-loopback address</li>
 * </ul>
 */
public class ClusterConfig {
    
    private static final String ENV_ADVERTISE_HOST = "JWSCH_ADVERTISE_HOST";
    private static final String JVM_ADVERTISE_HOST = "jwsch.advertise.host";
    
    private boolean enabled;
    private String nodePrefix;
    private int basePort;
    private int portRange;
    private int startupWaitSeconds;
    private int syncIntervalSeconds;
    private int heartbeatIntervalSeconds;
    private int heartbeatTimeoutSeconds;
    private int connectionTimeoutSeconds;
    private List<NodeConfig> nodes;
    
    private int websocketPort;
    private int httpPort;
    
    private volatile String advertiseHost;
    private volatile int bindPort;
    
    public ClusterConfig() {
        this.enabled = false;
        this.nodePrefix = "jwsch";
        this.basePort = 9090;
        this.portRange = 3;
        this.startupWaitSeconds = 5;
        this.syncIntervalSeconds = 30;
        this.heartbeatIntervalSeconds = 10;
        this.heartbeatTimeoutSeconds = 30;
        this.connectionTimeoutSeconds = 30;
        this.nodes = new ArrayList<>();
        this.websocketPort = 8080;
        this.httpPort = 8081;
        this.bindPort = -1;
    }
    
    /**
     * Returns whether cluster mode is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Sets whether cluster mode is enabled.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Returns node prefix for node ID generation.
     */
    public String getNodePrefix() {
        return nodePrefix;
    }
    
    /**
     * Sets node prefix for node ID generation.
     */
    public void setNodePrefix(String nodePrefix) {
        this.nodePrefix = nodePrefix;
    }
    
    /**
     * Returns base port for cluster communication.
     */
    public int getBasePort() {
        return basePort;
    }
    
    /**
     * Sets base port for cluster communication.
     */
    public void setBasePort(int basePort) {
        this.basePort = basePort;
    }
    
    /**
     * Returns port range for dynamic port allocation.
     */
    public int getPortRange() {
        return portRange;
    }
    
    /**
     * Sets port range for dynamic port allocation.
     */
    public void setPortRange(int portRange) {
        this.portRange = portRange;
    }
    
    /**
     * Returns startup wait time in seconds.
     */
    public int getStartupWaitSeconds() {
        return startupWaitSeconds;
    }
    
    /**
     * Sets startup wait time in seconds.
     */
    public void setStartupWaitSeconds(int startupWaitSeconds) {
        this.startupWaitSeconds = startupWaitSeconds;
    }
    
    /**
     * Returns cluster sync interval in seconds.
     */
    public int getSyncIntervalSeconds() {
        return syncIntervalSeconds;
    }
    
    /**
     * Sets cluster sync interval in seconds.
     */
    public void setSyncIntervalSeconds(int syncIntervalSeconds) {
        this.syncIntervalSeconds = syncIntervalSeconds;
    }
    
    /**
     * Returns heartbeat interval in seconds.
     */
    public int getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }
    
    /**
     * Sets heartbeat interval in seconds.
     */
    public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }
    
    /**
     * Returns heartbeat timeout in seconds.
     */
    public int getHeartbeatTimeoutSeconds() {
        return heartbeatTimeoutSeconds;
    }
    
    /**
     * Sets heartbeat timeout in seconds.
     */
    public void setHeartbeatTimeoutSeconds(int heartbeatTimeoutSeconds) {
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
    }
    
    /**
     * Returns connection timeout in milliseconds.
     */
    public int getConnectionTimeoutMs() {
        return connectionTimeoutSeconds * 1000;
    }
    
    /**
     * Sets connection timeout in seconds.
     */
    public void setConnectionTimeoutSeconds(int connectionTimeoutSeconds) {
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
    }
    
    /**
     * Returns cluster node configurations.
     */
    public List<NodeConfig> getNodes() {
        return Collections.unmodifiableList(nodes);
    }
    
    /**
     * Sets cluster node configurations.
     */
    public void setNodes(List<NodeConfig> nodes) {
        this.nodes = nodes != null ? new ArrayList<>(nodes) : new ArrayList<>();
    }
    
    /**
     * Returns WebSocket server port.
     */
    public int getWebsocketPort() {
        return websocketPort;
    }
    
    /**
     * Sets WebSocket server port.
     */
    public void setWebsocketPort(int websocketPort) {
        this.websocketPort = websocketPort;
    }
    
    /**
     * Returns HTTP server port.
     */
    public int getHttpPort() {
        return httpPort;
    }
    
    /**
     * Sets HTTP server port.
     */
    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }
    
    /**
     * Returns bind port for cluster communication.
     */
    public int getBindPort() {
        return bindPort;
    }
    
    /**
     * Sets bind port for cluster communication.
     */
    public void setBindPort(int bindPort) {
        this.bindPort = bindPort;
    }
    
    /**
     * Returns cluster port (bind port if set, otherwise base port).
     */
    public int getClusterPort() {
        return bindPort > 0 ? bindPort : basePort;
    }
    
    /**
     * Returns advertise host (JVM param > env var > auto-detected).
     */
    public String getAdvertiseHost() {
        if (advertiseHost == null) {
            advertiseHost = resolveAdvertiseHost();
        }
        return advertiseHost;
    }
    
    /**
     * Sets advertise host (overrides auto-detection).
     */
    public void setAdvertiseHost(String advertiseHost) {
        this.advertiseHost = advertiseHost;
    }
    
    /**
     * Get node ID: {node-prefix}-{advertise-host}-{bind-port}
     */
    public String getNodeId() {
        return nodePrefix + "-" + getAdvertiseHost() + "-" + getClusterPort();
    }
    
    /**
     * Resolve advertise host from JVM param > env var > auto-detect.
     */
    private String resolveAdvertiseHost() {
        String jvmHost = System.getProperty(JVM_ADVERTISE_HOST);
        if (jvmHost != null && !jvmHost.isEmpty()) {
            return jvmHost;
        }
        
        String envHost = System.getenv(ENV_ADVERTISE_HOST);
        if (envHost != null && !envHost.isEmpty()) {
            return envHost;
        }
        
        return autoDetectHost();
    }
    
    /**
     * Auto-detect first non-loopback address.
     */
    private String autoDetectHost() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.isLoopbackAddress()) {
                        continue;
                    }
                    String host = addr.getHostAddress();
                    if (host != null && !host.isEmpty()) {
                        return host;
                    }
                }
            }
        } catch (Exception e) {
        }
        return "127.0.0.1";
    }
    
    /**
     * Convert to NodeInfo for cluster communication.
     */
    public NodeInfo toNodeInfo() {
        return new NodeInfo(
            getNodeId(),
            getAdvertiseHost(),
            getClusterPort(),
            websocketPort,
            httpPort
        );
    }
    
    @Override
    public String toString() {
        return "ClusterConfig{" +
            "enabled=" + enabled +
            ", nodePrefix='" + nodePrefix + '\'' +
            ", basePort=" + basePort +
            ", portRange=" + portRange +
            ", bindPort=" + bindPort +
            ", advertiseHost='" + getAdvertiseHost() + '\'' +
            ", nodeId='" + getNodeId() + '\'' +
            ", nodes=" + nodes.size() +
            '}';
    }
    
    /**
     * Node configuration for cluster membership.
     */
    public static final class NodeConfig {
        
        private final String host;
        
        public NodeConfig() {
            this.host = null;
        }
        
        public NodeConfig(String host) {
            this.host = host;
        }
        
        /**
         * Returns host address for cluster node.
         */
        public String getHost() {
            return host;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NodeConfig that = (NodeConfig) o;
            return Objects.equals(host, that.host);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(host);
        }
        
        @Override
        public String toString() {
            return "NodeConfig{host='" + host + '\'' + '}';
        }
    }
}
