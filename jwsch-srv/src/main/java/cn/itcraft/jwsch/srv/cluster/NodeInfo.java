package cn.itcraft.jwsch.srv.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NodeInfo {
    
    private final String nodeId;
    private final String host;
    private final int clusterPort;
    private final int websocketPort;
    private final int httpPort;
    private final NodeStatus status;
    private final Map<String, String> metadata;
    
    public NodeInfo(String nodeId, String host, int clusterPort, int websocketPort, int httpPort) {
        this(nodeId, host, clusterPort, websocketPort, httpPort, NodeStatus.UP, new HashMap<>());
    }
    
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
    
    public String getNodeId() {
        return nodeId;
    }
    
    public String getHost() {
        return host;
    }
    
    public int getClusterPort() {
        return clusterPort;
    }
    
    public int getWebsocketPort() {
        return websocketPort;
    }
    
    public int getHttpPort() {
        return httpPort;
    }
    
    public NodeStatus getStatus() {
        return status;
    }
    
    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    public String getClusterAddress() {
        return host + ":" + clusterPort;
    }
    
    public String getWebsocketAddress() {
        return host + ":" + websocketPort;
    }
    
    public NodeInfo withStatus(NodeStatus newStatus) {
        return new NodeInfo(nodeId, host, clusterPort, websocketPort, httpPort, newStatus, metadata);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeInfo nodeInfo = (NodeInfo) o;
        return Objects.equals(nodeId, nodeInfo.nodeId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }
    
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