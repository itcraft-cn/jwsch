package cn.itcraft.jwsch.cli.connection;

import java.util.Objects;

/**
 * Connection information container.
 *
 * <p>Immutable container for connection metadata, with Builder pattern.
 * Tracks connection lifecycle and status.
 *
 * <p>Fields:
 * <ul>
 *   <li>connectionId: Unique connection identifier</li>
 *   <li>remoteAddress: Remote endpoint address</li>
 *   <li>localAddress: Local endpoint address</li>
 *   <li>connectionType: Connection type (CLIENT/SERVER)</li>
 *   <li>serviceName: Service name for routing</li>
 *   <li>createTime: Connection creation timestamp</li>
 *   <li>lastActiveTime: Last activity timestamp</li>
 *   <li>status: Current connection status</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 * ConnectionInfo info = new ConnectionInfo.Builder()
 *     .connectionId(12345L)
 *     .remoteAddress("192.168.1.100:8080")
 *     .connectionType(ConnectionType.CLIENT)
 *     .serviceName("chat-service")
 *     .status(ConnectionStatus.CONNECTED)
 *     .build();
 * </pre>
 */
public final class ConnectionInfo {
    
    private final long connectionId;
    private final String remoteAddress;
    private final String localAddress;
    private final ConnectionType connectionType;
    private final String serviceName;
    private final long createTime;
    private volatile long lastActiveTime;
    private volatile ConnectionStatus status;
    
    private ConnectionInfo(Builder builder) {
        this.connectionId = builder.connectionId;
        this.remoteAddress = builder.remoteAddress;
        this.localAddress = builder.localAddress;
        this.connectionType = builder.connectionType;
        this.serviceName = builder.serviceName;
        this.createTime = builder.createTime;
        this.lastActiveTime = builder.lastActiveTime;
        this.status = builder.status;
    }
    
    /**
     * Returns connection identifier.
     */
    public long getConnectionId() {
        return connectionId;
    }
    
    /**
     * Returns remote address (IP:port).
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }
    
    /**
     * Returns local address (IP:port).
     */
    public String getLocalAddress() {
        return localAddress;
    }
    
    /**
     * Returns connection type.
     */
    public ConnectionType getConnectionType() {
        return connectionType;
    }
    
    /**
     * Returns service name for routing.
     */
    public String getServiceName() {
        return serviceName;
    }
    
    /**
     * Returns creation timestamp (milliseconds).
     */
    public long getCreateTime() {
        return createTime;
    }
    
    /**
     * Returns last activity timestamp (milliseconds).
     */
    public long getLastActiveTime() {
        return lastActiveTime;
    }
    
    /**
     * Sets last activity timestamp.
     *
     * @param lastActiveTime timestamp in milliseconds
     */
    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }
    
    /**
     * Returns current connection status.
     */
    public ConnectionStatus getStatus() {
        return status;
    }
    
    /**
     * Updates connection status.
     *
     * @param status new status
     */
    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }
    
    /**
     * Updates last active time to current time.
     */
    public void updateActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "ConnectionInfo{" +
            "connectionId=" + connectionId +
            ", remoteAddress='" + remoteAddress + '\'' +
            ", localAddress='" + localAddress + '\'' +
            ", connectionType=" + connectionType +
            ", serviceName='" + serviceName + '\'' +
            ", status=" + status +
            '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionInfo that = (ConnectionInfo) o;
        return connectionId == that.connectionId;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(connectionId);
    }
    
    /**
     * Builder for ConnectionInfo.
     */
    public static class Builder {
        private long connectionId;
        private String remoteAddress;
        private String localAddress;
        private ConnectionType connectionType;
        private String serviceName;
        private long createTime;
        private long lastActiveTime;
        private ConnectionStatus status;
        
        /**
         * Sets connection identifier.
         */
        public Builder connectionId(long connectionId) {
            this.connectionId = connectionId;
            return this;
        }
        
        /**
         * Sets remote address (IP:port).
         */
        public Builder remoteAddress(String remoteAddress) {
            this.remoteAddress = remoteAddress;
            return this;
        }
        
        /**
         * Sets local address (IP:port).
         */
        public Builder localAddress(String localAddress) {
            this.localAddress = localAddress;
            return this;
        }
        
        /**
         * Sets connection type.
         */
        public Builder connectionType(ConnectionType connectionType) {
            this.connectionType = connectionType;
            return this;
        }
        
        /**
         * Sets service name.
         */
        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }
        
        /**
         * Sets creation timestamp.
         */
        public Builder createTime(long createTime) {
            this.createTime = createTime;
            return this;
        }
        
        /**
         * Sets last activity timestamp.
         */
        public Builder lastActiveTime(long lastActiveTime) {
            this.lastActiveTime = lastActiveTime;
            return this;
        }
        
        /**
         * Sets connection status.
         */
        public Builder status(ConnectionStatus status) {
            this.status = status;
            return this;
        }
        
        /**
         * Builds ConnectionInfo.
         *
         * @throws NullPointerException if connectionType or status is null
         */
        public ConnectionInfo build() {
            Objects.requireNonNull(connectionType, "connectionType cannot be null");
            Objects.requireNonNull(status, "status cannot be null");
            
            if (createTime == 0) {
                createTime = System.currentTimeMillis();
            }
            if (lastActiveTime == 0) {
                lastActiveTime = createTime;
            }
            
            return new ConnectionInfo(this);
        }
    }
}