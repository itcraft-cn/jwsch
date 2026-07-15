package cn.itcraft.jwsch.common.config;

public class TcpConfig {
    
    private boolean nodelay = true;
    private int sndbuf = 1048576;
    private int rcvbuf = 1048576;
    private WriteBufferWaterMark writeBufferWaterMark = new WriteBufferWaterMark(32768, 65536);
    private int connectTimeout = 30000;
    private int readTimeout = 0;
    private int writeTimeout = 0;
    
    public boolean isNodelay() {
        return nodelay;
    }
    
    public void setNodelay(boolean nodelay) {
        this.nodelay = nodelay;
    }
    
    public int getSndbuf() {
        return sndbuf;
    }
    
    public void setSndbuf(int sndbuf) {
        this.sndbuf = sndbuf;
    }
    
    public int getRcvbuf() {
        return rcvbuf;
    }
    
    public void setRcvbuf(int rcvbuf) {
        this.rcvbuf = rcvbuf;
    }
    
    public WriteBufferWaterMark getWriteBufferWaterMark() {
        return writeBufferWaterMark;
    }
    
    public void setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark) {
        this.writeBufferWaterMark = writeBufferWaterMark;
    }
    
    public int getConnectTimeout() {
        return connectTimeout;
    }
    
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
    
    public int getReadTimeout() {
        return readTimeout;
    }
    
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
    
    public int getWriteTimeout() {
        return writeTimeout;
    }
    
    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }
}