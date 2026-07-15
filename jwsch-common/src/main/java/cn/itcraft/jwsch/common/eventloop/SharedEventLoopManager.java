package cn.itcraft.jwsch.common.eventloop;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 共享 EventLoop 管理器。
 * 
 * <p>支持两种模式：
 * <ul>
 *   <li>服务端模式：bossGroup + workerGroup，用于 Netty Server</li>
 *   <li>客户端模式：workerGroup only，用于 Netty Client</li>
 * </ul>
 * 
 * <p>使用引用计数管理生命周期，支持多个组件共享同一组 EventLoop。
 * 
 * <p>服务端使用示例：
 * <pre>
 * SharedEventLoopManager manager = SharedEventLoopManager.createServer(1, 4);
 * manager.acquire();
 * ServerBootstrap bootstrap = new ServerBootstrap()
 *     .group(manager.getBossGroup(), manager.getWorkerGroup());
 * // ... 服务关闭后
 * manager.release();
 * manager.shutdown();
 * </pre>
 * 
 * <p>客户端使用示例：
 * <pre>
 * SharedEventLoopManager manager = SharedEventLoopManager.getClientInstance();
 * EventLoopGroup workerGroup = manager.acquireWorkerGroup();
 * Bootstrap bootstrap = new Bootstrap().group(workerGroup);
 * // ... 客户端关闭后
 * manager.release();
 * </pre>
 */
public final class SharedEventLoopManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedEventLoopManager.class);
    
    private static final int SHUTDOWN_QUIET_PERIOD_MS = 100;
    private static final int SHUTDOWN_TIMEOUT_MS = 300;
    
    private static final String DEFAULT_BOSS_THREAD_NAME = "jwsch-boss";
    private static final String DEFAULT_WORKER_THREAD_NAME = "jwsch-worker";
    private static final String CLIENT_THREAD_NAME = "jwsch-shared-worker";
    
    private static volatile SharedEventLoopManager clientInstance;
    
    private final String bossThreadName;
    private final String workerThreadName;
    private final int bossThreads;
    private final int workerThreads;
    private final boolean serverMode;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final AtomicInteger refCount;
    private final Object lock;
    private volatile boolean initialized;
    private volatile boolean shutdown;
    
    private SharedEventLoopManager(int bossThreads, int workerThreads, 
                                   String bossThreadName, String workerThreadName, 
                                   boolean serverMode) {
        this.bossThreads = bossThreads;
        this.workerThreads = workerThreads > 0 ? workerThreads : Runtime.getRuntime().availableProcessors() * 2;
        this.bossThreadName = bossThreadName != null ? bossThreadName : DEFAULT_BOSS_THREAD_NAME;
        this.workerThreadName = workerThreadName != null ? workerThreadName : DEFAULT_WORKER_THREAD_NAME;
        this.serverMode = serverMode;
        this.refCount = new AtomicInteger(0);
        this.lock = new Object();
        this.initialized = false;
        this.shutdown = false;
    }
    
    /**
     * 获取客户端模式单例。
     * 
     * <p>客户端模式只有 workerGroup，延迟初始化。
     * 
     * @return 客户端 SharedEventLoopManager 实例
     */
    public static SharedEventLoopManager getClientInstance() {
        if (clientInstance == null) {
            synchronized (SharedEventLoopManager.class) {
                if (clientInstance == null) {
                    clientInstance = new SharedEventLoopManager(
                        0, 
                        Runtime.getRuntime().availableProcessors() * 2, 
                        null, 
                        CLIENT_THREAD_NAME, 
                        false
                    );
                }
            }
        }
        return clientInstance;
    }
    
    /**
     * 创建服务端模式实例。
     * 
     * <p>服务端模式有 bossGroup + workerGroup，立即初始化。
     * 
     * @param bossThreads boss 线程数
     * @param workerThreads worker 线程数
     * @return 服务端 SharedEventLoopManager 实例
     */
    public static SharedEventLoopManager createServer(int bossThreads, int workerThreads) {
        return createServer(bossThreads, workerThreads, null, null);
    }
    
    /**
     * 创建服务端模式实例（自定义线程名）。
     * 
     * @param bossThreads boss 线程数
     * @param workerThreads worker 线程数
     * @param bossThreadName boss 线程名前缀
     * @param workerThreadName worker 线程名前缀
     * @return 服务端 SharedEventLoopManager 实例
     */
    public static SharedEventLoopManager createServer(int bossThreads, int workerThreads, 
                                                      String bossThreadName, String workerThreadName) {
        SharedEventLoopManager manager = new SharedEventLoopManager(
            bossThreads, workerThreads, bossThreadName, workerThreadName, true
        );
        manager.initialize();
        return manager;
    }
    
    private void initialize() {
        if (initialized) {
            return;
        }
        
        synchronized (lock) {
            if (initialized) {
                return;
            }
            
            if (serverMode) {
                bossGroup = new NioEventLoopGroup(bossThreads, 
                    new DefaultThreadFactory(bossThreadName, true));
            }
            
            workerGroup = new NioEventLoopGroup(workerThreads, 
                new DefaultThreadFactory(workerThreadName, true));
            
            initialized = true;
            
            LOGGER.info("SharedEventLoopManager initialized: serverMode={}, bossThreads={}, workerThreads={}", 
                serverMode, serverMode ? bossThreads : 0, workerThreads);
        }
    }
    
    /**
     * 获取 worker EventLoopGroup（客户端模式）。
     * 
     * <p>自动初始化并增加引用计数。
     * 
     * @return worker EventLoopGroup
     */
    public EventLoopGroup acquireWorkerGroup() {
        if (shutdown) {
            throw new IllegalStateException("SharedEventLoopManager has been shutdown");
        }
        
        synchronized (lock) {
            if (!initialized) {
                initialize();
            }
            refCount.incrementAndGet();
            LOGGER.debug("Acquired worker EventLoopGroup, refCount={}", refCount.get());
            return workerGroup;
        }
    }
    
    /**
     * 增加引用计数（服务端模式）。
     * 
     * <p>调用此方法前应确保已初始化。
     */
    public void acquire() {
        if (shutdown) {
            throw new IllegalStateException("SharedEventLoopManager has been shutdown");
        }
        
        synchronized (lock) {
            if (!initialized) {
                initialize();
            }
            int count = refCount.incrementAndGet();
            LOGGER.debug("SharedEventLoopManager acquired, refCount={}", count);
        }
    }
    
    /**
     * 释放引用。
     * 
     * <p>减少引用计数。客户端模式下，引用计数归零时会销毁 EventLoopGroup。
     */
    public void release() {
        synchronized (lock) {
            int count = refCount.decrementAndGet();
            LOGGER.debug("SharedEventLoopManager released, refCount={}", count);
            
            if (count < 0) {
                LOGGER.warn("SharedEventLoopManager refCount is negative: {}", count);
            }
            
            // 客户端模式：引用计数归零时销毁
            if (!serverMode && count == 0 && workerGroup != null) {
                workerGroup.shutdownGracefully(SHUTDOWN_QUIET_PERIOD_MS, 
                    SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                workerGroup = null;
                initialized = false;
                LOGGER.info("Destroyed shared worker EventLoopGroup (client mode)");
            }
        }
    }
    
    /**
     * 获取 boss EventLoopGroup。
     * 
     * @return boss EventLoopGroup，客户端模式返回 null
     */
    public EventLoopGroup getBossGroup() {
        return bossGroup;
    }
    
    /**
     * 获取 worker EventLoopGroup。
     * 
     * @return worker EventLoopGroup
     */
    public EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }
    
    /**
     * 获取当前引用计数。
     * 
     * @return 引用计数
     */
    public int getRefCount() {
        return refCount.get();
    }
    
    /**
     * 获取 boss 线程数。
     * 
     * @return boss 线程数，客户端模式返回 0
     */
    public int getBossThreadCount() {
        if (bossGroup instanceof NioEventLoopGroup) {
            return ((NioEventLoopGroup) bossGroup).executorCount();
        }
        return serverMode ? bossThreads : 0;
    }
    
    /**
     * 获取 worker 线程数。
     * 
     * @return worker 线程数
     */
    public int getWorkerThreadCount() {
        if (workerGroup instanceof NioEventLoopGroup) {
            return ((NioEventLoopGroup) workerGroup).executorCount();
        }
        return workerThreads;
    }
    
    /**
     * 是否已初始化。
     * 
     * @return true 已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 是否已关闭。
     * 
     * @return true 已关闭
     */
    public boolean isShutdown() {
        return shutdown;
    }
    
    /**
     * 关闭 EventLoopGroup。
     * 
     * <p>服务端模式调用此方法后不可再使用。
     * 客户端模式通常由引用计数自动管理，无需手动调用。
     */
    public void shutdown() {
        if (shutdown) {
            return;
        }
        
        synchronized (lock) {
            if (shutdown) {
                return;
            }
            shutdown = true;
            
            LOGGER.info("SharedEventLoopManager shutting down...");
            
            if (bossGroup != null) {
                bossGroup.shutdownGracefully(SHUTDOWN_QUIET_PERIOD_MS, 
                    SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }
            
            if (workerGroup != null) {
                workerGroup.shutdownGracefully(SHUTDOWN_QUIET_PERIOD_MS, 
                    SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }
            
            LOGGER.info("SharedEventLoopManager shutdown complete");
        }
    }
}