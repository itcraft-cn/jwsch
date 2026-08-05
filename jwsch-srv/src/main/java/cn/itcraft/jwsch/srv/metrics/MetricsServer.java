package cn.itcraft.jwsch.srv.metrics;

/**
 * MetricsServer 是提供 Prometheus 格式指标暴露的 HTTP 服务器。
 * 
 * <p>基于 Netty 实现，监听指定端口，当访问配置的路径时返回指标数据。
 * 支持使用共享 EventLoopGroup 或独立创建。
 * 
 * @author itcraft
 * @since 1.0
 */

import cn.itcraft.jwsch.srv.config.MetricsConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class MetricsServer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsServer.class);
    
    private static final int SHUTDOWN_QUIET_PERIOD_MS = 100;
    private static final int SHUTDOWN_TIMEOUT_MS = 300;
    
    private final MetricsConfig config;
    private final ServerMetrics serverMetrics;
    private final boolean ownsEventLoop;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean started = false;
    
    /**
     * 构造函数，创建独立 EventLoopGroup 的指标服务器。
     *
     * @param config 指标配置
     * @param serverMetrics 指标收集器
     */
    public MetricsServer(MetricsConfig config, ServerMetrics serverMetrics) {
        this.config = config;
        this.serverMetrics = serverMetrics;
        this.ownsEventLoop = true;
    }
    
    /**
     * 构造函数，使用共享 EventLoopGroup 的指标服务器。
     *
     * @param config 指标配置
     * @param serverMetrics 指标收集器
     * @param bossGroup 共享的 boss EventLoopGroup
     * @param workerGroup 共享的 worker EventLoopGroup
     */
    public MetricsServer(MetricsConfig config, ServerMetrics serverMetrics, 
                        EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this.config = config;
        this.serverMetrics = serverMetrics;
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.ownsEventLoop = false;
    }
    
    /**
     * 启动指标服务器。
     * 
     * <p>如果已启动则忽略重复调用。启动后会绑定配置的端口并开始监听 HTTP 请求。
     */
    public void start() {
        if (started) {
            LOGGER.warn("MetricsServer already started");
            return;
        }
        
        if (ownsEventLoop && bossGroup == null) {
            bossGroup = new NioEventLoopGroup(1, 
                new DefaultThreadFactory("metrics-boss", true));
            workerGroup = new NioEventLoopGroup(1, 
                new DefaultThreadFactory("metrics-worker", true));
        }
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                            .addLast("httpCodec", new HttpServerCodec())
                            .addLast("httpAggregator", new HttpObjectAggregator(8192))
                            .addLast("metricsHandler", new MetricsHandler());
                    }
                });
            
            ChannelFuture future = bootstrap.bind(config.getPort()).sync();
            serverChannel = future.channel();
            started = true;
            
            LOGGER.info("MetricsServer started on port {} at path {}", 
                config.getPort(), config.getPath());
        } catch (InterruptedException e) {
            LOGGER.error("Failed to start MetricsServer", e);
            shutdown();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 关闭指标服务器。
     * 
     * <p>如果未启动则无操作。关闭时会优雅停止 EventLoopGroup（如果是独立创建的）。
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
        LOGGER.info("MetricsServer shutdown");
    }
    
    /**
     * 检查指标服务器是否已启动。
     *
     * @return 是否已启动
     */
    public boolean isStarted() {
        return started;
    }
    
    /**
     * 获取指标服务器的监听端口。
     *
     * @return 监听端口
     */
    public int getPort() {
        return config.getPort();
    }
    
    private class MetricsHandler extends SimpleChannelInboundHandler<HttpRequest> {
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
            String uri = request.uri();
            
            if (uri.equals(config.getPath()) || uri.equals(config.getPath() + "/")) {
                String metrics = serverMetrics.scrapePrometheus();
                FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, 
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(metrics, CharsetUtil.UTF_8)
                );
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
                ctx.writeAndFlush(response);
            } else {
                FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, 
                    HttpResponseStatus.NOT_FOUND,
                    Unpooled.copiedBuffer("Not Found", CharsetUtil.UTF_8)
                );
                ctx.writeAndFlush(response);
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            LOGGER.error("Error handling metrics request", cause);
            ctx.close();
        }
    }
}