package cn.itcraft.jwsch.srv.cluster;

public class RemoteConnection {
    
    private final long connectionId;
    private final String nodeId;
    private final String nodeAddress;
    private final ConnectionMeta connectionMeta;
    
    public RemoteConnection(long connectionId, String nodeId) {
        this(connectionId, nodeId, null, null);
    }
    
    public RemoteConnection(long connectionId, String nodeId, String nodeAddress, 
                            ConnectionMeta connectionMeta) {
        this.connectionId = connectionId;
        this.nodeId = nodeId;
        this.nodeAddress = nodeAddress;
        this.connectionMeta = connectionMeta;
    }
    
    public long getConnectionId() {
        return connectionId;
    }
    
    public String getNodeId() {
        return nodeId;
    }
    
    public String getNodeAddress() {
        return nodeAddress;
    }
    
    public ConnectionMeta getConnectionMeta() {
        return connectionMeta;
    }
    
    @Override
    public String toString() {
        return "RemoteConnection{" +
            "connectionId=" + connectionId +
            ", nodeId='" + nodeId + '\'' +
            ", nodeAddress='" + nodeAddress + '\'' +
            '}';
    }
}