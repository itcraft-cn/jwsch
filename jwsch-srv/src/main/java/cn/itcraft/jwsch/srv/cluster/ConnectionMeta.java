package cn.itcraft.jwsch.srv.cluster;

public class ConnectionMeta {
    
    private final long connectionId;
    private final String connectionType;
    private final String remoteAddress;
    private final long createTime;
    private volatile long lastActiveTime;
    
    public ConnectionMeta(long connectionId, String connectionType, String remoteAddress) {
        this.connectionId = connectionId;
        this.connectionType = connectionType;
        this.remoteAddress = remoteAddress;
        this.createTime = System.currentTimeMillis();
        this.lastActiveTime = this.createTime;
    }
    
    public long getConnectionId() {
        return connectionId;
    }
    
    public String getConnectionType() {
        return connectionType;
    }
    
    public String getRemoteAddress() {
        return remoteAddress;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public long getLastActiveTime() {
        return lastActiveTime;
    }
    
    public void updateLastActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }
    
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