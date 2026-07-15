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
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getNodePrefix() {
        return nodePrefix;
    }
    
    public void setNodePrefix(String nodePrefix) {
        this.nodePrefix = nodePrefix;
    }
    
    public int getBasePort() {
        return basePort;
    }
    
    public void setBasePort(int basePort) {
        this.basePort = basePort;
    }
    
    public int getPortRange() {
        return portRange;
    }
    
    public void setPortRange(int portRange) {
        this.portRange = portRange;
    }
    
    public int getStartupWaitSeconds() {
        return startupWaitSeconds;
    }
    
    public void setStartupWaitSeconds(int startupWaitSeconds) {
        this.startupWaitSeconds = startupWaitSeconds;
    }
    
    public int getSyncIntervalSeconds() {
        return syncIntervalSeconds;
    }
    
    public void setSyncIntervalSeconds(int syncIntervalSeconds) {
        this.syncIntervalSeconds = syncIntervalSeconds;
    }
    
    public int getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }
    
    public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }
    
    public int getHeartbeatTimeoutSeconds() {
        return heartbeatTimeoutSeconds;
    }
    
    public void setHeartbeatTimeoutSeconds(int heartbeatTimeoutSeconds) {
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
    }
    
    public int getConnectionTimeoutMs() {
        return connectionTimeoutSeconds * 1000;
    }
    
    public void setConnectionTimeoutSeconds(int connectionTimeoutSeconds) {
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
    }
    
    public List<NodeConfig> getNodes() {
        return Collections.unmodifiableList(nodes);
    }
    
    public void setNodes(List<NodeConfig> nodes) {
        this.nodes = nodes != null ? new ArrayList<>(nodes) : new ArrayList<>();
    }
    
    public int getWebsocketPort() {
        return websocketPort;
    }
    
    public void setWebsocketPort(int websocketPort) {
        this.websocketPort = websocketPort;
    }
    
    public int getHttpPort() {
        return httpPort;
    }
    
    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }
    
    public int getBindPort() {
        return bindPort;
    }
    
    public void setBindPort(int bindPort) {
        this.bindPort = bindPort;
    }
    
    public int getClusterPort() {
        return bindPort > 0 ? bindPort : basePort;
    }
    
    public String getAdvertiseHost() {
        if (advertiseHost == null) {
            advertiseHost = resolveAdvertiseHost();
        }
        return advertiseHost;
    }
    
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
