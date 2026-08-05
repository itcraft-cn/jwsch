package cn.itcraft.jwsch.common.protocol;

import cn.itcraft.jwsch.common.exception.ErrorCode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 */
public final class PacketEncoder extends MessageToByteEncoder<Packet> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketEncoder.class);
    
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