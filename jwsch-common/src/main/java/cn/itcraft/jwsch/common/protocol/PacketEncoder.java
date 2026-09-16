package cn.itcraft.jwsch.common.protocol;

import cn.itcraft.jwsch.common.exception.ErrorCode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.LongAdder;
import java.nio.charset.StandardCharsets;

/**
 * Packet encoder for jwsch protocol.
 * 
 * <p>Netty ChannelHandler that encodes {@link Packet} objects into ByteBuf for network transmission.
 * Encoding order follows the protocol format defined in {@link ProtocolConsts}.
 * 
 * <p>Uses zero-copy technique to avoid unnecessary body data copying.
 * When writing body content, uses {@link ByteBuf#writeBytes(ByteBuf, int, int)} with 
 * readerIndex and readableBytes to prevent moving the original buffer's readerIndex,
 * allowing the same packet to be sent to multiple clients.
 * 
 * <p>Packet size constraint: packets whose total length (header + body)
 * exceeds the configured soft limit are dropped (not sent), promise completes
 * successfully; the limit is clamped to {@link ProtocolConsts#MAX_PACKET_LENGTH_LIMIT}.
 */
public final class PacketEncoder extends MessageToByteEncoder<Packet> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketEncoder.class);
    
    private final int maxPacketLength;
    private final LongAdder droppedCount = new LongAdder();
    
    public PacketEncoder() {
        this(ProtocolConsts.DEFAULT_MAX_PACKET_LENGTH);
    }
    
    public PacketEncoder(int maxPacketLength) {
        this.maxPacketLength = cn.itcraft.jwsch.common.config.TcpConfig.normalizePacketLimit(maxPacketLength);
    }
    
    /**
     * 获取累计丢弃的过大数据包数量。
     *
     * @return 丢弃包总数
     */
    public long getDroppedCount() {
        return droppedCount.sum();
    }
    
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Packet && totalLength((Packet) msg) > maxPacketLength) {
            Packet packet = (Packet) msg;
            droppedCount.increment();
            LOGGER.warn("Oversize packet dropped on write: total={} limit={}, cmd={}, topic={}",
                totalLength(packet), maxPacketLength, packet.getCommand(), packet.getTopic());
            try {
                io.netty.util.ReferenceCountUtil.release(msg);
            } catch (Exception ignore) {
                LOGGER.debug("Release oversize packet failed", ignore);
            }
            promise.trySuccess();
            return;
        }
        
        super.write(ctx, msg, promise);
    }
    
    private int totalLength(Packet packet) {
        PacketHeader header = packet.getHeader();
        ByteBuf bodyBuf = packet.getBodyBuf();
        int bodyLength = bodyBuf != null ? bodyBuf.readableBytes() : 0;
        return header.getHeaderLength() + bodyLength;
    }
    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, ByteBuf out) {
        PacketHeader header = msg.getHeader();
        
        // Write fixed header
        out.writeBytes(ProtocolConsts.MAGIC);
        out.writeShort(header.getHeaderLength());
        out.writeInt(header.getBodyLength());
        out.writeByte(header.getCommand());
        out.writeShort(header.getErrorCode());
        out.writeLong(header.getSourceId());
        out.writeLong(header.getTargetId());
        
        // Write variable-length Topic (using cached topicBytes)
        byte[] topicBytes = header.getTopicBytes();
        if (topicBytes != null && topicBytes.length > 0) {
            out.writeBytes(topicBytes);
        }
        
        // Write Body (zero-copy: specify read position without moving readerIndex)
        ByteBuf bodyBuf = msg.getBodyBuf();
        if (bodyBuf != null && bodyBuf.isReadable()) {
            out.writeBytes(bodyBuf, bodyBuf.readerIndex(), bodyBuf.readableBytes());
        }
        
        LOGGER.debug("Encoded packet: cmd={}, src={}, tgt={}, topic={}, bodyLen={}",
            header.getCommand(), header.getSourceId(), header.getTargetId(),
            header.getTopic(), header.getBodyLength());
    }
}