package cn.itcraft.jwsch.srv.cluster;

/**
 * Represents a connection that is located on a remote cluster node.
 * 
 * <p>Used by cluster connection registry to track connections across nodes.
 * Contains both connection identifier and the node where it resides.
 */
public class RemoteConnection {
    
    private final long connectionId;
    private final String nodeId;
    private final String nodeAddress;
    private final ConnectionMeta connectionMeta;
    
    /**
     * Creates a remote connection with minimal information.
     * 
     * @param connectionId connection identifier
     * @param nodeId node where the connection resides
     */
    public RemoteConnection(long connectionId, String nodeId) {
        this(connectionId, nodeId, null, null);
    }
    
    /**
     * Creates a remote connection with full information.
     * 
     * @param connectionId connection identifier
     * @param nodeId node where the connection resides
     * @param nodeAddress node address (host:port)
     * @param connectionMeta connection metadata (may be null)
     */
    public RemoteConnection(long connectionId, String nodeId, String nodeAddress, 
                            ConnectionMeta connectionMeta) {
        this.connectionId = connectionId;
        this.nodeId = nodeId;
        this.nodeAddress = nodeAddress;
        this.connectionMeta = connectionMeta;
    }
    
    /**
     * Returns the connection identifier.
     * 
     * @return connection ID
     */
    public long getConnectionId() {
        return connectionId;
    }
    
    /**
     * Returns the node identifier where the connection resides.
     * 
     * @return node ID
     */
    public String getNodeId() {
        return nodeId;
    }
    
    /**
     * Returns the node address (host:port).
     * 
     * @return node address, or {@code null} if unknown
     */
    public String getNodeAddress() {
        return nodeAddress;
    }
    
    /**
     * Returns the connection metadata.
     * 
     * @return connection metadata, or {@code null} if unknown
     */
    public ConnectionMeta getConnectionMeta() {
        return connectionMeta;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "RemoteConnection{" +
            "connectionId=" + connectionId +
            ", nodeId='" + nodeId + '\'' +
            ", nodeAddress='" + nodeAddress + '\'' +
            '}';
    }
}