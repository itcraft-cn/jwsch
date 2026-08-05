package cn.itcraft.jwsch.srv.cluster.message;

import io.netty.buffer.ByteBuf;

/**
 * Base class for all cluster messages.
 * 
 * <p>Cluster messages are used for inter-node communication in the mesh.
 * Each message type corresponds to a cluster command.
 * 
 * <p>All cluster messages must implement encode, decode, and estimateSize methods.
 */
public abstract class ClusterMessage {
    
    protected final byte cmd;
    
    /**
     * Constructs a cluster message with the specified command.
     *
     * @param cmd the command byte for this message
     */
    protected ClusterMessage(byte cmd) {
        this.cmd = cmd;
    }
    
    /**
     * Returns the command byte of this message.
     *
     * @return the command byte
     */
    public byte getCmd() {
        return cmd;
    }
    
    /**
     * Encodes this message into the given ByteBuf.
     *
     * <p>The implementation must write the message in the format defined by
     * the specific message type. The message should be written starting from
     * the current writer index.
     *
     * @param out the ByteBuf to write to
     * @throws IllegalArgumentException if the message is invalid
     */
    public abstract void encode(ByteBuf out);
    
    /**
     * Decodes this message from the given ByteBuf.
     *
     * <p>The implementation must read the message from the current reader index.
     * After decoding, the reader index will be advanced past the message.
     *
     * @param in the ByteBuf to read from
     * @throws IllegalArgumentException if the buffer does not contain a valid message
     */
    public abstract void decode(ByteBuf in);
    
    /**
     * Estimates the encoded size of this message in bytes.
     *
     * <p>This is used for buffer pre-allocation to avoid resizing.
     *
     * @return estimated size in bytes
     */
    public abstract int estimateSize();
}
