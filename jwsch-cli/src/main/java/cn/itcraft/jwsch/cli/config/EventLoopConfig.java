package cn.itcraft.jwsch.cli.config;

public final class EventLoopConfig {
    
    private boolean shared = true;
    private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;
    
    public boolean isShared() {
        return shared;
    }
    
    public void setShared(boolean shared) {
        this.shared = shared;
    }
    
    public int getWorkerThreads() {
        return workerThreads;
    }
    
    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }
}