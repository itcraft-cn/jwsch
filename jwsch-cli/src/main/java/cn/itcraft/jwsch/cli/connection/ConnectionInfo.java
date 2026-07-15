package cn.itcraft.jwsch.cli.connection;

import java.util.Objects;

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
    
    public long getConnectionId() {
        return connectionId;
    }
    
    public String getRemoteAddress() {
        return remoteAddress;
    }
    
    public String getLocalAddress() {
        return localAddress;
    }
    
    public ConnectionType getConnectionType() {
        return connectionType;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public long getLastActiveTime() {
        return lastActiveTime;
    }
    
    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }
    
    public ConnectionStatus getStatus() {
        return status;
    }
    
    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }
    
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
    
    public static class Builder {
        private long connectionId;
        private String remoteAddress;
        private String localAddress;
        private ConnectionType connectionType;
        private String serviceName;
        private long createTime;
        private long lastActiveTime;
        private ConnectionStatus status;
        
        public Builder connectionId(long connectionId) {
            this.connectionId = connectionId;
            return this;
        }
        
        public Builder remoteAddress(String remoteAddress) {
            this.remoteAddress = remoteAddress;
            return this;
        }
        
        public Builder localAddress(String localAddress) {
            this.localAddress = localAddress;
            return this;
        }
        
        public Builder connectionType(ConnectionType connectionType) {
            this.connectionType = connectionType;
            return this;
        }
        
        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }
        
        public Builder createTime(long createTime) {
            this.createTime = createTime;
            return this;
        }
        
        public Builder lastActiveTime(long lastActiveTime) {
            this.lastActiveTime = lastActiveTime;
            return this;
        }
        
        public Builder status(ConnectionStatus status) {
            this.status = status;
            return this;
        }
        
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