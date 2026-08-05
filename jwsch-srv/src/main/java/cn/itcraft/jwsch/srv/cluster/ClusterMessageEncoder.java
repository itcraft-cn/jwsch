package cn.itcraft.jwsch.srv.cluster;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import cn.itcraft.jwsch.srv.cluster.message.ClusterBroadcast;
import cn.itcraft.jwsch.srv.cluster.message.ClusterForward;
import cn.itcraft.jwsch.srv.cluster.message.ClusterHeartbeat;
import cn.itcraft.jwsch.srv.cluster.message.ClusterJoin;
import cn.itcraft.jwsch.srv.cluster.message.ClusterMembership;
import cn.itcraft.jwsch.srv.cluster.message.ClusterMessage;
import cn.itcraft.jwsch.srv.cluster.message.ClusterSync;

/**
 * Encoder for cluster messages.
 * 
 * <p>Encodes ClusterMessage subclasses to ByteBuf for network transmission.
 * Uses zerocopy where possible - message classes handle their own encoding.
 * Implements Netty's MessageToByteEncoder for efficient memory management.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Buffer pre-allocation based on message size estimation</li>
 *   <li>Support for both direct and heap buffers</li>
 *   <li>Delegates actual encoding to message.encode() method</li>
 *   <li>Optimized for high-throughput cluster communication</li>
 * </ul>
 */
class ClusterMessageEncoder extends MessageToByteEncoder<ClusterMessage> {
    
    @Override
    protected void encode(ChannelHandlerContext ctx, ClusterMessage msg, ByteBuf out) {
        msg.encode(out);
    }
    
    @Override
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ClusterMessage msg, boolean preferDirect) {
        int size = msg.estimateSize();
        
        if (preferDirect) {
            return ctx.alloc().ioBuffer(size);
        } else {
            return ctx.alloc().heapBuffer(size);
        }
    }
}