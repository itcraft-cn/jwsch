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

/**
 * 数据包解码器。
 * 
 * <p>Netty ChannelHandler，将 ByteBuf 解码为 {@link Packet} 对象。
 * 支持粘包拆包处理，使用零拷贝技术避免 Body 数据复制。
 * 
 * <p>使用 COMPOSITE_CUMULATOR 避免缓冲区复制，
 * 并限制累积缓冲区最大大小为 2MB，
 * 防止慢消费者或背压场景下的内存无限增长。
 */
public final class PacketDecoder extends ByteToMessageDecoder {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketDecoder.class);
    
    /**
     * 最大累积缓冲区大小（2MB）。
     * 
     * <p>超过此大小表示解码器处理跟不上输入速率，
     * 此时关闭通道以避免内存无限增长。
     */
    private static final int MAX_CUMULATION_BYTES = 2 * 1024 * 1024;
    
    /**
     * 带大小限制的复合累积器。
     * 
     * <p>使用 COMPOSITE_CUMULATOR 避免缓冲区复制，
     * 同时在累积前检查总大小是否超过限制。
     * 
     * <p>当大小超过限制时抛出 TooLongFrameException，
     * ByteToMessageDecoder 会捕获并释放缓冲区，然后关闭通道。
     */
    private static final Cumulator LIMITED_COMPOSITE_CUMULATOR = (allocator, cumulation, input) -> {
        int totalSize = (cumulation != null ? cumulation.readableBytes() : 0) + input.readableBytes();
        if (totalSize > MAX_CUMULATION_BYTES) {
            throw new TooLongFrameException("Cumulation buffer exceeded " + MAX_CUMULATION_BYTES + 
                " bytes (" + totalSize + " bytes), closing channel to prevent OOM");
        }
        return COMPOSITE_CUMULATOR.cumulate(allocator, cumulation, input);
    };
    
    public PacketDecoder() {
        setCumulator(LIMITED_COMPOSITE_CUMULATOR);
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
        
        if (in.readableBytes() < topicLength + bodyLength) {
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