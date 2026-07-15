package cn.itcraft.jwsch.srv.cluster.message;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketHeader;
import cn.itcraft.jwsch.common.protocol.PacketWriter;
import cn.itcraft.jwsch.common.protocol.ProtocolConsts;

/**
 * Cluster forward message for forwarding REQUEST to target node.
 * 
 * <p>Format:
 * <pre>
 * | Cmd(1B) | TargetId(8B) | PacketLen(4B) | Packet(NB) |
 * </pre>
 */
public final class ClusterForward extends ClusterMessage {
    
    private long targetId;
    private Packet packet;
    
    public ClusterForward() {
        super(Command.CLUSTER_FORWARD);
    }
    
    public ClusterForward(long targetId, Packet packet) {
        super(Command.CLUSTER_FORWARD);
        this.targetId = targetId;
        this.packet = packet;
    }
    
    @Override
    public void encode(ByteBuf out) {
        out.writeByte(cmd);
        out.writeLong(targetId);
        
        if (packet != null) {
            ByteBuf packetBuf = PacketWriter.write(packet, out.alloc());
            try {
                out.writeInt(packetBuf.readableBytes());
                out.writeBytes(packetBuf);
            } finally {
                packetBuf.release();
            }
        } else {
            out.writeInt(0);
        }
    }
    
    @Override
    public void decode(ByteBuf in) {
        byte cmdByte = in.readByte();
        if (cmdByte != cmd) {
            throw new IllegalArgumentException("Invalid cmd: " + cmdByte);
        }
        
        targetId = in.readLong();
        
        int packetLen = in.readInt();
        if (packetLen > 0) {
            ByteBuf packetBuf = in.readSlice(packetLen);
            packet = decodePacket(packetBuf);
        }
    }
    
    private Packet decodePacket(ByteBuf in) {
        if (in.readableBytes() < ProtocolConsts.FIXED_HEADER_LENGTH) {
            throw new IllegalArgumentException("Packet too short");
        }
        
        in.skipBytes(2);
        
        short headerLength = in.readShort();
        int bodyLength = in.readInt();
        byte command = in.readByte();
        short errorCode = in.readShort();
        long sourceId = in.readLong();
        long targetIdVal = in.readLong();
        
        int topicLength = headerLength - ProtocolConsts.FIXED_HEADER_LENGTH;
        
        String topic = null;
        if (topicLength > 0) {
            byte[] topicBytes = new byte[topicLength];
            in.readBytes(topicBytes);
            topic = new String(topicBytes, StandardCharsets.US_ASCII);
        }
        
        ByteBuf bodyBuf = null;
        if (bodyLength > 0) {
            bodyBuf = in.readSlice(bodyLength).retain();
        }
        
        PacketHeader header = new PacketHeader.Builder()
            .command(command)
            .errorCode(errorCode)
            .sourceId(sourceId)
            .targetId(targetIdVal)
            .topic(topic)
            .bodyLength(bodyLength)
            .build();
        
        return new Packet(header, bodyBuf);
    }
    
    @Override
    public int estimateSize() {
        if (packet != null) {
            PacketHeader header = packet.getHeader();
            int bodyLength = packet.getBodyBuf() != null ? packet.getBodyBuf().readableBytes() : 0;
            return 1 + 8 + 4 + header.getHeaderLength() + bodyLength;
        }
        return 1 + 8 + 4;
    }
    
    public long getTargetId() {
        return targetId;
    }
    
    public Packet getPacket() {
        return packet;
    }
    
    @Override
    public String toString() {
        return "ClusterForward{targetId=" + targetId + '}';
    }
}