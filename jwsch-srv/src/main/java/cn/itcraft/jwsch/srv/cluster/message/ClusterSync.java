package cn.itcraft.jwsch.srv.cluster.message;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.itcraft.jwsch.common.protocol.Command;

/**
 * Cluster sync message for connection information synchronization.
 * 
 * <p>Used to synchronize connection and subscription state across cluster nodes.
 * When a client connects, subscribes, or disconnects, the node broadcasts
 * incremental sync messages to other nodes. Full sync messages are sent
 * when a node joins the cluster.
 * 
 * <p>Format:
 * <pre>
 * | Cmd(1B) | SyncType(1B) | OpCount(2B) | Op1 | Op2 | ... |
 * 
 * Op:
 * | OpType(1B) | ConnectionId(8B) | TopicHashCount(2B) | TopicHashes(8B each) |
 * </pre>
 * 
 * <p>SyncType:
 * <ul>
 *   <li>0x01 = FULL (full sync)</li>
 *   <li>0x02 = INCREMENTAL (incremental sync)</li>
 * </ul>
 * 
 * <p>OpType:
 * <ul>
 *   <li>0x01 = ADD_CONNECTION</li>
 *   <li>0x02 = REMOVE_CONNECTION</li>
 *   <li>0x03 = ADD_SUBSCRIPTION</li>
 *   <li>0x04 = REMOVE_SUBSCRIPTION</li>
 * </ul>
 */
public final class ClusterSync extends ClusterMessage {
    
    public static final byte SYNC_FULL = 0x01;
    public static final byte SYNC_INCREMENTAL = 0x02;
    
    public static final byte OP_ADD_CONNECTION = 0x01;
    public static final byte OP_REMOVE_CONNECTION = 0x02;
    public static final byte OP_ADD_SUBSCRIPTION = 0x03;
    public static final byte OP_REMOVE_SUBSCRIPTION = 0x04;
    
    private byte syncType;
    private List<SyncOp> operations;
    
    /**
     * Constructs an empty sync message for decoding.
     */
    public ClusterSync() {
        super(Command.CLUSTER_SYNC);
        this.operations = Collections.emptyList();
    }
    
    /**
     * Constructs a sync message with the given sync type and operations.
     *
     * @param syncType sync type (FULL or INCREMENTAL)
     * @param operations list of sync operations, may be null or empty
     */
    public ClusterSync(byte syncType, List<SyncOp> operations) {
        super(Command.CLUSTER_SYNC);
        this.syncType = syncType;
        this.operations = operations != null ? new ArrayList<>(operations) : Collections.emptyList();
    }
    
    /**
     * Encodes this sync message into the given ByteBuf.
     *
     * <p>Format: Cmd(1B) | SyncType(1B) | OpCount(2B) | Op1 | Op2 | ...
     *
     * @param out the ByteBuf to write to
     */
    @Override
    public void encode(ByteBuf out) {
        out.writeByte(cmd);
        out.writeByte(syncType);
        out.writeShort(operations.size());
        
        for (SyncOp op : operations) {
            op.encode(out);
        }
    }
    
    /**
     * Decodes this sync message from the given ByteBuf.
     *
     * <p>Format: Cmd(1B) | SyncType(1B) | OpCount(2B) | Op1 | Op2 | ...
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
        
        syncType = in.readByte();
        int opCount = in.readUnsignedShort();
        operations = new ArrayList<>(opCount);
        
        for (int i = 0; i < opCount; i++) {
            operations.add(SyncOp.decode(in));
        }
    }
    
    /**
     * Estimates the encoded size of this sync message.
     *
     * <p>Size includes: command(1B) + sync type(1B) + op count(2B) + sum of operation sizes.
     *
     * @return estimated size in bytes
     */
    @Override
    public int estimateSize() {
        int size = 3;
        for (SyncOp op : operations) {
            size += op.estimateSize();
        }
        return size;
    }
    
    /**
     * Returns the sync type (FULL or INCREMENTAL).
     *
     * @return sync type byte
     */
    public byte getSyncType() {
        return syncType;
    }
    
    /**
     * Returns the list of sync operations.
     *
     * @return unmodifiable list of sync operations
     */
    public List<SyncOp> getOperations() {
        return Collections.unmodifiableList(operations);
    }
    
    /**
     * Checks if this is a full sync message.
     *
     * @return true if sync type is FULL, false otherwise
     */
    public boolean isFullSync() {
        return syncType == SYNC_FULL;
    }
    
    /**
     * Returns a string representation of this sync message.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "ClusterSync{syncType=" + syncType + ", ops=" + operations.size() + '}';
    }
    
    /**
     * Sync operation for connection subscription changes.
     */
    public static final class SyncOp {
        
        private final byte opType;
        private final long connectionId;
        private final Set<Long> topicHashes;
        
        /**
         * Constructs a sync operation without topic hashes.
         *
         * @param opType operation type (ADD_CONNECTION, REMOVE_CONNECTION, etc.)
         * @param connectionId connection identifier
         */
        public SyncOp(byte opType, long connectionId) {
            this(opType, connectionId, Collections.emptySet());
        }
        
        /**
         * Constructs a sync operation with topic hashes.
         *
         * @param opType operation type (ADD_CONNECTION, REMOVE_CONNECTION, etc.)
         * @param connectionId connection identifier
         * @param topicHashes set of topic hashes, may be null or empty
         */
        public SyncOp(byte opType, long connectionId, Set<Long> topicHashes) {
            this.opType = opType;
            this.connectionId = connectionId;
            this.topicHashes = topicHashes != null ? new HashSet<>(topicHashes) : Collections.emptySet();
        }
        
        /**
         * Encodes this sync operation into the given ByteBuf.
         *
         * <p>Format: OpType(1B) | ConnectionId(8B) | TopicHashCount(2B) | TopicHashes(8B each)
         *
         * @param out the ByteBuf to write to
         */
        public void encode(ByteBuf out) {
            out.writeByte(opType);
            out.writeLong(connectionId);
            out.writeShort(topicHashes.size());
            for (Long topicHash : topicHashes) {
                out.writeLong(topicHash);
            }
        }
        
        /**
         * Decodes a sync operation from the given ByteBuf.
         *
         * <p>Format: OpType(1B) | ConnectionId(8B) | TopicHashCount(2B) | TopicHashes(8B each)
         *
         * @param in the ByteBuf to read from
         * @return decoded sync operation
         * @throws IllegalArgumentException if the buffer does not contain a valid operation
         */
        public static SyncOp decode(ByteBuf in) {
            byte opType = in.readByte();
            long connectionId = in.readLong();
            int hashCount = in.readUnsignedShort();
            
            Set<Long> topicHashes = new HashSet<>(hashCount);
            for (int i = 0; i < hashCount; i++) {
                topicHashes.add(in.readLong());
            }
            
            return new SyncOp(opType, connectionId, topicHashes);
        }
        
        /**
         * Estimates the encoded size of this sync operation.
         *
         * <p>Size includes: op type(1B) + connection id(8B) + topic hash count(2B) + topic hashes(8B each).
         *
         * @return estimated size in bytes
         */
        public int estimateSize() {
            return 1 + 8 + 2 + topicHashes.size() * 8;
        }
        
        /**
         * Returns the operation type.
         *
         * @return operation type byte
         */
        public byte getOpType() {
            return opType;
        }
        
        /**
         * Returns the connection identifier.
         *
         * @return connection ID
         */
        public long getConnectionId() {
            return connectionId;
        }
        
        /**
         * Returns the set of topic hashes.
         *
         * @return unmodifiable set of topic hashes
         */
        public Set<Long> getTopicHashes() {
            return Collections.unmodifiableSet(topicHashes);
        }
        
        /**
         * Returns a string representation of this sync operation.
         *
         * @return string representation
         */
        @Override
        public String toString() {
            return "SyncOp{opType=" + opType + ", connId=" + connectionId + ", topics=" + topicHashes.size() + '}';
        }
    }
}
