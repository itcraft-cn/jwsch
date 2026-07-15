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
 * Server-side handler for cluster messages.
 * 
 * <p>Handles incoming messages from other cluster nodes:
 * <ul>
 *   <li>CLUSTER_JOIN: Record node info, return CLUSTER_MEMBERSHIP</li>
 *   <li>CLUSTER_MEMBERSHIP: Connect to new nodes</li>
 *   <li>CLUSTER_SYNC: Update ClusterConnectionRegistry</li>
 *   <li>CLUSTER_FORWARD: Forward to local connection</li>
 *   <li>CLUSTER_BROADCAST: Broadcast to local subscribers</li>
 *   <li>CLUSTER_HEARTBEAT: Update heartbeat timestamp</li>
 * </ul>
 */
class ClusterServerHandler extends SimpleChannelInboundHandler<Object> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterServerHandler.class);
    
    private final ClusterConnectionRegistry connectionRegistry;
    private final InMemoryClusterNodeRegistry nodeRegistry;
    private final ClusterMeshManager meshManager;
    private final ConcurrentHashMap<Channel, String> channelToNodeId = new ConcurrentHashMap<>();
    
    ClusterServerHandler(ClusterConnectionRegistry connectionRegistry,
                         InMemoryClusterNodeRegistry nodeRegistry,
                         ClusterMeshManager meshManager) {
        this.connectionRegistry = connectionRegistry;
        this.nodeRegistry = nodeRegistry;
        this.meshManager = meshManager;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ClusterJoin) {
            handleJoin(ctx, (ClusterJoin) msg);
        } else if (msg instanceof ClusterMembership) {
            handleMembership((ClusterMembership) msg);
        } else if (msg instanceof ClusterSync) {
            handleSync(ctx, (ClusterSync) msg);
        } else if (msg instanceof ClusterForward) {
            handleForward((ClusterForward) msg);
        } else if (msg instanceof ClusterBroadcast) {
            handleBroadcast((ClusterBroadcast) msg);
        } else if (msg instanceof ClusterHeartbeat) {
            handleHeartbeat(ctx, (ClusterHeartbeat) msg);
        } else {
            LOGGER.warn("Unknown message type: {}", msg.getClass().getSimpleName());
        }
    }
    
    private void handleJoin(ChannelHandlerContext ctx, ClusterJoin join) {
        String nodeId = join.getNodeId();
        
        channelToNodeId.put(ctx.channel(), nodeId);
        
        if (meshManager != null) {
            meshManager.onNodeJoin(join, ctx.channel());
        }
        
        LOGGER.info("Node joined: {} from {}", nodeId, ctx.channel().remoteAddress());
    }
    
    private void handleMembership(ClusterMembership membership) {
        if (meshManager != null) {
            meshManager.onMembershipReceived(membership);
        }
    }
    
    private void handleSync(ChannelHandlerContext ctx, ClusterSync sync) {
        String sourceNodeId = channelToNodeId.get(ctx.channel());
        if (sourceNodeId == null) {
            LOGGER.warn("Received CLUSTER_SYNC from unknown node");
            return;
        }
        
        for (ClusterSync.SyncOp op : sync.getOperations()) {
            handleSyncOp(sourceNodeId, op);
        }
        
        LOGGER.debug("Processed CLUSTER_SYNC: {} ops from {}", sync.getOperations().size(), sourceNodeId);
    }
    
    private void handleSyncOp(String sourceNodeId, ClusterSync.SyncOp op) {
        long connectionId = op.getConnectionId();
        byte opType = op.getOpType();
        
        switch (opType) {
            case ClusterSync.OP_ADD_CONNECTION:
                RemoteConnection remoteConn = new RemoteConnection(connectionId, sourceNodeId);
                connectionRegistry.addRemoteConnection(connectionId, remoteConn);
                LOGGER.debug("Added remote connection: {} from {}", connectionId, sourceNodeId);
                break;
                
            case ClusterSync.OP_REMOVE_CONNECTION:
                connectionRegistry.removeRemoteConnection(connectionId);
                LOGGER.debug("Removed remote connection: {} from {}", connectionId, sourceNodeId);
                break;
                
            case ClusterSync.OP_ADD_SUBSCRIPTION:
                LOGGER.debug("Added subscription: conn={}, topics={}", connectionId, op.getTopicHashes().size());
                break;
                
            case ClusterSync.OP_REMOVE_SUBSCRIPTION:
                LOGGER.debug("Removed subscription: conn={}, topics={}", connectionId, op.getTopicHashes().size());
                break;
                
            default:
                LOGGER.warn("Unknown sync op type: {}", opType);
        }
    }
    
    private void handleForward(ClusterForward forward) {
        LOGGER.debug("Received CLUSTER_FORWARD: targetId={}", forward.getTargetId());
        
        // TODO: Forward to local connection via ClusterForwarder
    }
    
    private void handleBroadcast(ClusterBroadcast broadcast) {
        LOGGER.debug("Received CLUSTER_BROADCAST: topic={}, cmd={}", 
            broadcast.getTopicHash(), broadcast.getOriginalCmd());
        
        // TODO: Broadcast to local subscribers via ClusterForwarder
    }
    
    private void handleHeartbeat(ChannelHandlerContext ctx, ClusterHeartbeat heartbeat) {
        String nodeId = channelToNodeId.get(ctx.channel());
        if (nodeId != null && meshManager != null) {
            meshManager.onHeartbeat(nodeId);
        }
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOGGER.info("Cluster node connected: {}", ctx.channel().remoteAddress());
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String nodeId = channelToNodeId.remove(ctx.channel());
        if (nodeId != null) {
            LOGGER.info("Cluster node disconnected: {}", nodeId);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Cluster connection error from {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}