package cn.itcraft.jwsch.srv.cluster;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Cluster server for inter-node communication.
 * 
 * <p>Listens on cluster port for incoming connections from other cluster nodes.
 * Handles cluster protocol messages (JOIN, MEMBERSHIP, HEARTBEAT, FORWARD, BROADCAST).
 * Uses Netty NIO server with separate boss/worker event loops.
 */
public class ClusterServer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterServer.class);
    
    private final ClusterConfig config;
    private final ClusterConnectionRegistry connectionRegistry;
    private final InMemoryClusterNodeRegistry nodeRegistry;
    private ClusterMeshManager meshManager;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    
    /**
     * Creates a cluster server with dependencies.
     * 
     * @param config cluster configuration
     * @param connectionRegistry registry mapping connections to nodes
     * @param nodeRegistry registry of known cluster nodes
     */
    public ClusterServer(ClusterConfig config, 
                         ClusterConnectionRegistry connectionRegistry,
                         InMemoryClusterNodeRegistry nodeRegistry) {
        this.config = config;
        this.connectionRegistry = connectionRegistry;
        this.nodeRegistry = nodeRegistry;
    }
    
    /**
     * Sets the mesh manager for callback handling.
     * 
     * @param meshManager cluster mesh manager
     */
    void setMeshManager(ClusterMeshManager meshManager) {
        this.meshManager = meshManager;
    }
    
    /**
     * Starts the cluster server, binding to the configured cluster port.
     * 
     * <p>Initializes Netty boss/worker event loops, sets up pipeline with
     * cluster message decoder/encoder and server handler.
     * 
     * @throws InterruptedException if thread is interrupted while binding
     */
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("cluster-boss"));
        workerGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("cluster-worker"));
        
        ClusterServerHandler serverHandler = new ClusterServerHandler(
            connectionRegistry, nodeRegistry, meshManager);
        
        ServerBootstrap bootstrap = new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast("decoder", new ClusterMessageDecoder());
                    ch.pipeline().addLast("encoder", new ClusterMessageEncoder());
                    ch.pipeline().addLast("handler", serverHandler);
                }
            });
        
        serverChannel = bootstrap.bind(config.getClusterPort()).sync().channel();
        
        LOGGER.info("ClusterServer started on port {}", config.getClusterPort());
    }
    
    /**
     * Stops the cluster server, closing the server channel and shutting down event loops.
     */
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully(0, 100, TimeUnit.MILLISECONDS);
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(0, 100, TimeUnit.MILLISECONDS);
        }
        
        LOGGER.info("ClusterServer stopped");
    }
    
    /**
     * Returns the port the cluster server is bound to.
     * 
     * @return the actual bound port if server is running, otherwise the configured cluster port
     */
    public int getPort() {
        if (serverChannel != null) {
            return ((InetSocketAddress) serverChannel.localAddress()).getPort();
        }
        return config.getClusterPort();
    }
}