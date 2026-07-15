package cn.itcraft.jwsch.srv.cluster.message;

import io.netty.buffer.ByteBuf;

/**
 * Base class for all cluster messages.
 * 
 * <p>Cluster messages are used for inter-node communication in the mesh.
 * Each message type corresponds to a cluster command.
 */
public abstract class ClusterMessage {
    
    protected final byte cmd;
    
    protected ClusterMessage(byte cmd) {
        this.cmd = cmd;
    }
    
    public byte getCmd() {
        return cmd;
    }
    
    public abstract void encode(ByteBuf out);
    
    public abstract void decode(ByteBuf in);
    
    public abstract int estimateSize();
}
