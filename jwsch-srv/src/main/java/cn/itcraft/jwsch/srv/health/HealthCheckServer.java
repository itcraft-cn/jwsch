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

/**
 * Health check server that exposes HTTP endpoints for health monitoring.
 *
 * <p>Starts a lightweight HTTP server on a configurable port to provide
 * health check endpoints for liveness and readiness probes.
 *
 * <p>Supports both standalone mode (with its own event loops) and shared
 * mode (using externally provided event loops).
 */
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
    
    /**
     * Creates a HealthCheckServer with its own event loops.
     *
     * @param config the health server configuration
     */
    public HealthCheckServer(HealthConfig config) {
        this.config = config;
        this.healthAggregator = new HealthAggregator();
        this.ownsEventLoop = true;
    }
    
    /**
     * Creates a HealthCheckServer with shared event loops.
     *
     * @param config the health server configuration
     * @param bossGroup the shared boss event loop group
     * @param workerGroup the shared worker event loop group
     */
    public HealthCheckServer(HealthConfig config, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this.config = config;
        this.healthAggregator = new HealthAggregator();
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.ownsEventLoop = false;
    }
    
    /**
     * Starts the health check server.
     *
     * @throws IllegalStateException if the server fails to start
     */
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
    
    /**
     * Shuts down the health check server.
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
        LOGGER.info("HealthCheckServer shutdown");
    }
    
    /**
     * Returns whether the server is started.
     *
     * @return true if the server is started, false otherwise
     */
    public boolean isStarted() {
        return started;
    }
    
    /**
     * Returns the port the server is listening on.
     *
     * @return the server port, or the configured port if not yet started
     */
    public int getPort() {
        if (serverChannel != null) {
            return ((InetSocketAddress) serverChannel.localAddress()).getPort();
        }
        return config.getPort();
    }
    
    /**
     * Returns the health aggregator used by this server.
     *
     * @return the HealthAggregator instance
     */
    public HealthAggregator getHealthAggregator() {
        return healthAggregator;
    }
    
    /**
     * Adds a health indicator to the aggregator.
     *
     * @param indicator the health indicator to add
     */
    public void addIndicator(HealthIndicator indicator) {
        healthAggregator.addIndicator(indicator);
    }
    
    /**
     * Removes a health indicator from the aggregator.
     *
     * @param indicator the health indicator to remove
     */
    public void removeIndicator(HealthIndicator indicator) {
        healthAggregator.removeIndicator(indicator);
    }
}