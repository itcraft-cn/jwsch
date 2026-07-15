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
 */
public class RouterConnectionManager implements ConnectionManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RouterConnectionManager.class);
    
    private final java.util.Map<Long, Channel> frontendConnections;
    private final TopicSubscription topicSubscription;
    
    public RouterConnectionManager(java.util.Map<Long, Channel> frontendConnections,
                                    TopicSubscription topicSubscription) {
        this.frontendConnections = frontendConnections;
        this.topicSubscription = topicSubscription;
    }
    
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
    
    @Override
    public boolean hasConnection(long connectionId) {
        Channel channel = frontendConnections.get(connectionId);
        return channel != null && channel.isActive();
    }
    
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
