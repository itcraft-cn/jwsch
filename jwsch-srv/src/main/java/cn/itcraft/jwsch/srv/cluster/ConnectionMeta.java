package cn.itcraft.jwsch.srv.cluster;

/**
 * Metadata for a connection (either WebSocket or TCP).
 * 
 * <p>Tracks connection identity, type, remote address, and activity timestamps.
 * Used by cluster connection registry to map connections to nodes.
 */
public class ConnectionMeta {
    
    private final long connectionId;
    private final String connectionType;
    private final String remoteAddress;
    private final long createTime;
    private volatile long lastActiveTime;
    
    /**
     * Creates connection metadata.
     * 
     * @param connectionId unique connection identifier
     * @param connectionType connection type ("ws" or "tcp")
     * @param remoteAddress remote address (IP:port)
     */
    public ConnectionMeta(long connectionId, String connectionType, String remoteAddress) {
        this.connectionId = connectionId;
        this.connectionType = connectionType;
        this.remoteAddress = remoteAddress;
        this.createTime = System.currentTimeMillis();
        this.lastActiveTime = this.createTime;
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
     * Returns the connection type.
     * 
     * @return connection type ("ws" for WebSocket, "tcp" for TCP)
     */
    public String getConnectionType() {
        return connectionType;
    }
    
    /**
     * Returns the remote address.
     * 
     * @return remote address (IP:port)
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }
    
    /**
     * Returns the creation timestamp.
     * 
     * @return creation time in milliseconds since epoch
     */
    public long getCreateTime() {
        return createTime;
    }
    
    /**
     * Returns the last active timestamp.
     * 
     * @return last active time in milliseconds since epoch
     */
    public long getLastActiveTime() {
        return lastActiveTime;
    }
    
    /**
     * Updates the last active timestamp to current time.
     */
    public void updateLastActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }
    
    /**
     * Returns string representation of connection metadata.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        return "ConnectionMeta{" +
            "connectionId=" + connectionId +
            ", connectionType='" + connectionType + '\'' +
            ", remoteAddress='" + remoteAddress + '\'' +
            ", createTime=" + createTime +
            ", lastActiveTime=" + lastActiveTime +
            '}';
    }
}