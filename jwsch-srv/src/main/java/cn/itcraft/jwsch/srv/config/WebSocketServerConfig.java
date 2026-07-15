package cn.itcraft.jwsch.srv.config;

import cn.itcraft.jwsch.common.ssl.SslConfig;

public class WebSocketServerConfig {
    
    public static final int DEFAULT_MAX_FRAME_SIZE = 65536;
    public static final int MAX_FRAME_SIZE = 512 * 1024;
    
    private int port = 8080;
    private String path = "/ws";
    private int bossThreads = 1;
    private int workerThreads = 0;
    private int maxFrameSize = DEFAULT_MAX_FRAME_SIZE;
    private boolean tcpNoDelay = true;
    private boolean keepAlive = true;
    private int soBacklog = 1024;
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