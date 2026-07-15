package cn.itcraft.jwsch.srv.config;

public final class TcpConfig {
    
    private final int port;
    private final int bossThreads;
    private final int workerThreads;
    private final int connectTimeout;
    private final boolean tcpNoDelay;
    private final boolean keepAlive;
    private final int soBacklog;
    
    private TcpConfig(Builder builder) {
        this.port = builder.port;
        this.bossThreads = builder.bossThreads;
        this.workerThreads = builder.workerThreads;
        this.connectTimeout = builder.connectTimeout;
        this.tcpNoDelay = builder.tcpNoDelay;
        this.keepAlive = builder.keepAlive;
        this.soBacklog = builder.soBacklog;
    }
    
    public int getPort() { return port; }
    public int getBossThreads() { return bossThreads; }
    public int getWorkerThreads() { return workerThreads; }
    public int getConnectTimeout() { return connectTimeout; }
    public boolean isTcpNoDelay() { return tcpNoDelay; }
    public boolean isKeepAlive() { return keepAlive; }
    public int getSoBacklog() { return soBacklog; }
    
    public static final class Builder {
        private int port = 9090;
        private int bossThreads = 1;
        private int workerThreads = 4;
        private int connectTimeout = 30000;
        private boolean tcpNoDelay = true;
        private boolean keepAlive = true;
        private int soBacklog = 1024;
        
        public Builder port(int port) { this.port = port; return this; }
        public Builder bossThreads(int bossThreads) { this.bossThreads = bossThreads; return this; }
        public Builder workerThreads(int workerThreads) { this.workerThreads = workerThreads; return this; }
        public Builder connectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; return this; }
        public Builder tcpNoDelay(boolean tcpNoDelay) { this.tcpNoDelay = tcpNoDelay; return this; }
        public Builder keepAlive(boolean keepAlive) { this.keepAlive = keepAlive; return this; }
        public Builder soBacklog(int soBacklog) { this.soBacklog = soBacklog; return this; }
        
        public TcpConfig build() { return new TcpConfig(this); }
    }
    
    public static Builder builder() { return new Builder(); }
}