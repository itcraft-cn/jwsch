package cn.itcraft.jwsch.srv.cluster.message;

import io.netty.buffer.ByteBuf;

import cn.itcraft.jwsch.common.protocol.Command;

/**
 * Cluster heartbeat message for node health check.
 * 
 * <p>Sent periodically between cluster nodes to verify connectivity and liveness.
 * If a node fails to respond to heartbeats, it is considered dead and removed
 * from the cluster membership.
 * 
 * <p>Format:
 * <pre>
 * | Cmd(1B) |
 * </pre>
 * 
 * <p>Empty message body - just the command byte.
 */
public final class ClusterHeartbeat extends ClusterMessage {
    
    /**
     * Constructs a heartbeat message.
     */
    public ClusterHeartbeat() {
        super(Command.CLUSTER_HEARTBEAT);
    }
    
    /**
     * Encodes this heartbeat message into the given ByteBuf.
     *
     * <p>Format: Cmd(1B)
     *
     * @param out the ByteBuf to write to
     */
    @Override
    public void encode(ByteBuf out) {
        out.writeByte(cmd);
    }
    
    /**
     * Decodes this heartbeat message from the given ByteBuf.
     *
     * <p>Format: Cmd(1B)
     *
     * @param in the ByteBuf to read from
     * @throws IllegalArgumentException if the command byte does not match
     */
    @Override
    public void decode(ByteBuf in) {
        byte cmdByte = in.readByte();
        if (cmdByte != cmd) {
            throw new IllegalArgumentException("Invalid cmd: " + cmdByte);
        }
    }
    
    /**
     * Estimates the encoded size of this heartbeat message.
     *
     * <p>Size is always 1 byte (command).
     *
     * @return estimated size in bytes (always 1)
     */
    @Override
    public int estimateSize() {
        return 1;
    }
    
    /**
     * Returns a string representation of this heartbeat message.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "ClusterHeartbeat{}";
    }
}
