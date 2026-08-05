package cn.itcraft.jwsch.srv.cluster.message;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import cn.itcraft.jwsch.common.protocol.Command;

/**
 * Cluster join message sent by a node when connecting to another node.
 * 
 * <p>When a node starts up or discovers another node, it sends this message
 * to announce its presence and provide connection information.
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
    
    /**
     * Constructs an empty join message for decoding.
     */
    public ClusterJoin() {
        super(Command.CLUSTER_JOIN);
    }
    
    /**
     * Constructs a join message with node information.
     *
     * @param nodeId the node identifier
     * @param host the hostname or IP address
     * @param port the cluster port
     * @throws NullPointerException if nodeId or host is null
     */
    public ClusterJoin(String nodeId, String host, int port) {
        super(Command.CLUSTER_JOIN);
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
    }
    
    /**
     * Encodes this join message into the given ByteBuf.
     *
     * <p>Format: Cmd(1B) | NodeIdLen(1B) | NodeId(NB) | HostLen(1B) | Host(NB) | Port(4B)
     *
     * @param out the ByteBuf to write to
     */
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
    
    /**
     * Decodes this join message from the given ByteBuf.
     *
     * <p>Format: Cmd(1B) | NodeIdLen(1B) | NodeId(NB) | HostLen(1B) | Host(NB) | Port(4B)
     *
     * @param in the ByteBuf to read from
     * @throws IllegalArgumentException if the buffer does not contain a valid message
     */
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
    
    /**
     * Estimates the encoded size of this join message.
     *
     * <p>Size includes: command(1B) + nodeId length(1B) + nodeId bytes + host length(1B) + host bytes + port(4B).
     *
     * @return estimated size in bytes
     */
    @Override
    public int estimateSize() {
        return 1 + 1 + (nodeId != null ? nodeId.length() : 0) 
             + 1 + (host != null ? host.length() : 0) + 4;
    }
    
    /**
     * Returns the node identifier.
     *
     * @return node identifier, may be null if not set
     */
    public String getNodeId() {
        return nodeId;
    }
    
    /**
     * Returns the hostname or IP address.
     *
     * @return host, may be null if not set
     */
    public String getHost() {
        return host;
    }
    
    /**
     * Returns the cluster port.
     *
     * @return cluster port
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Returns a string representation of this join message.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "ClusterJoin{nodeId='" + nodeId + "', host='" + host + "', port=" + port + '}';
    }
}
