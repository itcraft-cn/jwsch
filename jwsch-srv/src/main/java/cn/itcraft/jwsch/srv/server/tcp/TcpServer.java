package cn.itcraft.jwsch.srv.server.tcp;

import cn.itcraft.jwsch.common.eventloop.NativeTransport;
import cn.itcraft.jwsch.common.flowcontrol.FlowControlConfig;
import cn.itcraft.jwsch.srv.config.TcpConfig;
import cn.itcraft.jwsch.srv.metrics.ServerMetrics;
import cn.itcraft.jwsch.srv.router.PacketRouter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * TCP 服务器。
 * 
 * <p>负责处理后端服务（如 sample-pusher）的连接，接收并转发 Packet。
 * 支持原生传输（Epoll on Linux, KQueue on BSD）以获得更好的性能。
 * 
 * <p>特性：
 * <ul>
 *   <li>独立的 EventLoopGroup（与 WebSocket 服务器隔离）</li>
 *   <li>可配置的 TCP 参数（SO_BACKLOG、TCP_NODELAY、KEEPALIVE 等）</li>
 *   <li>L1 入站速率限制（基于 Token Bucket）</li>
 *   <li>背压控制（WriteBufferWaterMark）</li>
 * </ul>
 * 
 * <p>支持两种 EventLoopGroup 管理模式：
 * <ol>
 *   <li>自有模式（ownsEventLoop=true）：创建并管理 EventLoopGroup 生命周期</li>
 *   <li>共享模式（ownsEventLoop=false）：使用外部提供的 EventLoopGroup</li>
 * </ol>
 */
public class TcpServer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TcpServer.class);
    
    private static final String BOSS_THREAD_PREFIX = "jwsch-tcp-boss";
    private static final String WORKER_THREAD_PREFIX = "jwsch-tcp-worker";
    private static final int SHUTDOWN_QUIET_PERIOD_MS = 100;
    private static final int SHUTDOWN_TIMEOUT_MS = 300;
    
    private final TcpConfig config;
    private final PacketRouter packetRouter;
    private final ServerMetrics serverMetrics;
    private final FlowControlConfig flowControlConfig;
    private final boolean ownsEventLoop;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean started = false;
    
    /**
     * 使用默认的 ServerMetrics 和 FlowControlConfig 创建 TCP 服务器。
     *
     * @param config TCP 配置
     * @param packetRouter 数据包路由器
     * @throws NullPointerException 如果 config 或 packetRouter 为 null
     */
    public TcpServer(TcpConfig config, PacketRouter packetRouter) {
        this(config, packetRouter, null, FlowControlConfig.defaultConfig());
    }
    
    /**
     * 使用指定的 ServerMetrics 和默认的 FlowControlConfig 创建 TCP 服务器。
     *
     * @param config TCP 配置
     * @param packetRouter 数据包路由器
     * @param serverMetrics 服务器指标收集器（可为 null）
     * @throws NullPointerException 如果 config 或 packetRouter 为 null
     */
    public TcpServer(TcpConfig config, PacketRouter packetRouter, ServerMetrics serverMetrics) {
        this(config, packetRouter, serverMetrics, FlowControlConfig.defaultConfig());
    }
    
    /**
     * 创建 TCP 服务器（自有 EventLoopGroup 模式）。
     *
     * @param config TCP 配置
     * @param packetRouter 数据包路由器
     * @param serverMetrics 服务器指标收集器（可为 null）
     * @param flowControlConfig 流量控制配置（可为 null，使用默认配置）
     * @throws NullPointerException 如果 config 或 packetRouter 为 null
     */
    public TcpServer(TcpConfig config, PacketRouter packetRouter, ServerMetrics serverMetrics,
                     FlowControlConfig flowControlConfig) {
        this.config = config;
        this.packetRouter = packetRouter;
        this.serverMetrics = serverMetrics;
        this.flowControlConfig = flowControlConfig != null ? flowControlConfig : FlowControlConfig.defaultConfig();
        this.ownsEventLoop = true;
    }
    
    /**
     * 创建 TCP 服务器（共享 EventLoopGroup 模式）。
     *
     * @param config TCP 配置
     * @param packetRouter 数据包路由器
     * @param serverMetrics 服务器指标收集器（可为 null）
     * @param bossGroup 共享的 boss EventLoopGroup
     * @param workerGroup 共享的 worker EventLoopGroup
     * @throws NullPointerException 如果 config、packetRouter、bossGroup 或 workerGroup 为 null
     */
    public TcpServer(TcpConfig config, PacketRouter packetRouter, ServerMetrics serverMetrics, 
                    EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this(config, packetRouter, serverMetrics, bossGroup, workerGroup, FlowControlConfig.defaultConfig());
    }
    
    /**
     * 创建 TCP 服务器（共享 EventLoopGroup 模式，带流量控制配置）。
     *
     * @param config TCP 配置
     * @param packetRouter 数据包路由器
     * @param serverMetrics 服务器指标收集器（可为 null）
     * @param bossGroup 共享的 boss EventLoopGroup
     * @param workerGroup 共享的 worker EventLoopGroup
     * @param flowControlConfig 流量控制配置（可为 null，使用默认配置）
     * @throws NullPointerException 如果 config、packetRouter、bossGroup 或 workerGroup 为 null
     */
    public TcpServer(TcpConfig config, PacketRouter packetRouter, ServerMetrics serverMetrics, 
                    EventLoopGroup bossGroup, EventLoopGroup workerGroup,
                    FlowControlConfig flowControlConfig) {
        this.config = config;
        this.packetRouter = packetRouter;
        this.serverMetrics = serverMetrics;
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.flowControlConfig = flowControlConfig != null ? flowControlConfig : FlowControlConfig.defaultConfig();
        this.ownsEventLoop = false;
    }
    
    /**
     * 启动 TCP 服务器。
     * 
     * <p>如果 ownsEventLoop 为 true 且 EventLoopGroup 未初始化，则创建新的 EventLoopGroup。
     * 配置 ServerBootstrap 并绑定到指定端口。
     * 
     * @throws IllegalStateException 如果启动失败
     */
    public void start() {
        if (started) {
            LOGGER.warn("TcpServer already started");
            return;
        }
        
        if (ownsEventLoop && bossGroup == null) {
            bossGroup = NativeTransport.createEventLoopGroup(config.getBossThreads(), BOSS_THREAD_PREFIX);
            workerGroup = NativeTransport.createEventLoopGroup(config.getWorkerThreads(), WORKER_THREAD_PREFIX);
        }
        
        Class<? extends ServerChannel> channelClass = NativeTransport.getServerChannelClass();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(channelClass)
                .option(ChannelOption.SO_BACKLOG, config.getSoBacklog())
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, config.isTcpNoDelay())
                .childOption(ChannelOption.SO_KEEPALIVE, config.isKeepAlive())
                .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeout())
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                    new WriteBufferWaterMark(1024 * 1024, 8 * 1024 * 1024))
                .childOption(ChannelOption.SO_SNDBUF, 4 * 1024 * 1024)
                .childOption(ChannelOption.SO_RCVBUF, 4 * 1024 * 1024)
                .childHandler(new TcpServerInitializer(packetRouter, serverMetrics, flowControlConfig, 
                    config.getMaxPacketLength(), config.isLogOversizeContent()));
            
            ChannelFuture future = bootstrap.bind(config.getPort()).sync();
            serverChannel = future.channel();
            started = true;
            
            String transport = NativeTransport.getTransportType();
            LOGGER.info("TcpServer started on port {} (transport={})", config.getPort(), transport);
        } catch (InterruptedException e) {
            LOGGER.error("Failed to start TcpServer", e);
            shutdown();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 关闭 TCP 服务器。
     * 
     * <p>关闭服务器 Channel，如果 ownsEventLoop 为 true 则关闭 EventLoopGroup。
     */
    public void shutdown() {
        if (!started) {
            return;
        }
        
        if (serverChannel != null) {
            serverChannel.close();
        }
        
        if (ownsEventLoop) {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully(SHUTDOWN_QUIET_PERIOD_MS, 
                    SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully(SHUTDOWN_QUIET_PERIOD_MS, 
                    SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }
        }
        
        started = false;
        LOGGER.info("TcpServer shutdown");
    }
    
    /**
     * 检查服务器是否已启动。
     *
     * @return true 如果服务器已启动，否则 false
     */
    public boolean isStarted() {
        return started;
    }
    
    /**
     * 获取服务器监听的端口。
     *
     * @return 端口号
     */
    public int getPort() {
        return config.getPort();
    }
}