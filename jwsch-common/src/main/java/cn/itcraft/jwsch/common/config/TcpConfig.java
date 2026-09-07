package cn.itcraft.jwsch.common.config;

/**
 * TCP socket configuration options.
 * 
 * <p>Configures Netty channel options for TCP connections.
 * Used by both client and server connection factories.
 */
public class TcpConfig {
    
    /**
     * TCP_NODELAY option (default: true).
     * 
     * <p>Disables Nagle's algorithm for reduced latency.
     * Recommended for real-time messaging.
     */
    private boolean nodelay = true;
    
    /**
     * SO_SNDBUF option in bytes (default: 1MB).
     * 
     * <p>Send buffer size. Larger values improve throughput
     * for bulk data transfer at the cost of memory.
     */
    private int sndbuf = 1048576;
    
    /**
     * SO_RCVBUF option in bytes (default: 1MB).
     * 
     * <p>Receive buffer size.
     */
    private int rcvbuf = 1048576;
    
    /**
     * Write buffer watermarks (default: low=32KB, high=64KB).
     * 
     * <p>Controls when Channel.isWritable() returns false.
     * Prevents out-of-memory errors under backpressure.
     */
    private WriteBufferWaterMark writeBufferWaterMark = new WriteBufferWaterMark(32768, 65536);
    
    /**
     * Connection timeout in milliseconds (default: 30s).
     * 
     * <p>Timeout for establishing TCP connection.
     */
    private int connectTimeout = 30000;
    
    /**
     * Read timeout in milliseconds (default: 0 = disabled).
     * 
     * <p>If >0, triggers idle state event when no data is read
     * within the timeout period.
     */
    private int readTimeout = 0;
    
    /**
     * Write timeout in milliseconds (default: 0 = disabled).
     * 
     * <p>If >0, triggers idle state event when no data is written
     * within the timeout period.
     */
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