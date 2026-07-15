package cn.itcraft.jwsch.srv.config;

public class TcpServerConfig {
    
    private int port = 9090;
    private int bossThreads = 1;
    private int workerThreads = 4;
    private int connectTimeout = 30000;
    private int readTimeout = 0;
    private int writeTimeout = 0;
    private int soBacklog = 1024;
    private boolean tcpNoDelay = true;
    private boolean keepAlive = true;
    private int sndbuf = 0;
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