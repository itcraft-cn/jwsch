package cn.itcraft.jwsch.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;

import java.util.Objects;

/**
 * 二进制协议数据包。
 * 
 * <p>实现 {@link ReferenceCounted} 接口，支持引用计数管理。
 * 当 Packet 需要发送给多个客户端时，调用 {@link #retain()} 增加引用计数，
 * 发送完成后调用 {@link #release()} 释放资源。
 * 
 * <p>使用示例：
 * <pre>
 * Packet packet = new Packet(header, bodyBuf);
 * packet.retain();  // 发送给多个客户端前 retain
 * router.broadcastToTopic(topic, packet);
 * packet.release(); // 广播完成后 release
 * </pre>
 */
public final class Packet implements ReferenceCounted {
    
    private final PacketHeader header;
    private final ByteBuf bodyBuf;
    
    public Packet(PacketHeader header, ByteBuf bodyBuf) {
        this.header = Objects.requireNonNull(header, "header cannot be null");
        this.bodyBuf = bodyBuf;
    }
    
    public PacketHeader getHeader() {
        return header;
    }
    
    public ByteBuf getBodyBuf() {
        return bodyBuf;
    }
    
    public byte getCommand() {
        return header.getCommand();
    }
    
    public short getErrorCode() {
        return header.getErrorCode();
    }
    
    public long getSourceId() {
        return header.getSourceId();
    }
    
    public long getTargetId() {
        return header.getTargetId();
    }
    
    public String getTopic() {
        return header.getTopic();
    }
    
    public boolean hasBody() {
        return bodyBuf != null && bodyBuf.readableBytes() > 0;
    }
    
    @Override
    public int refCnt() {
        return bodyBuf != null ? bodyBuf.refCnt() : 0;
    }
    
    @Override
    public Packet retain() {
        if (bodyBuf != null) {
            bodyBuf.retain();
        }
        return this;
    }
    
    @Override
    public Packet retain(int increment) {
        if (bodyBuf != null) {
            bodyBuf.retain(increment);
        }
        return this;
    }
    
    @Override
    public Packet touch() {
        if (bodyBuf != null) {
            bodyBuf.touch();
        }
        return this;
    }
    
    @Override
    public Packet touch(Object hint) {
        if (bodyBuf != null) {
            bodyBuf.touch(hint);
        }
        return this;
    }
    
    @Override
    public boolean release() {
        return bodyBuf != null && bodyBuf.release();
    }
    
    @Override
    public boolean release(int decrement) {
        return bodyBuf != null && bodyBuf.release(decrement);
    }
    
    public void resetReaderIndex() {
        if (bodyBuf != null) {
            bodyBuf.resetReaderIndex();
        }
    }
}