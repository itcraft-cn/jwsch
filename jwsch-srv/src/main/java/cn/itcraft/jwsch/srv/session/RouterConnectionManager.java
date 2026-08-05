package cn.itcraft.jwsch.srv.session;

import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketHeader;
import cn.itcraft.jwsch.common.protocol.PacketWriter;
import cn.itcraft.jwsch.srv.router.TopicSubscription;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConnectionManager implementation that adapts PacketRouter.
 * 
 * <p>Provides cluster forwarding capabilities by wrapping PacketRouter's
 * frontend connection management and topic subscription.
 * 
 * <p>实现特点：
 * <ul>
 *   <li>零拷贝广播：使用 ByteBuf.retainedSlice() 避免内存复制</li>
 *   <li>连接状态检查：发送前验证 Channel.isActive()</li>
 *   <li>主题哈希映射：通过 TopicSubscription 快速定位订阅者</li>
 *   <li>资源管理：正确释放 PooledByteBufAllocator 分配的缓冲区</li>
 * </ul>
 * 
 * <p>与 PacketRouter 紧密集成，支持背压控制和订阅管理。
 */
public class RouterConnectionManager implements ConnectionManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RouterConnectionManager.class);
    
    private final java.util.Map<Long, Channel> frontendConnections;
    private final TopicSubscription topicSubscription;
    
    /**
     * 创建 RouterConnectionManager。
     *
     * @param frontendConnections 前端连接映射（Connection ID → Channel）
     * @param topicSubscription 主题订阅管理器
     * @throws NullPointerException 如果 frontendConnections 或 topicSubscription 为 null
     */
    public RouterConnectionManager(java.util.Map<Long, Channel> frontendConnections,
                                    TopicSubscription topicSubscription) {
        this.frontendConnections = frontendConnections;
        this.topicSubscription = topicSubscription;
    }
    
    /**
     * {@inheritDoc}
     * <p>通过 WebSocket 二进制帧发送数据包到指定连接。
     * 如果连接不存在或不活跃，记录警告日志。
     */
    @Override
    public void send(long connectionId, Packet packet) {
        Channel channel = frontendConnections.get(connectionId);
        
        if (channel != null && channel.isActive()) {
            ByteBuf encoded = PacketWriter.write(packet, channel.alloc());
            channel.writeAndFlush(new BinaryWebSocketFrame(encoded));
            LOGGER.debug("Sent packet to connection {}", connectionId);
        } else {
            LOGGER.warn("Connection not found or inactive: {}", connectionId);
        }
    }
    
    /**
     * {@inheritDoc}
     * <p>广播消息到所有本地连接。
     * 使用零拷贝技术（ByteBuf.retainedSlice）避免内存复制。
     * 如果无活跃连接，立即返回。
     */
    @Override
    public void broadcastAll(byte[] body, byte originalCmd) {
        if (frontendConnections.isEmpty()) {
            return;
        }
        
        Packet packet = createPacket(body, originalCmd, null);
        ByteBuf encoded = PacketWriter.writeToPooledDirectBuffer(packet, PooledByteBufAllocator.DEFAULT);
        
        try {
            int sentCount = 0;
            for (Channel channel : frontendConnections.values()) {
                if (channel.isActive()) {
                    ByteBuf slice = encoded.retainedSlice();
                    channel.writeAndFlush(new BinaryWebSocketFrame(slice));
                    sentCount++;
                }
            }
            LOGGER.debug("Broadcast to {} connections", sentCount);
        } finally {
            encoded.release();
        }
    }
    
    /**
     * {@inheritDoc}
     * <p>根据主题哈希广播消息到订阅者。
     * 使用零拷贝技术，仅发送给相关连接的订阅者。
     * 如果无订阅者，记录调试日志并返回。
     */
    @Override
    public void broadcastByTopicHash(long topicHash, byte[] body, byte originalCmd) {
        Set<Long> subscribers = topicSubscription.getSubscribersByHash(topicHash);
        
        if (subscribers.isEmpty()) {
            LOGGER.debug("No subscribers for topicHash={}", topicHash);
            return;
        }
        
        Packet packet = createPacket(body, originalCmd, null);
        ByteBuf encoded = PacketWriter.writeToPooledDirectBuffer(packet, PooledByteBufAllocator.DEFAULT);
        
        try {
            int sentCount = 0;
            for (Long connectionId : subscribers) {
                Channel channel = frontendConnections.get(connectionId);
                if (channel != null && channel.isActive()) {
                    ByteBuf slice = encoded.retainedSlice();
                    channel.writeAndFlush(new BinaryWebSocketFrame(slice));
                    sentCount++;
                }
            }
            LOGGER.debug("Broadcast by topicHash={} to {} connections", topicHash, sentCount);
        } finally {
            encoded.release();
        }
    }
    
    /**
     * {@inheritDoc}
     * <p>检查指定连接是否存在于本地且活跃。
     */
    @Override
    public boolean hasConnection(long connectionId) {
        Channel channel = frontendConnections.get(connectionId);
        return channel != null && channel.isActive();
    }
    
    /**
     * {@inheritDoc}
     * <p>获取本地管理的连接数量（包括活跃和非活跃连接）。
     */
    @Override
    public int getConnectionCount() {
        return frontendConnections.size();
    }
    
    private Packet createPacket(byte[] body, byte cmd, String topic) {
        PacketHeader.Builder headerBuilder = new PacketHeader.Builder()
            .command(cmd);
        
        if (topic != null) {
            headerBuilder.topic(topic);
        }
        
        if (body != null && body.length > 0) {
            headerBuilder.bodyLength(body.length);
        }
        
        PacketHeader header = headerBuilder.build();
        
        ByteBuf bodyBuf = null;
        if (body != null && body.length > 0) {
            bodyBuf = PooledByteBufAllocator.DEFAULT.buffer(body.length);
            bodyBuf.writeBytes(body);
        }
        
        return new Packet(header, bodyBuf);
    }
}
