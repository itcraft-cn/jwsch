package cn.itcraft.jwsch.cli.client;

import cn.itcraft.jwsch.cli.config.TcpClientConfig;
import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketHeader;
import cn.itcraft.jwsch.common.protocol.ProtocolConsts;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TCP 客户端消息处理器。
 * 
 * <p>功能：
 * <ul>
 *   <li>接收服务器分配的 connectionId（CONNECT_RESPONSE）</li>
 *   <li>空闲时发送心跳包</li>
 *   <li>心跳超时重试</li>
 * </ul>
 * 
 * <p>心跳机制：
 * <pre>
 * WRITER_IDLE 事件触发 → 发送心跳 → 等待响应
 *   ↓ 无响应
 * 超时后重试 retryTimes 次，仍无响应则关闭连接
 * </pre>
 */
public class TcpHandler extends SimpleChannelInboundHandler<Packet> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TcpHandler.class);
    
    private final TcpClientConfig config;
    private volatile long connectionId;
    private volatile ScheduledFuture<?> heartbeatTimeoutFuture;
    private final AtomicInteger retryCount = new AtomicInteger(0);
    
    public TcpHandler() {
        this(new TcpClientConfig());
    }
    
    public TcpHandler(TcpClientConfig config) {
        this.config = config;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet msg) {
        byte command = msg.getCommand();
        
        if (command == Command.CONNECT_RESPONSE) {
            handleConnectResponse(msg);
        } else if (command == Command.HEARTBEAT) {
            handleHeartbeatResponse();
        } else {
            LOGGER.debug("Received packet: cmd={}", command);
        }
    }
    
    private void handleConnectResponse(Packet packet) {
        this.connectionId = packet.getSourceId();
        LOGGER.info("Received connectionId: {}", connectionId);
    }
    
    private void handleHeartbeatResponse() {
        cancelHeartbeatTimeout();
        retryCount.set(0);
        LOGGER.debug("Heartbeat response received, connectionId={}", connectionId);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        cancelHeartbeatTimeout();
        LOGGER.info("Channel inactive: connectionId={}, remote={}", 
            connectionId, ctx.channel().remoteAddress());
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            LOGGER.debug("Channel idle: {}, connectionId={}", e.state(), connectionId);
            sendHeartbeat(ctx);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
    
    private void sendHeartbeat(ChannelHandlerContext ctx) {
        if (connectionId == 0) {
            LOGGER.debug("Skip heartbeat, connectionId not assigned yet");
            return;
        }
        
        ByteBuf buf = ctx.alloc().buffer(ProtocolConsts.FIXED_HEADER_LENGTH);
        buf.writeByte(ProtocolConsts.MAGIC[0]);
        buf.writeByte(ProtocolConsts.MAGIC[1]);
        buf.writeShort(ProtocolConsts.FIXED_HEADER_LENGTH);
        buf.writeInt(0);
        buf.writeByte(Command.HEARTBEAT);
        buf.writeShort(0);
        buf.writeLong(connectionId);
        buf.writeLong(0);
        
        ctx.writeAndFlush(buf);
        LOGGER.debug("Heartbeat sent, connectionId={}, retryCount={}", 
            connectionId, retryCount.get());
        
        scheduleHeartbeatTimeout(ctx);
    }
    
    private void scheduleHeartbeatTimeout(ChannelHandlerContext ctx) {
        cancelHeartbeatTimeout();
        
        heartbeatTimeoutFuture = ctx.executor().schedule(() -> {
            int currentRetry = retryCount.incrementAndGet();
            int maxRetry = config.getRetryTimes();
            
            if (currentRetry >= maxRetry) {
                LOGGER.error("Heartbeat timeout, max retry reached: connectionId={}, retryCount={}", 
                    connectionId, currentRetry);
                ctx.close();
            } else {
                LOGGER.warn("Heartbeat timeout, retrying: connectionId={}, retryCount={}/{}", 
                    connectionId, currentRetry, maxRetry);
                sendHeartbeat(ctx);
            }
        }, config.getHeartbeatTimeout(), TimeUnit.SECONDS);
    }
    
    private void cancelHeartbeatTimeout() {
        if (heartbeatTimeoutFuture != null) {
            heartbeatTimeoutFuture.cancel(false);
            heartbeatTimeoutFuture = null;
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Exception caught, closing channel: connectionId={}, error={}", 
            connectionId, cause.getMessage(), cause);
        ctx.close();
    }
    
    public long getConnectionId() {
        return connectionId;
    }
}