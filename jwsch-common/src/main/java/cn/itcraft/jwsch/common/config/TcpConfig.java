package cn.itcraft.jwsch.common.config;

import cn.itcraft.jwsch.common.protocol.ProtocolConsts;

/**
 * TCP socket configuration options.
 * 
 * <p>Configures Netty channel options for TCP connections.
 * Used by both client and server connection factories.
 * 
 * <p>Packet size constraints:
 * <ul>
 *   <li>Soft limit ({@value ProtocolConsts#DEFAULT_MAX_PACKET_LENGTH}): default max packet
 *       total length (header + body), adjustable via configuration</li>
 *   <li>Hard limit ({@value ProtocolConsts#MAX_PACKET_LENGTH_LIMIT}): hardcoded ceiling;
 *       any configured value above it is force-clamped to it</li>
 * </ul>
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
    
    /**
     * 数据包总长度软上限（默认 200KB）。
     * 
     * <p>超过此上限的包在收发两端被丢弃。
     * 设置值大于硬上限（{@value ProtocolConsts#MAX_PACKET_LENGTH_LIMIT}）时
     * 强制钳制为硬上限；非正值恢复默认。
     */
    private int maxPacketLength = ProtocolConsts.DEFAULT_MAX_PACKET_LENGTH;
    
    /**
     * 是否打印过大数据包的前 200 字节内容（默认 false）。
     * 
     * <p>启用后丢包内容进入有界队列，由独立的超限日志线程解析打印。
     */
    private boolean logOversizeContent = false;
    
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
    
    public int getMaxPacketLength() {
        return maxPacketLength;
    }
    
    public void setMaxPacketLength(int maxPacketLength) {
        this.maxPacketLength = normalizePacketLimit(maxPacketLength);
    }
    
    /**
     * 钳制包上限：超过硬上限强制回落为硬上限；非正值回落默认值。
     */
    public boolean isLogOversizeContent() {
        return logOversizeContent;
    }
    
    public void setLogOversizeContent(boolean logOversizeContent) {
        this.logOversizeContent = logOversizeContent;
    }
    
    public static int normalizePacketLimit(int value) {
        if (value <= 0) {
            return ProtocolConsts.DEFAULT_MAX_PACKET_LENGTH;
        }
        if (value > ProtocolConsts.MAX_PACKET_LENGTH_LIMIT) {
            return ProtocolConsts.MAX_PACKET_LENGTH_LIMIT;
        }
        return value;
    }
}