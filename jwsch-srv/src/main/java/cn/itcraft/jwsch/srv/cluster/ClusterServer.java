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

public class ClusterServer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterServer.class);
    
    private final ClusterConfig config;
    private final ClusterConnectionRegistry connectionRegistry;
    private final InMemoryClusterNodeRegistry nodeRegistry;
    private ClusterMeshManager meshManager;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    
    public ClusterServer(ClusterConfig config, 
                         ClusterConnectionRegistry connectionRegistry,
                         InMemoryClusterNodeRegistry nodeRegistry) {
        this.config = config;
        this.connectionRegistry = connectionRegistry;
        this.nodeRegistry = nodeRegistry;
    }
    
    void setMeshManager(ClusterMeshManager meshManager) {
        this.meshManager = meshManager;
    }
    
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
    
    public int getPort() {
        if (serverChannel != null) {
            return ((InetSocketAddress) serverChannel.localAddress()).getPort();
        }
        return config.getClusterPort();
    }
}