package cn.itcraft.jwsch.srv.server.websocket;

import cn.itcraft.jwsch.common.eventloop.NativeTransport;
import cn.itcraft.jwsch.common.flowcontrol.FlowControlConfig;
import cn.itcraft.jwsch.common.ssl.SslConfig;
import cn.itcraft.jwsch.common.ssl.SslContextFactory;
import cn.itcraft.jwsch.srv.config.WebSocketConfig;
import cn.itcraft.jwsch.srv.flowcontrol.OutboundBufferHandler;
import cn.itcraft.jwsch.srv.metrics.ServerMetrics;
import cn.itcraft.jwsch.srv.router.PacketRouter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * WebSocket 服务器实现。
 * 
 * <p>基于 Netty 的 WebSocket 服务器，支持 SSL/TLS 加密和背压控制。
 * <ul>
 *   <li>支持二进制和文本帧协议</li>
 *   <li>集成 SSL/TLS 加密（WSS）</li>
 *   <li>支持自定义 EventLoopGroup（共享或独占）</li>
 *   <li>集成背压控制（OutboundBufferHandler）</li>
 *   <li>支持慢查询检测</li>
 *   <li>支持指标收集</li>
 * </ul>
 * 
 * <p>使用 NativeTransport 自动选择最佳传输（Epoll/KQueue/NIO）。
 * 支持优雅启动和关闭。
 */
public class WebSocketServer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketServer.class);
    
    private static final String BOSS_THREAD_PREFIX = "jwsch-ws-boss";
    private static final String WORKER_THREAD_PREFIX = "jwsch-ws-worker";
    private static final int SHUTDOWN_QUIET_PERIOD_MS = 100;
    private static final int SHUTDOWN_TIMEOUT_MS = 300;
    
    private final WebSocketConfig config;
    private final PacketRouter packetRouter;
    private final ServerMetrics serverMetrics;
    private final int slowQueryThresholdMs;
    private final SslContext sslContext;
    private final boolean ownsEventLoop;
    private final FlowControlConfig flowControlConfig;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean started = false;
    
    /**
     * 使用默认配置创建 WebSocket 服务器（禁用 SSL、慢查询检测和背压控制）。
     *
     * @param config WebSocket 配置
     * @param packetRouter 数据包路由器
     * @throws NullPointerException 如果 config 或 packetRouter 为 null
     */
    public WebSocketServer(WebSocketConfig config, PacketRouter packetRouter) {
        this(config, packetRouter, null, 0);
    }
    
    /**
     * 使用默认的背压控制和禁用慢查询检测创建 WebSocket 服务器。
     *
     * @param config WebSocket 配置
     * @param packetRouter 数据包路由器
     * @param serverMetrics 服务器指标收集器（可为 null）
     * @throws NullPointerException 如果 config 或 packetRouter 为 null
     */
    public WebSocketServer(WebSocketConfig config, PacketRouter packetRouter, 
                          ServerMetrics serverMetrics) {
        this(config, packetRouter, serverMetrics, 0);
    }
    
    /**
     * 使用默认的背压控制创建 WebSocket 服务器。
     *
     * @param config WebSocket 配置
     * @param packetRouter 数据包路由器
     * @param serverMetrics 服务器指标收集器（可为 null）
     * @param slowQueryThresholdMs 慢查询阈值（毫秒），0 表示禁用
     * @throws NullPointerException 如果 config 或 packetRouter 为 null
     */
    public WebSocketServer(WebSocketConfig config, PacketRouter packetRouter, 
                          ServerMetrics serverMetrics, int slowQueryThresholdMs) {
        this(config, packetRouter, serverMetrics, slowQueryThresholdMs, FlowControlConfig.defaultConfig());
    }
    
    /**
     * 创建 WebSocket 服务器（拥有独立 EventLoopGroup）。
     *
     * @param config WebSocket 配置
     * @param packetRouter 数据包路由器
     * @param serverMetrics 服务器指标收集器（可为 null）
     * @param slowQueryThresholdMs 慢查询阈值（毫秒），0 表示禁用
     * @param flowControlConfig 背压控制配置（可为 null，使用默认配置）
     * @throws NullPointerException 如果 config 或 packetRouter 为 null
     */
    public WebSocketServer(WebSocketConfig config, PacketRouter packetRouter, 
                          ServerMetrics serverMetrics, int slowQueryThresholdMs,
                          FlowControlConfig flowControlConfig) {
        this.config = config;
        this.packetRouter = packetRouter;
        this.serverMetrics = serverMetrics;
        this.slowQueryThresholdMs = slowQueryThresholdMs;
        this.sslContext = initSslContext(config.getSslConfig());
        this.ownsEventLoop = true;
        this.flowControlConfig = flowControlConfig != null ? flowControlConfig : FlowControlConfig.defaultConfig();
    }
    
    /**
     * 使用共享的 EventLoopGroup 创建 WebSocket 服务器（默认背压控制）。
     *
     * @param config WebSocket 配置
     * @param packetRouter 数据包路由器
     * @param serverMetrics 服务器指标收集器（可为 null）
     * @param slowQueryThresholdMs 慢查询阈值（毫秒），0 表示禁用
     * @param bossGroup 共享的 boss EventLoopGroup（非 null）
     * @param workerGroup 共享的 worker EventLoopGroup（非 null）
     * @throws NullPointerException 如果 config、packetRouter、bossGroup 或 workerGroup 为 null
     */
    public WebSocketServer(WebSocketConfig config, PacketRouter packetRouter, 
                          ServerMetrics serverMetrics, int slowQueryThresholdMs,
                          EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this(config, packetRouter, serverMetrics, slowQueryThresholdMs, 
             bossGroup, workerGroup, FlowControlConfig.defaultConfig());
    }
    
    /**
     * 使用共享的 EventLoopGroup 创建 WebSocket 服务器。
     *
     * @param config WebSocket 配置
     * @param packetRouter 数据包路由器
     * @param serverMetrics 服务器指标收集器（可为 null）
     * @param slowQueryThresholdMs 慢查询阈值（毫秒），0 表示禁用
     * @param bossGroup 共享的 boss EventLoopGroup（非 null）
     * @param workerGroup 共享的 worker EventLoopGroup（非 null）
     * @param flowControlConfig 背压控制配置（可为 null，使用默认配置）
     * @throws NullPointerException 如果 config、packetRouter、bossGroup 或 workerGroup 为 null
     */
    public WebSocketServer(WebSocketConfig config, PacketRouter packetRouter, 
                          ServerMetrics serverMetrics, int slowQueryThresholdMs,
                          EventLoopGroup bossGroup, EventLoopGroup workerGroup,
                          FlowControlConfig flowControlConfig) {
        this.config = config;
        this.packetRouter = packetRouter;
        this.serverMetrics = serverMetrics;
        this.slowQueryThresholdMs = slowQueryThresholdMs;
        this.sslContext = initSslContext(config.getSslConfig());
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.ownsEventLoop = false;
        this.flowControlConfig = flowControlConfig != null ? flowControlConfig : FlowControlConfig.defaultConfig();
    }
    
    private SslContext initSslContext(SslConfig sslConfig) {
        if (sslConfig == null || !sslConfig.isEnabled()) {
            return null;
        }
        
        try {
            SslContext context = SslContextFactory.createServerContext(sslConfig);
            LOGGER.info("SSL enabled for WebSocket server");
            return context;
        } catch (Exception e) {
            LOGGER.error("Failed to initialize SSL context", e);
            throw new RuntimeException("Failed to initialize SSL context", e);
        }
    }
    
    /**
     * 启动 WebSocket 服务器。
     * <p>如果已启动，则忽略调用。
     * <p>启动过程：
     * <ol>
     *   <li>创建或使用现有的 EventLoopGroup</li>
     *   <li>配置 ServerBootstrap（TCP 选项、SSL、HTTP 编解码器）</li>
     *   <li>添加背压控制处理器（如果启用）</li>
     *   <li>配置 WebSocket 协议处理器</li>
     *   <li>绑定端口并启动监听</li>
     * </ol>
     * 
     * @throws IllegalStateException 如果启动失败
     */
    public void start() {
        if (started) {
            LOGGER.warn("WebSocketServer already started");
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
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                    new WriteBufferWaterMark(1024 * 1024, 8 * 1024 * 1024))
                .childOption(ChannelOption.SO_SNDBUF, 4 * 1024 * 1024)
                .childOption(ChannelOption.SO_RCVBUF, 4 * 1024 * 1024)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        
                        if (sslContext != null) {
                            pipeline.addLast("ssl", sslContext.newHandler(ch.alloc()));
                        }
                        
                        pipeline.addLast("httpCodec", new HttpServerCodec())
                            .addLast("httpAggregator", new HttpObjectAggregator(config.getMaxFrameSize()))
                            .addLast("idleState", new IdleStateHandler(180, 0, 0, TimeUnit.SECONDS))
                            .addLast("flushConsolidation", new FlushConsolidationHandler(128, false));
                        
                        if (flowControlConfig.isOutboundEnabled()) {
                            pipeline.addLast("outboundBuffer", new OutboundBufferHandler(flowControlConfig));
                        }
                        
                        int maxFrameSize = config.getMaxFrameSize();
                        LOGGER.info("WebSocketServer initializing with maxFrameSize={}", maxFrameSize);
                        
                        WebSocketDecoderConfig decoderConfig = WebSocketDecoderConfig.newBuilder()
                            .maxFramePayloadLength(maxFrameSize)
                            .build();
                        
                        WebSocketServerProtocolConfig protocolConfig = WebSocketServerProtocolConfig.newBuilder()
                            .websocketPath(config.getPath())
                            .decoderConfig(decoderConfig)
                            .build();
                        
                        pipeline.addLast("webSocketProtocol", new WebSocketServerProtocolHandler(protocolConfig))
                            .addLast("webSocketHandler", new WebSocketHandler(packetRouter, serverMetrics, slowQueryThresholdMs));
                    }
                });
            
            ChannelFuture future = bootstrap.bind(config.getPort()).sync();
            serverChannel = future.channel();
            started = true;
            
            String protocol = sslContext != null ? "WSS" : "WS";
            String transport = NativeTransport.getTransportType();
            LOGGER.info("WebSocketServer started on port {} ({}, transport={})", config.getPort(), protocol, transport);
        } catch (InterruptedException e) {
            LOGGER.error("Failed to start WebSocketServer", e);
            shutdown();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 关闭 WebSocket 服务器。
     * <p>如果未启动，则忽略调用。
     * <p>关闭过程：
     * <ol>
     *   <li>关闭服务器 Channel</li>
     *   <li>如果拥有独立的 EventLoopGroup，则优雅关闭</li>
     *   <li>更新启动状态</li>
     * </ol>
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
        LOGGER.info("WebSocketServer shutdown");
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
    
    /**
     * 检查是否启用了 SSL/TLS 加密。
     *
     * @return true 如果启用了 SSL/TLS，否则 false
     */
    public boolean isSslEnabled() {
        return sslContext != null;
    }
}