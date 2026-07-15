package cn.itcraft.jwsch.srv.cluster;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.srv.cluster.message.ClusterBroadcast;
import cn.itcraft.jwsch.srv.cluster.message.ClusterForward;
import cn.itcraft.jwsch.srv.cluster.message.ClusterHeartbeat;
import cn.itcraft.jwsch.srv.cluster.message.ClusterJoin;
import cn.itcraft.jwsch.srv.cluster.message.ClusterMembership;
import cn.itcraft.jwsch.srv.cluster.message.ClusterMessage;
import cn.itcraft.jwsch.srv.cluster.message.ClusterSync;

/**
 * Decoder for cluster messages.
 * 
 * <p>Decodes ByteBuf to appropriate ClusterMessage subclass based on command byte.
 */
class ClusterMessageDecoder extends ByteToMessageDecoder {
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 1) {
            return;
        }
        
        in.markReaderIndex();
        
        byte cmd = in.getByte(in.readerIndex());
        
        ClusterMessage message = createMessage(cmd);
        if (message == null) {
            in.resetReaderIndex();
            return;
        }
        
        try {
            message.decode(in);
            out.add(message);
        } catch (Exception e) {
            in.resetReaderIndex();
            throw e;
        }
    }
    
    private ClusterMessage createMessage(byte cmd) {
        switch (cmd) {
            case Command.CLUSTER_JOIN:
                return new ClusterJoin();
            case Command.CLUSTER_MEMBERSHIP:
                return new ClusterMembership();
            case Command.CLUSTER_SYNC:
                return new ClusterSync();
            case Command.CLUSTER_FORWARD:
                return new ClusterForward();
            case Command.CLUSTER_BROADCAST:
                return new ClusterBroadcast();
            case Command.CLUSTER_HEARTBEAT:
                return new ClusterHeartbeat();
            default:
                return null;
        }
    }
}