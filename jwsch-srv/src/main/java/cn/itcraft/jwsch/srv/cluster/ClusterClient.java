package cn.itcraft.jwsch.srv.cluster;

import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.srv.cluster.message.ClusterMessage;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Cluster client for connecting to other cluster nodes.
 * 
 * <p>Maintains TCP connections to other nodes in the cluster mesh.
 * Handles sending and broadcasting messages (both Packet and ClusterMessage).
 * Uses Netty NIO client with single worker event loop.
 */
public class ClusterClient {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterClient.class);
    
    private final ClusterConfig config;
    private final EventLoopGroup workerGroup;
    private final Map<String, Channel> connections = new ConcurrentHashMap<>();
    
    /**
     * Creates a cluster client with configuration.
     * 
     * @param config cluster configuration
     */
    public ClusterClient(ClusterConfig config) {
        this.config = config;
        this.workerGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("cluster-client"));
    }
    
    /**
     * Connects to a cluster node.
     * 
     * <p>Establishes a TCP connection to the node's cluster port.
     * If already connected, does nothing.
     * 
     * @param node node information
     */
    public void connect(NodeInfo node) {
        if (connections.containsKey(node.getNodeId())) {
            return;
        }
        
        try {
            Bootstrap bootstrap = new Bootstrap()
                .group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectionTimeoutMs())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new ClusterMessageDecoder());
                        ch.pipeline().addLast(new ClusterMessageEncoder());
                        ch.pipeline().addLast(new ClusterClientHandler());
                    }
                });
            
            ChannelFuture future = bootstrap.connect(node.getHost(), node.getClusterPort()).sync();
            connections.put(node.getNodeId(), future.channel());
            LOGGER.info("Connected to cluster node: {} at {}", node.getNodeId(), node.getClusterAddress());
            
        } catch (Exception e) {
            LOGGER.error("Failed to connect to node: {}", node.getNodeId(), e);
        }
    }
    
    /**
     * Disconnects from a cluster node.
     * 
     * @param nodeId identifier of the node to disconnect from
     */
    public void disconnect(String nodeId) {
        Channel channel = connections.remove(nodeId);
        if (channel != null) {
            channel.close();
            LOGGER.info("Disconnected from cluster node: {}", nodeId);
        }
    }
    
    /**
     * Sends a Packet to a specific cluster node.
     * 
     * @param nodeId identifier of the target node
     * @param packet packet to send
     */
    public void send(String nodeId, Packet packet) {
        Channel channel = connections.get(nodeId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(packet);
        } else {
            LOGGER.warn("Channel not active for node: {}", nodeId);
        }
    }
    
    /**
     * Sends a ClusterMessage to a specific cluster node.
     * 
     * @param nodeId identifier of the target node
     * @param msg cluster message to send
     */
    public void sendClusterMessage(String nodeId, ClusterMessage msg) {
        Channel channel = connections.get(nodeId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(msg);
        } else {
            LOGGER.warn("Channel not active for node: {}", nodeId);
        }
    }
    
    /**
     * Broadcasts a Packet to all connected cluster nodes.
     * 
     * @param packet packet to broadcast
     */
    public void broadcast(Packet packet) {
        for (Channel channel : connections.values()) {
            if (channel.isActive()) {
                channel.writeAndFlush(packet);
            }
        }
    }
    
    /**
     * Broadcasts a ClusterMessage to all connected cluster nodes.
     * 
     * @param msg cluster message to broadcast
     */
    public void broadcastClusterMessage(ClusterMessage msg) {
        for (Channel channel : connections.values()) {
            if (channel.isActive()) {
                channel.writeAndFlush(msg);
            }
        }
    }
    
    /**
     * Checks if connected to a specific cluster node.
     * 
     * @param nodeId identifier of the node
     * @return {@code true} if connected and channel is active
     */
    public boolean isConnected(String nodeId) {
        Channel channel = connections.get(nodeId);
        return channel != null && channel.isActive();
    }
    
    /**
     * Gets the channel for a specific cluster node.
     * 
     * @param nodeId identifier of the node
     * @return the Netty Channel, or {@code null} if not connected
     */
    public Channel getChannel(String nodeId) {
        return connections.get(nodeId);
    }
    
    /**
     * Returns the number of established connections.
     * 
     * @return connection count
     */
    public int getConnectionCount() {
        return connections.size();
    }
    
    /**
     * Checks if there is at least one active connection to any cluster node.
     * 
     * @return {@code true} if any connected channel is active
     */
    public boolean hasConnectedNodes() {
        for (Channel channel : connections.values()) {
            if (channel.isActive()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Shuts down the cluster client, closing all connections and releasing resources.
     */
    public void shutdown() {
        for (Channel channel : connections.values()) {
            channel.close();
        }
        connections.clear();
        
        workerGroup.shutdownGracefully(0, 100, TimeUnit.MILLISECONDS);
        LOGGER.info("ClusterClient shutdown");
    }
}