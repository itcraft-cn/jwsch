package cn.itcraft.jwsch.cli.config;

/**
 * Event loop configuration for Netty client.
 *
 * <p>Controls how Netty EventLoop resources are allocated:
 * <ul>
 *   <li>Shared: multiple clients share a single EventLoopGroup</li>
 *   <li>Dedicated: each client creates its own EventLoopGroup</li>
 *   <li>Worker threads: number of I/O threads for dedicated mode</li>
 * </ul>
 *
 * <p>Default values:
 * <ul>
 *   <li>shared: true (recommended for production)</li>
 *   <li>workerThreads: CPU cores × 2</li>
 * </ul>
 */
public final class EventLoopConfig {
    
    private boolean shared = true;
    private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;
    
    /**
     * Returns whether EventLoopGroup is shared among clients.
     *
     * @return true if EventLoopGroup is shared
     */
    public boolean isShared() {
        return shared;
    }
    
    /**
     * Sets whether EventLoopGroup is shared among clients.
     *
     * @param shared true to share EventLoopGroup
     */
    public void setShared(boolean shared) {
        this.shared = shared;
    }
    
    /**
     * Returns number of worker threads for dedicated EventLoopGroup.
     *
     * @return worker thread count
     */
    public int getWorkerThreads() {
        return workerThreads;
    }
    
    /**
     * Sets number of worker threads for dedicated EventLoopGroup.
     *
     * @param workerThreads worker thread count
     */
    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }
}