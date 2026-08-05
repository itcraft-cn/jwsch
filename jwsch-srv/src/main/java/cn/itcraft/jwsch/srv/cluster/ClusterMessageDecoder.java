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
 * Implements Netty's ByteToMessageDecoder for streaming protocol decoding.
 * 
 * <p>Decoding process:
 * <ol>
 *   <li>Check if sufficient bytes are available (minimum 1 byte for command)</li>
 *   <li>Read command byte without advancing reader index</li>
 *   <li>Create appropriate ClusterMessage instance based on command</li>
 *   <li>Delegate decoding to the message's decode() method</li>
 *   <li>Add decoded message to output list</li>
 * </ol>
 * 
 * <p>Supports all cluster message types defined in {@link cn.itcraft.jwsch.common.protocol.Command}.
 */
class ClusterMessageDecoder extends ByteToMessageDecoder {
    
    /**
     * Decodes bytes from the input buffer into cluster messages.
     * 
     * <p>Uses a stateful decoding approach with mark/reset for partial reads.
     * If insufficient bytes are available, returns without adding to output.
     * If an unknown command is received, resets reader index and returns.
     * 
     * @param ctx the Netty channel handler context
     * @param in the input byte buffer
     * @param out the list to add decoded messages to
     */
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
    
    /**
     * Creates a ClusterMessage instance based on the command byte.
     * 
     * <p>Maps command bytes from {@link cn.itcraft.jwsch.common.protocol.Command}
     * to corresponding ClusterMessage subclasses.
     * 
     * @param cmd the command byte
     * @return the appropriate ClusterMessage instance, or null for unknown commands
     */
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