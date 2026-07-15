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
    
    public TcpServer(TcpConfig config, PacketRouter packetRouter) {
        this(config, packetRouter, null, FlowControlConfig.defaultConfig());
    }
    
    public TcpServer(TcpConfig config, PacketRouter packetRouter, ServerMetrics serverMetrics) {
        this(config, packetRouter, serverMetrics, FlowControlConfig.defaultConfig());
    }
    
    public TcpServer(TcpConfig config, PacketRouter packetRouter, ServerMetrics serverMetrics,
                     FlowControlConfig flowControlConfig) {
        this.config = config;
        this.packetRouter = packetRouter;
        this.serverMetrics = serverMetrics;
        this.flowControlConfig = flowControlConfig != null ? flowControlConfig : FlowControlConfig.defaultConfig();
        this.ownsEventLoop = true;
    }
    
    public TcpServer(TcpConfig config, PacketRouter packetRouter, ServerMetrics serverMetrics, 
                    EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this(config, packetRouter, serverMetrics, bossGroup, workerGroup, FlowControlConfig.defaultConfig());
    }
    
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
                .childHandler(new TcpServerInitializer(packetRouter, serverMetrics, flowControlConfig));
            
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
    
    public boolean isStarted() {
        return started;
    }
    
    public int getPort() {
        return config.getPort();
    }
}