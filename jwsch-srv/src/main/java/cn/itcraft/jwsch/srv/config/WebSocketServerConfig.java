package cn.itcraft.jwsch.srv.config;

import cn.itcraft.jwsch.common.ssl.SslConfig;

/**
 * WebSocket server configuration for YAML/JSON deserialization.
 *
 * <p>This is a mutable configuration class used for external configuration
 * loading (e.g., from YAML files). It contains WebSocket server settings
 * including SSL/TLS configuration.
 */
public class WebSocketServerConfig {
    
    public static final int DEFAULT_MAX_FRAME_SIZE = 65536;
    public static final int MAX_FRAME_SIZE = 512 * 1024;
    
    /** WebSocket server port (default: 8080) */
    private int port = 8080;
    /** WebSocket endpoint path (default: "/ws") */
    private String path = "/ws";
    /** Number of boss threads for Netty event loop (default: 1) */
    private int bossThreads = 1;
    /** Number of worker threads for Netty event loop (0 = auto-detect, default: 0) */
    private int workerThreads = 0;
    /** Maximum WebSocket frame size in bytes (default: 65536) */
    private int maxFrameSize = DEFAULT_MAX_FRAME_SIZE;
    /** Whether TCP_NODELAY option is enabled (default: true) */
    private boolean tcpNoDelay = true;
    /** Whether SO_KEEPALIVE option is enabled (default: true) */
    private boolean keepAlive = true;
    /** Server socket backlog size (default: 1024) */
    private int soBacklog = 1024;
    /** SSL/TLS configuration (null if SSL is disabled, default: null) */
    private SslConfig sslConfig;
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public int getBossThreads() {
        return bossThreads;
    }
    
    public void setBossThreads(int bossThreads) {
        this.bossThreads = bossThreads;
    }
    
    public int getWorkerThreads() {
        return workerThreads;
    }
    
    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }
    
    public int getMaxFrameSize() {
        return maxFrameSize;
    }
    
    public void setMaxFrameSize(int maxFrameSize) {
        this.maxFrameSize = Math.max(1, Math.min(MAX_FRAME_SIZE, maxFrameSize));
    }
    
    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }
    
    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }
    
    public boolean isKeepAlive() {
        return keepAlive;
    }
    
    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }
    
    public int getSoBacklog() {
        return soBacklog;
    }
    
    public void setSoBacklog(int soBacklog) {
        this.soBacklog = soBacklog;
    }
    
    public SslConfig getSslConfig() {
        return sslConfig;
    }
    
    public void setSslConfig(SslConfig sslConfig) {
        this.sslConfig = sslConfig;
    }
    
    public boolean isSslEnabled() {
        return sslConfig != null && sslConfig.isEnabled();
    }
}