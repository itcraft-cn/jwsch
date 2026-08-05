package cn.itcraft.jwsch.srv.config;

/**
 * TCP server configuration for YAML/JSON deserialization.
 *
 * <p>This is a mutable configuration class used for external configuration
 * loading (e.g., from YAML files). It contains TCP server settings including
 * socket options and timeouts.
 */
public class TcpServerConfig {
    
    /** TCP server port (default: 9090) */
    private int port = 9090;
    /** Number of boss threads for Netty event loop (default: 1) */
    private int bossThreads = 1;
    /** Number of worker threads for Netty event loop (default: 4) */
    private int workerThreads = 4;
    /** Connection timeout in milliseconds (default: 30000) */
    private int connectTimeout = 30000;
    /** Read timeout in milliseconds (0 = disabled, default: 0) */
    private int readTimeout = 0;
    /** Write timeout in milliseconds (0 = disabled, default: 0) */
    private int writeTimeout = 0;
    /** Server socket backlog size (default: 1024) */
    private int soBacklog = 1024;
    /** Whether TCP_NODELAY option is enabled (default: true) */
    private boolean tcpNoDelay = true;
    /** Whether SO_KEEPALIVE option is enabled (default: true) */
    private boolean keepAlive = true;
    /** Socket send buffer size in bytes (0 = use system default, default: 0) */
    private int sndbuf = 0;
    /** Socket receive buffer size in bytes (0 = use system default, default: 0) */
    private int rcvbuf = 0;
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
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
    
    public int getSoBacklog() {
        return soBacklog;
    }
    
    public void setSoBacklog(int soBacklog) {
        this.soBacklog = soBacklog;
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
}