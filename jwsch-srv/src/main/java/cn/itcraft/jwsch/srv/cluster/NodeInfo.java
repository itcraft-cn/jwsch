package cn.itcraft.jwsch.srv.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Information about a cluster node.
 * 
 * <p>Contains node identity, network addresses, status, and metadata.
 * Immutable value object used throughout cluster management.
 */
public class NodeInfo {
    
    private final String nodeId;
    private final String host;
    private final int clusterPort;
    private final int websocketPort;
    private final int httpPort;
    private final NodeStatus status;
    private final Map<String, String> metadata;
    
    /**
     * Creates a node information object with default status (UP) and empty metadata.
     * 
     * @param nodeId node identifier
     * @param host hostname or IP address
     * @param clusterPort cluster communication port
     * @param websocketPort WebSocket service port
     * @param httpPort HTTP API port
     */
    public NodeInfo(String nodeId, String host, int clusterPort, int websocketPort, int httpPort) {
        this(nodeId, host, clusterPort, websocketPort, httpPort, NodeStatus.UP, new HashMap<>());
    }
    
    /**
     * Creates a node information object with all fields.
     * 
     * @param nodeId node identifier
     * @param host hostname or IP address
     * @param clusterPort cluster communication port
     * @param websocketPort WebSocket service port
     * @param httpPort HTTP API port
     * @param status node status (null defaults to UP)
     * @param metadata metadata map (null defaults to empty)
     */
    public NodeInfo(String nodeId, String host, int clusterPort, int websocketPort, int httpPort, 
                    NodeStatus status, Map<String, String> metadata) {
        this.nodeId = nodeId;
        this.host = host;
        this.clusterPort = clusterPort;
        this.websocketPort = websocketPort;
        this.httpPort = httpPort;
        this.status = status != null ? status : NodeStatus.UP;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    /**
     * Returns the node identifier.
     * 
     * @return node ID
     */
    public String getNodeId() {
        return nodeId;
    }
    
    /**
     * Returns the hostname or IP address.
     * 
     * @return host
     */
    public String getHost() {
        return host;
    }
    
    /**
     * Returns the cluster communication port.
     * 
     * @return cluster port
     */
    public int getClusterPort() {
        return clusterPort;
    }
    
    /**
     * Returns the WebSocket service port.
     * 
     * @return WebSocket port
     */
    public int getWebsocketPort() {
        return websocketPort;
    }
    
    /**
     * Returns the HTTP API port.
     * 
     * @return HTTP port
     */
    public int getHttpPort() {
        return httpPort;
    }
    
    /**
     * Returns the node status.
     * 
     * @return node status
     */
    public NodeStatus getStatus() {
        return status;
    }
    
    /**
     * Returns a copy of the metadata map.
     * 
     * @return metadata map copy
     */
    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    /**
     * Returns the cluster address (host:clusterPort).
     * 
     * @return cluster address
     */
    public String getClusterAddress() {
        return host + ":" + clusterPort;
    }
    
    /**
     * Returns the WebSocket address (host:websocketPort).
     * 
     * @return WebSocket address
     */
    public String getWebsocketAddress() {
        return host + ":" + websocketPort;
    }
    
    /**
     * Creates a copy with a different status.
     * 
     * @param newStatus new node status
     * @return new NodeInfo instance with updated status
     */
    public NodeInfo withStatus(NodeStatus newStatus) {
        return new NodeInfo(nodeId, host, clusterPort, websocketPort, httpPort, newStatus, metadata);
    }
    
    /**
     * {@inheritDoc}
     * <p>Nodes are equal if they have the same nodeId.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeInfo nodeInfo = (NodeInfo) o;
        return Objects.equals(nodeId, nodeInfo.nodeId);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "NodeInfo{" +
            "nodeId='" + nodeId + '\'' +
            ", host='" + host + '\'' +
            ", clusterPort=" + clusterPort +
            ", websocketPort=" + websocketPort +
            ", httpPort=" + httpPort +
            ", status=" + status +
            '}';
    }
}