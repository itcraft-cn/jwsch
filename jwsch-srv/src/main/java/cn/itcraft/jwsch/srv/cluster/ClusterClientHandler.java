package cn.itcraft.jwsch.srv.cluster;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.itcraft.jwsch.srv.cluster.message.ClusterBroadcast;
import cn.itcraft.jwsch.srv.cluster.message.ClusterForward;
import cn.itcraft.jwsch.srv.cluster.message.ClusterHeartbeat;
import cn.itcraft.jwsch.srv.cluster.message.ClusterJoin;
import cn.itcraft.jwsch.srv.cluster.message.ClusterMembership;
import cn.itcraft.jwsch.srv.cluster.message.ClusterSync;

/**
 * Client-side handler for cluster messages.
 * 
 * <p>Handles incoming messages from other cluster nodes.
 */
class ClusterClientHandler extends SimpleChannelInboundHandler<Object> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterClientHandler.class);
    
    private ClusterMeshManager meshManager;
    private final ConcurrentHashMap<Channel, String> channelToNodeId = new ConcurrentHashMap<>();
    
    void setMeshManager(ClusterMeshManager meshManager) {
        this.meshManager = meshManager;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ClusterMembership) {
            handleMembership((ClusterMembership) msg);
        } else if (msg instanceof ClusterSync) {
            handleSync(ctx, (ClusterSync) msg);
        } else if (msg instanceof ClusterForward) {
            handleForward((ClusterForward) msg);
        } else if (msg instanceof ClusterBroadcast) {
            handleBroadcast((ClusterBroadcast) msg);
        } else if (msg instanceof ClusterHeartbeat) {
            handleHeartbeat(ctx);
        } else if (msg instanceof ClusterJoin) {
            LOGGER.debug("Received CLUSTER_JOIN response");
        } else {
            LOGGER.warn("Unknown message type: {}", msg.getClass().getSimpleName());
        }
    }
    
    private void handleMembership(ClusterMembership membership) {
        if (meshManager != null) {
            meshManager.onMembershipReceived(membership);
        }
    }
    
    private void handleSync(ChannelHandlerContext ctx, ClusterSync sync) {
        LOGGER.debug("Received CLUSTER_SYNC: {} ops", sync.getOperations().size());
    }
    
    private void handleForward(ClusterForward forward) {
        LOGGER.debug("Received CLUSTER_FORWARD: targetId={}", forward.getTargetId());
    }
    
    private void handleBroadcast(ClusterBroadcast broadcast) {
        LOGGER.debug("Received CLUSTER_BROADCAST: topic={}", broadcast.getTopicHash());
    }
    
    private void handleHeartbeat(ChannelHandlerContext ctx) {
        String nodeId = channelToNodeId.get(ctx.channel());
        if (nodeId != null && meshManager != null) {
            meshManager.onHeartbeat(nodeId);
        }
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOGGER.info("Connected to cluster node: {}", ctx.channel().remoteAddress());
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String nodeId = channelToNodeId.remove(ctx.channel());
        LOGGER.info("Disconnected from cluster node: {}", 
            nodeId != null ? nodeId : ctx.channel().remoteAddress());
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Cluster connection error: {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}
