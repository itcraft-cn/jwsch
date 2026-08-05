package cn.itcraft.jwsch.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;

import java.util.Objects;

/**
 * Binary protocol packet.
 * 
 * <p>Implements {@link ReferenceCounted} interface for reference counting management.
 * When a Packet needs to be sent to multiple clients, call {@link #retain()} to increase reference count,
 * then call {@link #release()} to release resources after sending completes.
 * 
 * <p>Usage example:
 * <pre>
 * Packet packet = new Packet(header, bodyBuf);
 * packet.retain();  // retain before sending to multiple clients
 * router.broadcastToTopic(topic, packet);
 * packet.release(); // release after broadcast completes
 * </pre>
 */
public final class Packet implements ReferenceCounted {
    
    private final PacketHeader header;
    private final ByteBuf bodyBuf;
    
    /**
     * Creates a new Packet with header and body buffer.
     * 
     * @param header   packet header (must not be null)
     * @param bodyBuf  packet body buffer (can be null for empty body)
     */
    public Packet(PacketHeader header, ByteBuf bodyBuf) {
        this.header = Objects.requireNonNull(header, "header cannot be null");
        this.bodyBuf = bodyBuf;
    }
    
    /**
     * Returns the packet header.
     */
    public PacketHeader getHeader() {
        return header;
    }
    
    /**
     * Returns the packet body buffer.
     * 
     * <p>Note: The returned ByteBuf retains its reader/writer indices.
     * Multiple calls to this method return the same ByteBuf instance.
     */
    public ByteBuf getBodyBuf() {
        return bodyBuf;
    }
    
    /**
     * Returns the command byte from packet header.
     */
    public byte getCommand() {
        return header.getCommand();
    }
    
    /**
     * Returns the error code from packet header.
     */
    public short getErrorCode() {
        return header.getErrorCode();
    }
    
    /**
     * Returns the source ID from packet header.
     */
    public long getSourceId() {
        return header.getSourceId();
    }
    
    /**
     * Returns the target ID from packet header.
     */
    public long getTargetId() {
        return header.getTargetId();
    }
    
    /**
     * Returns the topic string from packet header.
     */
    public String getTopic() {
        return header.getTopic();
    }
    
    /**
     * Checks if this packet has a non-empty body.
     * 
     * @return true if bodyBuf exists and has readable bytes
     */
    public boolean hasBody() {
        return bodyBuf != null && bodyBuf.readableBytes() > 0;
    }
    
    /**
     * Returns the reference count of the underlying body buffer.
     * 
     * <p>Returns 0 if bodyBuf is null.
     */
    @Override
    public int refCnt() {
        return bodyBuf != null ? bodyBuf.refCnt() : 0;
    }
    
    /**
     * Increases the reference count by 1.
     * 
     * <p>Called before sending the same Packet to multiple clients.
     * Each client should call {@link #release()} after processing.
     */
    @Override
    public Packet retain() {
        if (bodyBuf != null) {
            bodyBuf.retain();
        }
        return this;
    }
    
    /**
     * Increases the reference count by the specified increment.
     */
    @Override
    public Packet retain(int increment) {
        if (bodyBuf != null) {
            bodyBuf.retain(increment);
        }
        return this;
    }
    
    /**
     * Records the current access location of this object for debugging purposes.
     * 
     * <p>If leak detection is enabled, this helps identify the location
     * where the ByteBuf was accessed before a leak is reported.
     */
    @Override
    public Packet touch() {
        if (bodyBuf != null) {
            bodyBuf.touch();
        }
        return this;
    }
    
    /**
     * Records the current access location of this object with additional hint.
     */
    @Override
    public Packet touch(Object hint) {
        if (bodyBuf != null) {
            bodyBuf.touch(hint);
        }
        return this;
    }
    
    /**
     * Decreases the reference count by 1.
     * 
     * <p>Returns true if the reference count became 0 and the buffer has been released.
     * Returns false if bodyBuf is null.
     */
    @Override
    public boolean release() {
        return bodyBuf != null && bodyBuf.release();
    }
    
    /**
     * Decreases the reference count by the specified decrement.
     */
    @Override
    public boolean release(int decrement) {
        return bodyBuf != null && bodyBuf.release(decrement);
    }
    
    /**
     * Resets the reader index of the body buffer to its marked position.
     * 
     * <p>Useful when the same body needs to be read multiple times
     * (e.g., when broadcasting to multiple subscribers).
     */
    public void resetReaderIndex() {
        if (bodyBuf != null) {
            bodyBuf.resetReaderIndex();
        }
    }
}