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
    
    /**
     * Creates an empty cluster broadcast message.
     */
    public ClusterBroadcast() {
        super(Command.CLUSTER_BROADCAST);
    }
    
    /**
     * Creates a cluster broadcast message with the given data.
     *
     * @param sourceNodeId ID of the source cluster node
     * @param topicHash hash of the broadcast topic
     * @param originalCmd original command (PUSH or BROADCAST)
     * @param body message body
     */
    public ClusterBroadcast(String sourceNodeId, long topicHash, byte originalCmd, byte[] body) {
        super(Command.CLUSTER_BROADCAST);
        this.sourceNodeId = sourceNodeId;
        this.topicHash = topicHash;
        this.originalCmd = originalCmd;
        this.body = body != null ? body : new byte[0];
    }
    
    /**
     * Encodes this message to a byte buffer.
     *
     * @param out output buffer
     */
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
    
    /**
     * Decodes this message from a byte buffer.
     *
     * @param in input buffer
     * @throws IllegalArgumentException if the command byte does not match CLUSTER_BROADCAST
     */
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
    
    /**
     * Estimates the encoded size of this message in bytes.
     *
     * @return estimated size in bytes
     */
    @Override
    public int estimateSize() {
        return 1 + 1 + sourceNodeId.length() + 8 + 1 + 4 + body.length;
    }
    
    /**
     * Returns the source node ID.
     *
     * @return source node ID
     */
    public String getSourceNodeId() {
        return sourceNodeId;
    }
    
    /**
     * Returns the topic hash.
     *
     * @return topic hash
     */
    public long getTopicHash() {
        return topicHash;
    }
    
    /**
     * Returns the original command (PUSH or BROADCAST).
     *
     * @return original command byte
     */
    public byte getOriginalCmd() {
        return originalCmd;
    }
    
    /**
     * Returns the message body.
     *
     * @return message body bytes
     */
    public byte[] getBody() {
        return body;
    }
    
    /**
     * Returns the message body as a ByteBuf.
     *
     * @return body wrapped in a ByteBuf
     */
    public ByteBuf getBodyAsByteBuf() {
        return Unpooled.wrappedBuffer(body);
    }
    
    /**
     * Returns whether this message has a topic (topic hash non-zero).
     *
     * @return true if topic hash is non-zero
     */
    public boolean hasTopic() {
        return topicHash != 0;
    }
    
    /**
     * Returns whether the original command is PUSH.
     *
     * @return true if original command is PUSH
     */
    public boolean isPush() {
        return originalCmd == Command.PUSH;
    }
    
    /**
     * Returns whether the original command is BROADCAST.
     *
     * @return true if original command is BROADCAST
     */
    public boolean isBroadcast() {
        return originalCmd == Command.BROADCAST;
    }
    
    /**
     * Returns a string representation of this message.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "ClusterBroadcast{src='" + sourceNodeId + "', topicHash=" + topicHash + ", cmd=" + originalCmd + '}';
    }
}
