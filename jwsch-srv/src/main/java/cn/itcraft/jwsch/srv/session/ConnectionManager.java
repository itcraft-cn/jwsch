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
 * 
 * <p>在集群环境中，ConnectionManager 实现负责：
 * <ul>
 *   <li>本地连接管理：维护当前节点的连接映射</li>
 *   <li>集群转发：将消息转发到其他节点的连接</li>
 *   <li>负载均衡：根据连接分布决定转发目标</li>
 * </ul>
 * 
 * <p>默认实现 {@link RouterConnectionManager} 与 PacketRouter 集成，
 * 支持背压控制和订阅管理。
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
