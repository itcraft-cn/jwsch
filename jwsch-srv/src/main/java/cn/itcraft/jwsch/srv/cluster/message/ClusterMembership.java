package cn.itcraft.jwsch.srv.cluster.message;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.srv.cluster.NodeInfo;
import cn.itcraft.jwsch.srv.cluster.NodeStatus;

/**
 * Cluster membership message containing list of known nodes.
 * 
 * <p>Format:
 * <pre>
 * | Cmd(1B) | NodeCount(2B) | NodeInfo1 | NodeInfo2 | ... |
 * 
 * NodeInfo:
 * | NodeIdLen(1B) | NodeId(NB) | HostLen(1B) | Host(NB) | Port(4B) | WsPort(4B) | HttpPort(4B) |
 * </pre>
 */
public final class ClusterMembership extends ClusterMessage {
    
    private List<NodeInfo> nodes;
    
    public ClusterMembership() {
        super(Command.CLUSTER_MEMBERSHIP);
        this.nodes = Collections.emptyList();
    }
    
    public ClusterMembership(List<NodeInfo> nodes) {
        super(Command.CLUSTER_MEMBERSHIP);
        this.nodes = nodes != null ? new ArrayList<>(nodes) : Collections.emptyList();
    }
    
    @Override
    public void encode(ByteBuf out) {
        out.writeByte(cmd);
        out.writeShort(nodes.size());
        
        for (NodeInfo node : nodes) {
            encodeNodeInfo(out, node);
        }
    }
    
    @Override
    public void decode(ByteBuf in) {
        byte cmdByte = in.readByte();
        if (cmdByte != cmd) {
            throw new IllegalArgumentException("Invalid cmd: " + cmdByte);
        }
        
        int nodeCount = in.readUnsignedShort();
        nodes = new ArrayList<>(nodeCount);
        
        for (int i = 0; i < nodeCount; i++) {
            nodes.add(decodeNodeInfo(in));
        }
    }
    
    @Override
    public int estimateSize() {
        int size = 1 + 2;
        for (NodeInfo node : nodes) {
            size += 1 + node.getNodeId().length() + 1 + node.getHost().length() + 12;
        }
        return size;
    }
    
    private void encodeNodeInfo(ByteBuf out, NodeInfo node) {
        byte[] nodeIdBytes = node.getNodeId().getBytes(StandardCharsets.UTF_8);
        byte[] hostBytes = node.getHost().getBytes(StandardCharsets.UTF_8);
        
        out.writeByte(nodeIdBytes.length);
        out.writeBytes(nodeIdBytes);
        out.writeByte(hostBytes.length);
        out.writeBytes(hostBytes);
        out.writeInt(node.getClusterPort());
        out.writeInt(node.getWebsocketPort());
        out.writeInt(node.getHttpPort());
    }
    
    private NodeInfo decodeNodeInfo(ByteBuf in) {
        int nodeIdLen = in.readUnsignedByte();
        String nodeId = in.toString(in.readerIndex(), nodeIdLen, StandardCharsets.UTF_8);
        in.skipBytes(nodeIdLen);
        
        int hostLen = in.readUnsignedByte();
        String host = in.toString(in.readerIndex(), hostLen, StandardCharsets.UTF_8);
        in.skipBytes(hostLen);
        
        int port = in.readInt();
        int wsPort = in.readInt();
        int httpPort = in.readInt();
        
        return new NodeInfo(nodeId, host, port, wsPort, httpPort);
    }
    
    public List<NodeInfo> getNodes() {
        return Collections.unmodifiableList(nodes);
    }
    
    @Override
    public String toString() {
        return "ClusterMembership{nodes=" + nodes.size() + '}';
    }
}
