package cn.itcraft.jwsch.srv.cluster;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.TopicHash;
import cn.itcraft.jwsch.srv.cluster.message.ClusterBroadcast;
import cn.itcraft.jwsch.srv.cluster.message.ClusterForward;
import cn.itcraft.jwsch.srv.session.ConnectionManager;

/**
 * Forwarder for cluster message routing.
 * 
 * <p>Handles message forwarding logic:
 * <ul>
 *   <li>REQUEST: Forward to target node (lookup in ClusterConnectionRegistry)</li>
 *   <li>PUSH: Broadcast to nodes that have subscribers (filter by NodeBloomFilter)</li>
 *   <li>BROADCAST: Forward to all nodes</li>
 * </ul>
 */
public class ClusterForwarder {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterForwarder.class);
    
    private final ClusterConfig config;
    private final ClusterClient client;
    private final ClusterConnectionRegistry connectionRegistry;
    private final InMemoryClusterNodeRegistry nodeRegistry;
    private final ConnectionManager connectionManager;
    private final NodeBloomFilter localBloomFilter;
    
    public ClusterForwarder(ClusterConfig config,
                            ClusterClient client,
                            ClusterConnectionRegistry connectionRegistry,
                            InMemoryClusterNodeRegistry nodeRegistry,
                            ConnectionManager connectionManager,
                            NodeBloomFilter localBloomFilter) {
        this.config = config;
        this.client = client;
        this.connectionRegistry = connectionRegistry;
        this.nodeRegistry = nodeRegistry;
        this.connectionManager = connectionManager;
        this.localBloomFilter = localBloomFilter;
    }
    
    /**
     * Forward REQUEST to target node.
     * 
     * <p>If target connection is local, forward locally.
     * If target connection is remote, forward to the node that owns the connection.
     */
    public void forwardRequest(Packet packet) {
        long targetId = packet.getTargetId();
        
        String targetNode = connectionRegistry.findNodeForConnection(targetId);
        if (targetNode == null) {
            LOGGER.debug("Target connection not found: {}", targetId);
            return;
        }
        
        if (connectionRegistry.isLocal(targetId)) {
            forwardToLocal(targetId, packet);
        } else {
            ClusterForward forward = new ClusterForward(targetId, packet);
            client.sendClusterMessage(targetNode, forward);
            LOGGER.debug("Forwarded REQUEST to node {} for connection {}", targetNode, targetId);
        }
    }
    
    /**
     * Broadcast PUSH to nodes that have subscribers for the topic.
     * 
     * <p>Uses NodeBloomFilter to filter nodes that definitely have no subscribers.
     */
    public void broadcastPush(Packet packet) {
        String topic = packet.getTopic();
        if (topic == null || topic.isEmpty()) {
            LOGGER.warn("PUSH without topic, skipping cluster broadcast");
            return;
        }
        
        long topicHash = TopicHash.hash(topic);
        byte[] body = extractBody(packet);
        
        ClusterBroadcast broadcast = new ClusterBroadcast(
            config.getNodeId(),
            topicHash,
            Command.PUSH,
            body
        );
        
        int sentCount = 0;
        for (NodeInfo node : nodeRegistry.getNodes()) {
            if (client.isConnected(node.getNodeId())) {
                client.sendClusterMessage(node.getNodeId(), broadcast);
                sentCount++;
            }
        }
        
        LOGGER.debug("Broadcast PUSH topic={} to {} nodes", topic, sentCount);
    }
    
    /**
     * Broadcast BROADCAST to all nodes.
     */
    public void broadcastAll(Packet packet) {
        byte[] body = extractBody(packet);
        
        ClusterBroadcast broadcast = new ClusterBroadcast(
            config.getNodeId(),
            0,
            Command.BROADCAST,
            body
        );
        
        client.broadcastClusterMessage(broadcast);
        LOGGER.debug("Broadcast BROADCAST to all nodes");
    }
    
    /**
     * Forward message to local connection.
     */
    public void forwardToLocal(long connectionId, Packet packet) {
        if (connectionManager != null) {
            connectionManager.send(connectionId, packet);
            LOGGER.debug("Forwarded to local connection {}", connectionId);
        }
    }
    
    /**
     * Broadcast locally to subscribers matching the topic.
     */
    public void broadcastLocally(ClusterBroadcast broadcast) {
        if (connectionManager == null) {
            return;
        }
        
        byte[] body = broadcast.getBody();
        byte originalCmd = broadcast.getOriginalCmd();
        
        if (broadcast.isBroadcast()) {
            connectionManager.broadcastAll(body, originalCmd);
            LOGGER.debug("Broadcast locally to all connections");
        } else if (broadcast.isPush() && broadcast.hasTopic()) {
            long topicHash = broadcast.getTopicHash();
            connectionManager.broadcastByTopicHash(topicHash, body, originalCmd);
            LOGGER.debug("Broadcast locally by topicHash={}", topicHash);
        }
    }
    
    private byte[] extractBody(Packet packet) {
        if (packet.getBodyBuf() != null && packet.getBodyBuf().isReadable()) {
            byte[] body = new byte[packet.getBodyBuf().readableBytes()];
            packet.getBodyBuf().getBytes(packet.getBodyBuf().readerIndex(), body);
            return body;
        }
        return new byte[0];
    }
}
