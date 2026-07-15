package cn.itcraft.jwsch.common.protocol;

import cn.itcraft.jwsch.common.exception.ErrorCode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * 数据包编码器。
 * 
 * <p>Netty ChannelHandler，将 {@link Packet} 编码为 ByteBuf 用于网络传输。
 * 编码顺序与协议格式一致，参见 {@link ProtocolConsts}。
 * 
 * <p>使用零拷贝技术，避免 body 数据复制。
 */
public final class PacketEncoder extends MessageToByteEncoder<Packet> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketEncoder.class);
    
    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, ByteBuf out) {
        PacketHeader header = msg.getHeader();
        
        // 写入固定头部
        out.writeBytes(ProtocolConsts.MAGIC);
        out.writeShort(header.getHeaderLength());
        out.writeInt(header.getBodyLength());
        out.writeByte(header.getCommand());
        out.writeShort(header.getErrorCode());
        out.writeLong(header.getSourceId());
        out.writeLong(header.getTargetId());
        
        // 写入变长 Topic（使用缓存的 topicBytes）
        byte[] topicBytes = header.getTopicBytes();
        if (topicBytes != null && topicBytes.length > 0) {
            out.writeBytes(topicBytes);
        }
        
        // 写入 Body（零拷贝：指定读取位置，不移动 readerIndex）
        ByteBuf bodyBuf = msg.getBodyBuf();
        if (bodyBuf != null && bodyBuf.isReadable()) {
            out.writeBytes(bodyBuf, bodyBuf.readerIndex(), bodyBuf.readableBytes());
        }
        
        LOGGER.debug("Encoded packet: cmd={}, src={}, tgt={}, topic={}, bodyLen={}",
            header.getCommand(), header.getSourceId(), header.getTargetId(),
            header.getTopic(), header.getBodyLength());
    }
}