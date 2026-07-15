package cn.itcraft.jwsch.srv.cluster.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;

/**
 * Cluster broadcast message for spreading PUSH/BROADCAST to other nodes.
 * 
 * <p>Format:
 * <pre>
 * | Cmd(1B) | SrcNodeIdLen(1B) | SrcNodeId(NB) | TopicHash(8B) | OriginalCmd(1B) | BodyLen(4B) | Body(NB) |
 * </pre>
 */
public final class ClusterBroadcast extends ClusterMessage {
    
    private String sourceNodeId;
    private long topicHash;
    private byte originalCmd;
    private byte[] body;
    
    public ClusterBroadcast() {
        super(Command.CLUSTER_BROADCAST);
    }
    
    public ClusterBroadcast(String sourceNodeId, long topicHash, byte originalCmd, byte[] body) {
        super(Command.CLUSTER_BROADCAST);
        this.sourceNodeId = sourceNodeId;
        this.topicHash = topicHash;
        this.originalCmd = originalCmd;
        this.body = body != null ? body : new byte[0];
    }
    
    @Override
    public void encode(ByteBuf out) {
        byte[] nodeIdBytes = sourceNodeId.getBytes(StandardCharsets.UTF_8);
        
        out.writeByte(cmd);
        out.writeByte(nodeIdBytes.length);
        out.writeBytes(nodeIdBytes);
        out.writeLong(topicHash);
        out.writeByte(originalCmd);
        out.writeInt(body.length);
        if (body.length > 0) {
            out.writeBytes(body);
        }
    }
    
    @Override
    public void decode(ByteBuf in) {
        byte cmdByte = in.readByte();
        if (cmdByte != cmd) {
            throw new IllegalArgumentException("Invalid cmd: " + cmdByte);
        }
        
        int nodeIdLen = in.readUnsignedByte();
        sourceNodeId = in.toString(in.readerIndex(), nodeIdLen, StandardCharsets.UTF_8);
        in.skipBytes(nodeIdLen);
        
        topicHash = in.readLong();
        originalCmd = in.readByte();
        
        int bodyLen = in.readInt();
        if (bodyLen > 0) {
            body = new byte[bodyLen];
            in.readBytes(body);
        } else {
            body = new byte[0];
        }
    }
    
    @Override
    public int estimateSize() {
        return 1 + 1 + sourceNodeId.length() + 8 + 1 + 4 + body.length;
    }
    
    public String getSourceNodeId() {
        return sourceNodeId;
    }
    
    public long getTopicHash() {
        return topicHash;
    }
    
    public byte getOriginalCmd() {
        return originalCmd;
    }
    
    public byte[] getBody() {
        return body;
    }
    
    public ByteBuf getBodyAsByteBuf() {
        return Unpooled.wrappedBuffer(body);
    }
    
    public boolean hasTopic() {
        return topicHash != 0;
    }
    
    public boolean isPush() {
        return originalCmd == Command.PUSH;
    }
    
    public boolean isBroadcast() {
        return originalCmd == Command.BROADCAST;
    }
    
    @Override
    public String toString() {
        return "ClusterBroadcast{src='" + sourceNodeId + "', topicHash=" + topicHash + ", cmd=" + originalCmd + '}';
    }
}
