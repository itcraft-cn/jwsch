package cn.itcraft.jwsch.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

/**
 * Packet writer utility class.
 * 
 * <p>Provides static methods to encode {@link Packet} objects into ByteBuf.
 * Functionally similar to {@link PacketEncoder} but can be called anywhere without Netty Pipeline.
 * 
 * <p>Uses zero-copy technique, supporting sending the same Packet to multiple clients.
 */
public final class PacketWriter {
    
    private PacketWriter() {
    }
    
    /**
     * Encodes Packet into ByteBuf.
     * 
     * <p>Uses zero-copy technique: reads body data from specified position without moving readerIndex,
     * allowing the same body to be sent to multiple clients.
     * 
     * @param packet      packet to encode
     * @param allocator   ByteBuf allocator
     * @return encoded ByteBuf, caller is responsible for releasing
     */
    public static ByteBuf write(Packet packet, ByteBufAllocator allocator) {
        PacketHeader header = packet.getHeader();
        ByteBuf body = packet.getBodyBuf();
        
        int bodyLength = body != null ? body.readableBytes() : 0;
        int totalLength = header.getHeaderLength() + bodyLength;
        
        ByteBuf buf = allocator.buffer(totalLength);
        
        // Write fixed header
        buf.writeByte(ProtocolConsts.MAGIC[0]);
        buf.writeByte(ProtocolConsts.MAGIC[1]);
        buf.writeShort(header.getHeaderLength());
        buf.writeInt(bodyLength);
        buf.writeByte(header.getCommand());
        buf.writeShort(header.getErrorCode());
        buf.writeLong(header.getSourceId());
        buf.writeLong(header.getTargetId());
        
        // Write variable-length Topic (using cached topicBytes)
        byte[] topicBytes = header.getTopicBytes();
        if (topicBytes != null) {
            buf.writeBytes(topicBytes);
        }
        
        // Write Body (zero-copy: read from specified position without moving readerIndex)
        if (body != null && body.readableBytes() > 0) {
            buf.writeBytes(body, body.readerIndex(), bodyLength);
        }
        
        return buf;
    }
    
    /**
     * Encodes Packet into byte array.
     * 
     * <p>Used for broadcast scenarios: encode once, then use {@link Unpooled#wrappedBuffer(byte[])}
     * to create heap buffers for distribution to multiple subscribers, avoiding holding direct memory
     * references during fan-out.
     * 
     * @param packet packet to encode
     * @return encoded byte array
     */
    public static byte[] writeToBytes(Packet packet) {
        PacketHeader header = packet.getHeader();
        ByteBuf body = packet.getBodyBuf();
        
        int bodyLength = body != null ? body.readableBytes() : 0;
        int totalLength = header.getHeaderLength() + bodyLength;
        
        ByteBuf buf = Unpooled.buffer(totalLength);
        try {
            buf.writeByte(ProtocolConsts.MAGIC[0]);
            buf.writeByte(ProtocolConsts.MAGIC[1]);
            buf.writeShort(header.getHeaderLength());
            buf.writeInt(bodyLength);
            buf.writeByte(header.getCommand());
            buf.writeShort(header.getErrorCode());
            buf.writeLong(header.getSourceId());
            buf.writeLong(header.getTargetId());
            
            byte[] topicBytes = header.getTopicBytes();
            if (topicBytes != null) {
                buf.writeBytes(topicBytes);
            }
            
            if (body != null && body.readableBytes() > 0) {
                buf.writeBytes(body, body.readerIndex(), bodyLength);
            }
            
            return ByteBufUtil.getBytes(buf);
        } finally {
            buf.release();
        }
    }
    
    /**
     * Encodes Packet into pooled Direct ByteBuf.
     * 
     * <p>Used for broadcast scenarios: encode once, distribute to multiple subscribers via retainedSlice(),
     * avoiding creating multiple ByteBuf wrappers, reducing memory allocation and GC.
     * 
     * <p>Memory optimization:
     * <pre>
     * Old approach: byte[] + Unpooled.wrappedBuffer() × N → N non-pooled HeapByteBuf
     * New approach: Pooled Direct ByteBuf + retainedSlice() × N → N lightweight slices sharing underlying memory
     * </pre>
     * 
     * @param packet      packet to encode
     * @param allocator   ByteBuf allocator (recommended: PooledByteBufAllocator.DEFAULT)
     * @return encoded pooled Direct ByteBuf, caller is responsible for releasing
     */
    public static ByteBuf writeToPooledDirectBuffer(Packet packet, ByteBufAllocator allocator) {
        PacketHeader header = packet.getHeader();
        ByteBuf body = packet.getBodyBuf();
        
        int bodyLength = body != null ? body.readableBytes() : 0;
        int totalLength = header.getHeaderLength() + bodyLength;
        
        ByteBuf buf = allocator.directBuffer(totalLength);
        
        buf.writeByte(ProtocolConsts.MAGIC[0]);
        buf.writeByte(ProtocolConsts.MAGIC[1]);
        buf.writeShort(header.getHeaderLength());
        buf.writeInt(bodyLength);
        buf.writeByte(header.getCommand());
        buf.writeShort(header.getErrorCode());
        buf.writeLong(header.getSourceId());
        buf.writeLong(header.getTargetId());
        
        byte[] topicBytes = header.getTopicBytes();
        if (topicBytes != null) {
            buf.writeBytes(topicBytes);
        }
        
        if (body != null && body.readableBytes() > 0) {
            buf.writeBytes(body, body.readerIndex(), bodyLength);
        }
        
        return buf;
    }
}