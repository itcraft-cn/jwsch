package cn.itcraft.jwsch.common.protocol;

import cn.itcraft.jwsch.common.exception.ErrorCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.TooLongFrameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

/**
 * Packet decoder for jwsch protocol.
 * 
 * <p>Netty ChannelHandler that decodes ByteBuf into {@link Packet} objects.
 * Handles TCP stream fragmentation and reassembly, using zero-copy technique to avoid body data copying.
 * 
 * <p>Uses COMPOSITE_CUMULATOR to avoid buffer copying and limits cumulative buffer size to 2MB
 * to prevent unbounded memory growth in slow consumer or backpressure scenarios.
 */
public final class PacketDecoder extends ByteToMessageDecoder {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketDecoder.class);
    
    /**
     * Maximum cumulative buffer size (2MB).
     * 
     * <p>Exceeding this size indicates decoder cannot keep up with input rate,
     * at which point the channel is closed to prevent unbounded memory growth.
     */
    private static final int MAX_CUMULATION_BYTES = 2 * 1024 * 1024;
    
    /**
     * Composite cumulator with size limitation.
     * 
     * <p>Uses COMPOSITE_CUMULATOR to avoid buffer copying while checking total size before accumulation.
     * 
     * <p>When size exceeds limit, throws TooLongFrameException,
     * which ByteToMessageDecoder catches, releases buffers, and closes the channel.
     */
    private static final Cumulator LIMITED_COMPOSITE_CUMULATOR = (allocator, cumulation, input) -> {
        int totalSize = (cumulation != null ? cumulation.readableBytes() : 0) + input.readableBytes();
        if (totalSize > MAX_CUMULATION_BYTES) {
            throw new TooLongFrameException("Cumulation buffer exceeded " + MAX_CUMULATION_BYTES + 
                " bytes (" + totalSize + " bytes), closing channel to prevent OOM");
        }
        return COMPOSITE_CUMULATOR.cumulate(allocator, cumulation, input);
    };
    
    /**
     * 数据包总长度软上限（默认 200KB，硬上限 500KB，取值超限时强制钳制）。
     * 超限数据包被丢弃（跳过），不关闭连接。
     */
    private final int maxPacketLength;
    private final LongAdder droppedCount = new LongAdder();
    
    public PacketDecoder() {
        this(ProtocolConsts.DEFAULT_MAX_PACKET_LENGTH);
    }
    
    public PacketDecoder(int maxPacketLength) {
        setCumulator(LIMITED_COMPOSITE_CUMULATOR);
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
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < ProtocolConsts.FIXED_HEADER_LENGTH) {
            return;
        }
        
        in.markReaderIndex();
        
        if (!validateMagic(in, ctx)) {
            return;
        }
        
        short headerLength = in.readShort();
        int bodyLength = in.readInt();
        byte command = in.readByte();
        short errorCode = in.readShort();
        long sourceId = in.readLong();
        long targetId = in.readLong();
        
        if (!validateLengths(headerLength, bodyLength, ctx)) {
            return;
        }
        
        int topicLength = headerLength - ProtocolConsts.FIXED_HEADER_LENGTH;
        
        int payloadLength = topicLength + bodyLength;
        if (headerLength + bodyLength > maxPacketLength) {
            if (in.readableBytes() >= payloadLength) {
                in.skipBytes(payloadLength);
                droppedCount.increment();
                LOGGER.warn("Oversize packet dropped: total={} limit={}, remote={}",
                    headerLength + bodyLength, maxPacketLength,
                    ctx.channel() != null ? ctx.channel().remoteAddress() : null);
                return;
            }
            in.resetReaderIndex();
            return;
        }
        
        if (in.readableBytes() < payloadLength) {
            in.resetReaderIndex();
            return;
        }
        
        String topic = decodeTopic(in, topicLength);
        ByteBuf bodyBuf = decodeBody(in, bodyLength);
        
        PacketHeader header = new PacketHeader.Builder()
            .command(command)
            .errorCode(errorCode)
            .sourceId(sourceId)
            .targetId(targetId)
            .topic(topic)
            .bodyLength(bodyLength)
            .build();
        
        Packet packet = new Packet(header, bodyBuf);
        out.add(packet);
        
        LOGGER.debug("Decoded packet: cmd={}, src={}, tgt={}, topic={}, bodyLen={}",
            command, sourceId, targetId, topic, bodyLength);
    }
    
    private boolean validateMagic(ByteBuf in, ChannelHandlerContext ctx) {
        byte[] magic = new byte[2];
        in.readBytes(magic);
        
        if (magic[0] != ProtocolConsts.MAGIC[0] || magic[1] != ProtocolConsts.MAGIC[1]) {
            LOGGER.error("Invalid magic: [{}, {}]", magic[0], magic[1]);
            ctx.close();
            return false;
        }
        return true;
    }
    
    private boolean validateLengths(short headerLength, int bodyLength, ChannelHandlerContext ctx) {
        if (headerLength < ProtocolConsts.FIXED_HEADER_LENGTH) {
            LOGGER.error("Invalid header length: {}", headerLength);
            ctx.close();
            return false;
        }
        
        if (bodyLength < 0 || bodyLength > ProtocolConsts.MAX_BODY_LENGTH) {
            LOGGER.error("Invalid body length: {}", bodyLength);
            ctx.close();
            return false;
        }
        return true;
    }
    
    private String decodeTopic(ByteBuf in, int topicLength) {
        if (topicLength <= 0) {
            return null;
        }
        byte[] topicBytes = new byte[topicLength];
        in.readBytes(topicBytes);
        return new String(topicBytes, StandardCharsets.US_ASCII);
    }
    
    private ByteBuf decodeBody(ByteBuf in, int bodyLength) {
        if (bodyLength <= 0) {
            return null;
        }
        return in.readBytes(bodyLength);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof TooLongFrameException) {
            LOGGER.error("Cumulation buffer overflow, closing channel: {}", cause.getMessage());
        } else if (cause instanceof DecoderException) {
            LOGGER.error("Decoder error, closing channel: {}", cause.getMessage());
        } else {
            LOGGER.error("Unexpected error in decoder, closing channel", cause);
        }
        ctx.close();
    }
}