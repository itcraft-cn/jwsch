package cn.itcraft.jwsch.srv.cluster.message;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import cn.itcraft.jwsch.common.protocol.Command;

/**
 * Cluster join message sent by a node when connecting to another node.
 * 
 * <p>Format:
 * <pre>
 * | Cmd(1B) | NodeIdLen(1B) | NodeId(NB) | HostLen(1B) | Host(NB) | Port(4B) |
 * </pre>
 */
public final class ClusterJoin extends ClusterMessage {
    
    private String nodeId;
    private String host;
    private int port;
    
    public ClusterJoin() {
        super(Command.CLUSTER_JOIN);
    }
    
    public ClusterJoin(String nodeId, String host, int port) {
        super(Command.CLUSTER_JOIN);
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
    }
    
    @Override
    public void encode(ByteBuf out) {
        byte[] nodeIdBytes = nodeId.getBytes(StandardCharsets.UTF_8);
        byte[] hostBytes = host.getBytes(StandardCharsets.UTF_8);
        
        out.writeByte(cmd);
        out.writeByte(nodeIdBytes.length);
        out.writeBytes(nodeIdBytes);
        out.writeByte(hostBytes.length);
        out.writeBytes(hostBytes);
        out.writeInt(port);
    }
    
    @Override
    public void decode(ByteBuf in) {
        byte cmdByte = in.readByte();
        if (cmdByte != cmd) {
            throw new IllegalArgumentException("Invalid cmd: " + cmdByte);
        }
        
        int nodeIdLen = in.readUnsignedByte();
        nodeId = in.toString(in.readerIndex(), nodeIdLen, StandardCharsets.UTF_8);
        in.skipBytes(nodeIdLen);
        
        int hostLen = in.readUnsignedByte();
        host = in.toString(in.readerIndex(), hostLen, StandardCharsets.UTF_8);
        in.skipBytes(hostLen);
        
        port = in.readInt();
    }
    
    @Override
    public int estimateSize() {
        return 1 + 1 + (nodeId != null ? nodeId.length() : 0) 
             + 1 + (host != null ? host.length() : 0) + 4;
    }
    
    public String getNodeId() {
        return nodeId;
    }
    
    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }
    
    @Override
    public String toString() {
        return "ClusterJoin{nodeId='" + nodeId + "', host='" + host + "', port=" + port + '}';
    }
}
