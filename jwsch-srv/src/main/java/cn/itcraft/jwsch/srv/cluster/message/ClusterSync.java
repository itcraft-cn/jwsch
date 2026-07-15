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
    
    public ClusterSync() {
        super(Command.CLUSTER_SYNC);
        this.operations = Collections.emptyList();
    }
    
    public ClusterSync(byte syncType, List<SyncOp> operations) {
        super(Command.CLUSTER_SYNC);
        this.syncType = syncType;
        this.operations = operations != null ? new ArrayList<>(operations) : Collections.emptyList();
    }
    
    @Override
    public void encode(ByteBuf out) {
        out.writeByte(cmd);
        out.writeByte(syncType);
        out.writeShort(operations.size());
        
        for (SyncOp op : operations) {
            op.encode(out);
        }
    }
    
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
    
    @Override
    public int estimateSize() {
        int size = 3;
        for (SyncOp op : operations) {
            size += op.estimateSize();
        }
        return size;
    }
    
    public byte getSyncType() {
        return syncType;
    }
    
    public List<SyncOp> getOperations() {
        return Collections.unmodifiableList(operations);
    }
    
    public boolean isFullSync() {
        return syncType == SYNC_FULL;
    }
    
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
        
        public SyncOp(byte opType, long connectionId) {
            this(opType, connectionId, Collections.emptySet());
        }
        
        public SyncOp(byte opType, long connectionId, Set<Long> topicHashes) {
            this.opType = opType;
            this.connectionId = connectionId;
            this.topicHashes = topicHashes != null ? new HashSet<>(topicHashes) : Collections.emptySet();
        }
        
        public void encode(ByteBuf out) {
            out.writeByte(opType);
            out.writeLong(connectionId);
            out.writeShort(topicHashes.size());
            for (Long topicHash : topicHashes) {
                out.writeLong(topicHash);
            }
        }
        
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
        
        public int estimateSize() {
            return 1 + 8 + 2 + topicHashes.size() * 8;
        }
        
        public byte getOpType() {
            return opType;
        }
        
        public long getConnectionId() {
            return connectionId;
        }
        
        public Set<Long> getTopicHashes() {
            return Collections.unmodifiableSet(topicHashes);
        }
        
        @Override
        public String toString() {
            return "SyncOp{opType=" + opType + ", connId=" + connectionId + ", topics=" + topicHashes.size() + '}';
        }
    }
}
