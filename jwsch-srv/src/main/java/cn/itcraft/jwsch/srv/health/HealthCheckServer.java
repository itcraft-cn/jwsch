package cn.itcraft.jwsch.srv.health;

import cn.itcraft.jwsch.srv.config.HealthConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class HealthCheckServer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckServer.class);
    
    private static final int SHUTDOWN_QUIET_PERIOD_MS = 100;
    private static final int SHUTDOWN_TIMEOUT_MS = 300;
    
    private final HealthConfig config;
    private final HealthAggregator healthAggregator;
    private final boolean ownsEventLoop;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean started = false;
    
    public HealthCheckServer(HealthConfig config) {
        this.config = config;
        this.healthAggregator = new HealthAggregator();
        this.ownsEventLoop = true;
    }
    
    public HealthCheckServer(HealthConfig config, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this.config = config;
        this.healthAggregator = new HealthAggregator();
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.ownsEventLoop = false;
    }
    
    public void start() {
        if (started) {
            LOGGER.warn("HealthCheckServer already started");
            return;
        }
        
        if (ownsEventLoop && bossGroup == null) {
            bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("health-boss", true));
            workerGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("health-worker", true));
        }
        
        HealthCheckHandler handler = new HealthCheckHandler(healthAggregator);
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new HealthServerInitializer(handler));
            
            ChannelFuture future = bootstrap.bind(config.getHost(), config.getPort()).sync();
            serverChannel = future.channel();
            started = true;
            
            LOGGER.info("HealthCheckServer started on {}:{}", config.getHost(), config.getPort());
        } catch (InterruptedException e) {
            LOGGER.error("Failed to start HealthCheckServer", e);
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
        LOGGER.info("HealthCheckServer shutdown");
    }
    
    public boolean isStarted() {
        return started;
    }
    
    public int getPort() {
        if (serverChannel != null) {
            return ((InetSocketAddress) serverChannel.localAddress()).getPort();
        }
        return config.getPort();
    }
    
    public HealthAggregator getHealthAggregator() {
        return healthAggregator;
    }
    
    public void addIndicator(HealthIndicator indicator) {
        healthAggregator.addIndicator(indicator);
    }
    
    public void removeIndicator(HealthIndicator indicator) {
        healthAggregator.removeIndicator(indicator);
    }
}