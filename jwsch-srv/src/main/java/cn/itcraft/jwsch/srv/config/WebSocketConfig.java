package cn.itcraft.jwsch.srv.config;

import cn.itcraft.jwsch.common.ssl.SslConfig;

/**
 * WebSocket server configuration.
 *
 * <p>Uses Builder pattern to create immutable configuration:
 * <pre>
 * WebSocketConfig config = WebSocketConfig.builder()
 *     .port(8080)
 *     .path("/ws")
 *     .bossThreads(1)
 *     .workerThreads(0)
 *     .maxFrameSize(65536)
 *     .tcpNoDelay(true)
 *     .keepAlive(true)
 *     .soBacklog(1024)
 *     .sslConfig(null)
 *     .build();
 * </pre>
 */
public final class WebSocketConfig {
    
    public static final int DEFAULT_MAX_FRAME_SIZE = 65536;
    public static final int MAX_FRAME_SIZE = 512 * 1024;
    
    /** WebSocket server port */
    private final int port;
    /** WebSocket endpoint path */
    private final String path;
    /** Number of boss threads for Netty event loop */
    private final int bossThreads;
    /** Number of worker threads for Netty event loop (0 = auto-detect) */
    private final int workerThreads;
    /** Maximum WebSocket frame size in bytes */
    private final int maxFrameSize;
    /** Whether TCP_NODELAY option is enabled */
    private final boolean tcpNoDelay;
    /** Whether SO_KEEPALIVE option is enabled */
    private final boolean keepAlive;
    /** Server socket backlog size */
    private final int soBacklog;
    /** SSL/TLS configuration (null if SSL is disabled) */
    private final SslConfig sslConfig;
    
    private WebSocketConfig(Builder builder) {
        this.port = builder.port;
        this.path = builder.path;
        this.bossThreads = builder.bossThreads;
        this.workerThreads = builder.workerThreads;
        this.maxFrameSize = builder.maxFrameSize;
        this.tcpNoDelay = builder.tcpNoDelay;
        this.keepAlive = builder.keepAlive;
        this.soBacklog = builder.soBacklog;
        this.sslConfig = builder.sslConfig;
    }
    
    public int getPort() { return port; }
    public String getPath() { return path; }
    public int getBossThreads() { return bossThreads; }
    public int getWorkerThreads() { return workerThreads; }
    public int getMaxFrameSize() { return maxFrameSize; }
    public boolean isTcpNoDelay() { return tcpNoDelay; }
    public boolean isKeepAlive() { return keepAlive; }
    public int getSoBacklog() { return soBacklog; }
    public SslConfig getSslConfig() { return sslConfig; }
    
    public static final class Builder {
        private int port = 8080;
        private String path = "/ws";
        private int bossThreads = 1;
        private int workerThreads = 0;
        private int maxFrameSize = DEFAULT_MAX_FRAME_SIZE;
        private boolean tcpNoDelay = true;
        private boolean keepAlive = true;
        private int soBacklog = 1024;
        private SslConfig sslConfig = null;
        
        public Builder port(int port) { this.port = port; return this; }
        public Builder path(String path) { this.path = path; return this; }
        public Builder bossThreads(int bossThreads) { this.bossThreads = bossThreads; return this; }
        public Builder workerThreads(int workerThreads) { this.workerThreads = workerThreads; return this; }
        public Builder maxFrameSize(int maxFrameSize) { 
            this.maxFrameSize = Math.max(1, Math.min(MAX_FRAME_SIZE, maxFrameSize));
            return this; 
        }
        public Builder tcpNoDelay(boolean tcpNoDelay) { this.tcpNoDelay = tcpNoDelay; return this; }
        public Builder keepAlive(boolean keepAlive) { this.keepAlive = keepAlive; return this; }
        public Builder soBacklog(int soBacklog) { this.soBacklog = soBacklog; return this; }
        public Builder sslConfig(SslConfig sslConfig) { this.sslConfig = sslConfig; return this; }
        
        public WebSocketConfig build() { return new WebSocketConfig(this); }
    }
    
    public static Builder builder() { return new Builder(); }
}