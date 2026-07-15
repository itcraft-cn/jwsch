package cn.itcraft.jwsch.srv.server.websocket;

import cn.itcraft.jwsch.common.exception.ErrorCode;
import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketHeader;
import cn.itcraft.jwsch.common.protocol.ProtocolConsts;
import cn.itcraft.jwsch.common.id.IdGenerator;
import cn.itcraft.jwsch.srv.flowcontrol.TopicBackpressureManager;
import cn.itcraft.jwsch.srv.metrics.NoOpServerMetrics;
import cn.itcraft.jwsch.srv.metrics.ServerMetrics;
import cn.itcraft.jwsch.srv.router.PacketRouter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;
import io.netty.util.ReferenceCountUtil;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 帧处理器。
 */
public class WebSocketHandler extends ChannelInboundHandlerAdapter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketHandler.class);
    
    private final PacketRouter packetRouter;
    private final ServerMetrics serverMetrics;
    private final int slowQueryThresholdMs;
    private Long connectionId;
    
    public WebSocketHandler(PacketRouter packetRouter) {
        this(packetRouter, NoOpServerMetrics.INSTANCE, 0);
    }
    
    public WebSocketHandler(PacketRouter packetRouter, ServerMetrics serverMetrics) {
        this(packetRouter, serverMetrics, 0);
    }
    
    public WebSocketHandler(PacketRouter packetRouter, ServerMetrics serverMetrics, int slowQueryThresholdMs) {
        this.packetRouter = packetRouter;
        this.serverMetrics = serverMetrics;
        this.slowQueryThresholdMs = slowQueryThresholdMs;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        connectionId = IdGenerator.nextId();
        packetRouter.addFrontendConnection(connectionId, ctx.channel());
        serverMetrics.incrementWebSocketConnections();
        LOGGER.debug("TCP connection active: connectionId={}, waiting for WebSocket handshake", connectionId);
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof HandshakeComplete) {
            sendConnectResponse(ctx);
            LOGGER.info("WebSocket connected: connectionId={}, remote={}", 
                connectionId, ctx.channel().remoteAddress());
            return;
        }
        if (evt instanceof IdleStateEvent) {
            LOGGER.debug("Channel idle, closing: connectionId={}", connectionId);
            ctx.close();
        }
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (connectionId != null) {
            packetRouter.removeFrontendConnection(connectionId);
            serverMetrics.decrementWebSocketConnections();
            LOGGER.info("WebSocket disconnected: connectionId={}", connectionId);
        }
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof WebSocketFrame)) {
            return;
        }
        
        WebSocketFrame frame = (WebSocketFrame) msg;
        
        try {
            if (frame instanceof BinaryWebSocketFrame) {
                handleBinaryFrame(ctx, (BinaryWebSocketFrame) frame);
            } else if (frame instanceof TextWebSocketFrame) {
                handleTextFrame(ctx, (TextWebSocketFrame) frame);
            } else if (frame instanceof PingWebSocketFrame) {
                handlePingFrame(ctx, (PingWebSocketFrame) frame);
            } else if (frame instanceof PongWebSocketFrame) {
                handlePongFrame(ctx, (PongWebSocketFrame) frame);
            } else if (frame instanceof CloseWebSocketFrame) {
                handleCloseFrame(ctx, (CloseWebSocketFrame) frame);
            }
        } finally {
            ReferenceCountUtil.release(frame);
        }
    }
    
    private void handleBinaryFrame(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
        long startTime = System.nanoTime();
        ByteBuf content = frame.content();
        int frameSize = content.readableBytes();
        
        serverMetrics.recordPacketReceived(frameSize);
        
        try {
            if (content.readableBytes() < ProtocolConsts.FIXED_HEADER_LENGTH) {
                LOGGER.warn("Packet too short: {}", content.readableBytes());
                serverMetrics.recordError(ErrorCode.DECODE_FAILED);
                return;
            }
            
            byte m0 = content.readByte();
            byte m1 = content.readByte();
            
            if (m0 != ProtocolConsts.MAGIC[0] || m1 != ProtocolConsts.MAGIC[1]) {
                LOGGER.error("Invalid magic: [{}, {}]", m0, m1);
                serverMetrics.recordError(ErrorCode.INVALID_MAGIC);
                return;
            }
            
            short headerLength = content.readShort();
            int bodyLength = content.readInt();
            byte command = content.readByte();
            short errorCode = content.readShort();
            long sourceId = content.readLong();
            long targetId = content.readLong();
            
            int topicLength = headerLength - ProtocolConsts.FIXED_HEADER_LENGTH;
            
            String topic = null;
            if (topicLength > 0 && content.readableBytes() >= topicLength) {
                byte[] topicBytes = new byte[topicLength];
                content.readBytes(topicBytes);
                topic = new String(topicBytes, StandardCharsets.US_ASCII);
            }
            
            ByteBuf bodyBuf = null;
            if (bodyLength > 0 && content.readableBytes() >= bodyLength) {
                bodyBuf = content.slice(content.readerIndex(), bodyLength);
            }
            
            PacketHeader header = new PacketHeader.Builder()
                .command(command)
                .errorCode(errorCode)
                .sourceId(sourceId)
                .targetId(targetId)
                .topic(topic)
                .bodyLength(bodyLength)
                .build();
            
            Packet packet = new Packet(header, bodyBuf);
            handlePacket(ctx, packet);
        } finally {
            long durationNs = System.nanoTime() - startTime;
            long durationMs = durationNs / 1_000_000;
            
            serverMetrics.recordProcessTime(durationNs, TimeUnit.NANOSECONDS);
            
            if (slowQueryThresholdMs > 0 && durationMs > slowQueryThresholdMs) {
                LOGGER.warn("Slow query detected: connectionId={}, duration={}ms, threshold={}ms",
                    connectionId, durationMs, slowQueryThresholdMs);
            }
        }
    }
    
    private void handlePacket(ChannelHandlerContext ctx, Packet packet) {
        byte command = packet.getHeader().getCommand();
        String topic = packet.getHeader().getTopic();
        
        LOGGER.debug("Received packet: connectionId={}, command={}, topic={}", 
            connectionId, command, topic);
        
        switch (command) {
            case Command.SUBSCRIBE:
                if (topic != null) {
                    packetRouter.handleSubscribe(topic, connectionId);
                    LOGGER.info("Subscribed: connectionId={}, topic={}", connectionId, topic);
                }
                break;
            case Command.HEARTBEAT:
                sendHeartbeatResponse(ctx);
                break;
            case Command.REQUEST:
            case Command.PUSH:
            case Command.BROADCAST:
                LOGGER.debug("Received {} from frontend: connectionId={}", 
                    command, connectionId);
                break;
            default:
                LOGGER.warn("Unknown command: {}", command);
        }
    }
    
    private void sendHeartbeatResponse(ChannelHandlerContext ctx) {
        ByteBuf buf = ctx.alloc().buffer(ProtocolConsts.FIXED_HEADER_LENGTH);
        buf.writeByte(ProtocolConsts.MAGIC[0]);
        buf.writeByte(ProtocolConsts.MAGIC[1]);
        buf.writeShort(ProtocolConsts.FIXED_HEADER_LENGTH);
        buf.writeInt(0);
        buf.writeByte(Command.HEARTBEAT);
        buf.writeShort(0);
        buf.writeLong(connectionId);
        buf.writeLong(0);
        
        ctx.writeAndFlush(new BinaryWebSocketFrame(buf));
        LOGGER.debug("Sent heartbeat response: connectionId={}", connectionId);
    }
    
    private void sendConnectResponse(ChannelHandlerContext ctx) {
        ByteBuf buf = ctx.alloc().buffer(ProtocolConsts.FIXED_HEADER_LENGTH);
        buf.writeByte(ProtocolConsts.MAGIC[0]);
        buf.writeByte(ProtocolConsts.MAGIC[1]);
        buf.writeShort(ProtocolConsts.FIXED_HEADER_LENGTH);
        buf.writeInt(0);
        buf.writeByte(Command.CONNECT_RESPONSE);
        buf.writeShort(0);
        buf.writeLong(connectionId);
        buf.writeLong(0);
        
        ctx.writeAndFlush(new BinaryWebSocketFrame(buf));
        LOGGER.debug("Sent connect response: connectionId={}", connectionId);
    }
    
    private void handleTextFrame(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String text = frame.text();
        LOGGER.debug("Received text frame: connectionId={}, text={}", connectionId, text);
        
        if (text.startsWith("subscribe:")) {
            String topic = text.substring("subscribe:".length());
            packetRouter.handleSubscribe(topic, connectionId);
            ctx.writeAndFlush(new TextWebSocketFrame("subscribed:" + topic));
            LOGGER.info("Subscribed via text: connectionId={}, topic={}", connectionId, topic);
        } else {
            ctx.writeAndFlush(new TextWebSocketFrame("echo:" + text));
        }
    }
    
    private void handlePingFrame(ChannelHandlerContext ctx, PingWebSocketFrame frame) {
        LOGGER.debug("Received ping frame: connectionId={}", connectionId);
        ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
    }
    
    private void handlePongFrame(ChannelHandlerContext ctx, PongWebSocketFrame frame) {
        LOGGER.debug("Received pong frame: connectionId={}", connectionId);
    }
    
    private void handleCloseFrame(ChannelHandlerContext ctx, CloseWebSocketFrame frame) {
        LOGGER.debug("Received close frame: connectionId={}", connectionId);
        ctx.close();
    }
    
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        boolean writable = ctx.channel().isWritable();
        packetRouter.getBackpressureManager().onFrontendWritabilityChanged(ctx.channel());
        
        if (connectionId != null) {
            notifyTopicBackpressureManager(writable);
        }
        
        if (!writable) {
            LOGGER.debug("Frontend channel non-writable (backpressure): connectionId={}", connectionId);
        } else {
            LOGGER.debug("Frontend channel writable again: connectionId={}", connectionId);
        }
    }
    
    private void notifyTopicBackpressureManager(boolean writable) {
        TopicBackpressureManager tbm = packetRouter.getTopicBackpressureManager();
        if (tbm == null) {
            return;
        }
        
        Set<Long> topicHashes = packetRouter.getTopicSubscription().getTopicHashesForConnection(connectionId);
        for (Long topicHash : topicHashes) {
            tbm.onSubscriberWritabilityChanged(topicHash, connectionId, writable);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Exception caught: connectionId={}", connectionId, cause);
        serverMetrics.recordError(ErrorCode.INTERNAL_ERROR);
        ctx.close();
    }
}