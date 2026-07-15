package cn.itcraft.jwsch.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

/**
 * 数据包写入工具类。
 * 
 * <p>提供静态方法将 {@link Packet} 编码为 ByteBuf。
 * 与 {@link PacketEncoder} 功能相同，但可在任意位置调用，无需 Netty Pipeline。
 * 
 * <p>使用零拷贝技术，支持同一个 Packet 发送给多个客户端。
 */
public final class PacketWriter {
    
    private PacketWriter() {
    }
    
    /**
     * 将 Packet 编码为 ByteBuf。
     * 
     * <p>使用零拷贝技术，从指定位置读取 Body 数据，不移动 readerIndex，
     * 允许同一个 Body 发送给多个客户端。
     * 
     * @param packet  待编码的数据包
     * @param allocator ByteBuf 分配器
     * @return 编码后的 ByteBuf，调用者负责释放
     */
    public static ByteBuf write(Packet packet, ByteBufAllocator allocator) {
        PacketHeader header = packet.getHeader();
        ByteBuf body = packet.getBodyBuf();
        
        int bodyLength = body != null ? body.readableBytes() : 0;
        int totalLength = header.getHeaderLength() + bodyLength;
        
        ByteBuf buf = allocator.buffer(totalLength);
        
        // 写入固定头部
        buf.writeByte(ProtocolConsts.MAGIC[0]);
        buf.writeByte(ProtocolConsts.MAGIC[1]);
        buf.writeShort(header.getHeaderLength());
        buf.writeInt(bodyLength);
        buf.writeByte(header.getCommand());
        buf.writeShort(header.getErrorCode());
        buf.writeLong(header.getSourceId());
        buf.writeLong(header.getTargetId());
        
        // 写入变长 Topic（使用缓存的 topicBytes）
        byte[] topicBytes = header.getTopicBytes();
        if (topicBytes != null) {
            buf.writeBytes(topicBytes);
        }
        
        // 写入 Body（零拷贝：指定读取位置，不移动 readerIndex）
        if (body != null && body.readableBytes() > 0) {
            buf.writeBytes(body, body.readerIndex(), bodyLength);
        }
        
        return buf;
    }
    
    /**
     * 将 Packet 编码为字节数组。
     * 
     * <p>用于广播场景：编码一次，分发给多个订阅者时使用 {@link Unpooled#wrappedBuffer(byte[])} 
     * 创建堆缓冲区，避免在扇出过程中持有直接内存引用。
     * 
     * @param packet 待编码的数据包
     * @return 编码后的字节数组
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
     * 将 Packet 编码为池化 Direct ByteBuf。
     * 
     * <p>用于广播场景：编码一次，通过 retainedSlice() 分发给多个订阅者，
     * 避免创建多个 ByteBuf wrapper，减少内存分配和 GC。
     * 
     * <p>内存优化：
     * <pre>
     * 原方案：byte[] + Unpooled.wrappedBuffer() × N → N 个非池化 HeapByteBuf
     * 新方案：Pooled Direct ByteBuf + retainedSlice() × N → N 个轻量 slice，共享底层内存
     * </pre>
     * 
     * @param packet 待编码的数据包
     * @param allocator ByteBuf 分配器（推荐使用 PooledByteBufAllocator.DEFAULT）
     * @return 编码后的池化 Direct ByteBuf，调用者负责释放
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