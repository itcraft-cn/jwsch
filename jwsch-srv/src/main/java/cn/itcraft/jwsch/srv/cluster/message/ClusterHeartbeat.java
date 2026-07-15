package cn.itcraft.jwsch.srv.cluster.message;

import io.netty.buffer.ByteBuf;

import cn.itcraft.jwsch.common.protocol.Command;

/**
 * Cluster heartbeat message for node health check.
 * 
 * <p>Format:
 * <pre>
 * | Cmd(1B) |
 * </pre>
 * 
 * <p>Empty message body - just the command byte.
 */
public final class ClusterHeartbeat extends ClusterMessage {
    
    public ClusterHeartbeat() {
        super(Command.CLUSTER_HEARTBEAT);
    }
    
    @Override
    public void encode(ByteBuf out) {
        out.writeByte(cmd);
    }
    
    @Override
    public void decode(ByteBuf in) {
        byte cmdByte = in.readByte();
        if (cmdByte != cmd) {
            throw new IllegalArgumentException("Invalid cmd: " + cmdByte);
        }
    }
    
    @Override
    public int estimateSize() {
        return 1;
    }
    
    @Override
    public String toString() {
        return "ClusterHeartbeat{}";
    }
}
