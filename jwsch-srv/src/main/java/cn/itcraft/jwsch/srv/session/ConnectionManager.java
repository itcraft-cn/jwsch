package cn.itcraft.jwsch.srv.session;

import cn.itcraft.jwsch.common.protocol.Packet;

/**
 * Interface for managing connections and sending messages.
 * 
 * <p>Provides abstraction for cluster message forwarding:
 * <ul>
 *   <li>send: Send packet to a specific connection</li>
 *   <li>broadcastAll: Broadcast to all connections</li>
 *   <li>broadcastByTopicHash: Broadcast to subscribers of a topic (by hash)</li>
 * </ul>
 */
public interface ConnectionManager {
    
    /**
     * Send packet to a specific connection.
     * 
     * @param connectionId Target connection ID
     * @param packet Packet to send
     */
    void send(long connectionId, Packet packet);
    
    /**
     * Broadcast body to all connections.
     * 
     * @param body Message body bytes
     * @param originalCmd Original command type (PUSH or BROADCAST)
     */
    void broadcastAll(byte[] body, byte originalCmd);
    
    /**
     * Broadcast body to subscribers of a topic.
     * 
     * @param topicHash Topic hash (xxHash64)
     * @param body Message body bytes
     * @param originalCmd Original command type (PUSH or BROADCAST)
     */
    void broadcastByTopicHash(long topicHash, byte[] body, byte originalCmd);
    
    /**
     * Check if connection exists locally.
     * 
     * @param connectionId Connection ID to check
     * @return true if connection exists and is active
     */
    boolean hasConnection(long connectionId);
    
    /**
     * Get total number of local connections.
     * 
     * @return Connection count
     */
    int getConnectionCount();
}
