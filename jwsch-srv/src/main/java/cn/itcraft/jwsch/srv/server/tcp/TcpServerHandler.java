package cn.itcraft.jwsch.srv.server.tcp;

import cn.itcraft.jwsch.common.exception.ErrorCode;
import cn.itcraft.jwsch.common.id.IdGenerator;
import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketHeader;
import cn.itcraft.jwsch.common.protocol.ProtocolConsts;
import cn.itcraft.jwsch.srv.metrics.NoOpServerMetrics;
import cn.itcraft.jwsch.srv.metrics.ServerMetrics;
import cn.itcraft.jwsch.srv.router.PacketRouter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP 服务端处理器。
 * 
 * <p>处理后端服务（如 sample-pusher）发送的 Packet：
 * <ul>
 *   <li>PUSH：推送到指定 Topic 的所有订阅者</li>
 *   <li>BROADCAST：广播给所有前端连接</li>
 *   <li>REQUEST：路由到指定目标连接</li>
 * </ul>
 * 
 * <p>消息流程：
 * <pre>
 * sample-pusher → TCP (PUSH) → TcpServerHandler.handlePush() 
 *              → PacketRouter.broadcastToTopic() → WebSocket Clients
 * </pre>
 */
@ChannelHandler.Sharable
public class TcpServerHandler extends ChannelInboundHandlerAdapter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TcpServerHandler.class);
    private static final AttributeKey<Long> CONNECTION_ID_KEY = AttributeKey.valueOf("connectionId");
    
    private final PacketRouter packetRouter;
    private final ServerMetrics serverMetrics;
    
    public TcpServerHandler(PacketRouter packetRouter) {
        this(packetRouter, NoOpServerMetrics.INSTANCE);
    }
    
    public TcpServerHandler(PacketRouter packetRouter, ServerMetrics serverMetrics) {
        this.packetRouter = packetRouter;
        this.serverMetrics = serverMetrics != null ? serverMetrics : NoOpServerMetrics.INSTANCE;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        long connectionId = IdGenerator.nextId();
        ctx.channel().attr(CONNECTION_ID_KEY).set(connectionId);
        sendConnectResponse(ctx, connectionId);
        
        packetRouter.getBackpressureManager().registerTcpChannel(ctx.channel());
        serverMetrics.incrementTcpConnections();
        
        LOGGER.info("TCP client connected: connectionId={}, remote={}", 
            connectionId, ctx.channel().remoteAddress());
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Long connectionId = ctx.channel().attr(CONNECTION_ID_KEY).get();
        
        packetRouter.getBackpressureManager().unregisterTcpChannel(ctx.channel());
        serverMetrics.decrementTcpConnections();
        
        LOGGER.info("TCP client disconnected: connectionId={}, remote={}", 
            connectionId, ctx.channel().remoteAddress());
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof Packet)) {
            return;
        }
        
        Packet packet = (Packet) msg;
        
        int packetSize = ProtocolConsts.FIXED_HEADER_LENGTH 
            + (packet.getHeader().getTopicBytes() != null ? packet.getHeader().getTopicBytes().length : 0)
            + packet.getHeader().getBodyLength();
        serverMetrics.recordPacketReceived(packetSize);
        
        try {
            byte command = packet.getHeader().getCommand();
            String topic = packet.getHeader().getTopic();
            
            LOGGER.debug("Received packet from TCP: command={}, topic={}", command, topic);
            
            switch (command) {
                case Command.PUSH:
                    handlePush(packet);
                    break;
                case Command.BROADCAST:
                    handleBroadcast(packet);
                    break;
                case Command.REQUEST:
                    handleRequest(ctx, packet);
                    break;
                case Command.HEARTBEAT:
                    handleHeartbeat(ctx, packet);
                    packet = null;
                    break;
                default:
                    LOGGER.warn("Unknown command from TCP: {}", command);
            }
        } finally {
            if (packet != null) {
                packet.release();
            }
        }
    }
    
    /**
     * 处理 PUSH 命令。
     * 
     * <p>将消息推送到指定 Topic 的所有订阅者。
     * 使用 Packet.retain() 增加引用计数，broadcastToTopic 后 release。
     * 
     * @param packet 待推送的数据包
     */
    private void handlePush(Packet packet) {
        String topic = packet.getHeader().getTopic();
        
        LOGGER.debug("Handling PUSH: topic={}, subscribers={}", 
            topic, packetRouter.getTopicSubscription().getSubscriberCount(topic));
        
        if (topic != null && packetRouter.getTopicSubscription().hasTopic(topic)) {
            packet.retain();
            try {
                packetRouter.broadcastToTopic(topic, packet);
                LOGGER.debug("Pushed message to topic: {}, subscribers={}", 
                    topic, packetRouter.getTopicSubscription().getSubscriberCount(topic));
            } finally {
                packet.release();
            }
        }
        
        if (packetRouter.isClusterEnabled()) {
            packet.retain();
            try {
                packetRouter.routeToCluster(packet);
            } finally {
                packet.release();
            }
        }
    }
    
    /**
     * 处理 BROADCAST 命令。
     * 
     * <p>广播给所有前端 WebSocket 连接。
     * 
     * @param packet 待广播的数据包
     */
    private void handleBroadcast(Packet packet) {
        packetRouter.broadcast(packet);
        LOGGER.debug("Broadcast message to {} frontend connections", 
            packetRouter.getFrontendConnectionCount());
        
        if (packetRouter.isClusterEnabled()) {
            packet.retain();
            try {
                packetRouter.routeToCluster(packet);
            } finally {
                packet.release();
            }
        }
    }
    
    private void handleRequest(ChannelHandlerContext ctx, Packet packet) {
        long targetId = packet.getHeader().getTargetId();
        
        if (targetId > 0) {
            packetRouter.routeToFrontend(packet);
        } else {
            LOGGER.warn("Request has no targetId, topic={}", packet.getHeader().getTopic());
        }
    }
    
    private void handleHeartbeat(ChannelHandlerContext ctx, Packet packet) {
        Long connectionId = ctx.channel().attr(CONNECTION_ID_KEY).get();
        
        ByteBuf buf = ctx.alloc().buffer(ProtocolConsts.FIXED_HEADER_LENGTH);
        buf.writeByte(ProtocolConsts.MAGIC[0]);
        buf.writeByte(ProtocolConsts.MAGIC[1]);
        buf.writeShort(ProtocolConsts.FIXED_HEADER_LENGTH);
        buf.writeInt(0);
        buf.writeByte(Command.HEARTBEAT);
        buf.writeShort(0);
        buf.writeLong(connectionId != null ? connectionId : 0L);
        buf.writeLong(0);
        
        ctx.writeAndFlush(buf);
        packet.release();
        LOGGER.debug("Heartbeat response sent: connectionId={}", connectionId);
    }
    
    private void sendConnectResponse(ChannelHandlerContext ctx, long connectionId) {
        ByteBuf buf = ctx.alloc().buffer(ProtocolConsts.FIXED_HEADER_LENGTH);
        buf.writeByte(ProtocolConsts.MAGIC[0]);
        buf.writeByte(ProtocolConsts.MAGIC[1]);
        buf.writeShort(ProtocolConsts.FIXED_HEADER_LENGTH);
        buf.writeInt(0);
        buf.writeByte(Command.CONNECT_RESPONSE);
        buf.writeShort(0);
        buf.writeLong(connectionId);
        buf.writeLong(0);
        
        ctx.writeAndFlush(buf);
        LOGGER.debug("Sent connect response: connectionId={}", connectionId);
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                LOGGER.info("TCP client idle timeout: {}", ctx.channel().remoteAddress());
                ctx.close();
            }
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("TCP server error, closing channel: {}", cause.getMessage(), cause);
        
        serverMetrics.recordError(ErrorCode.INTERNAL_ERROR);
        
        ctx.close();
    }
}