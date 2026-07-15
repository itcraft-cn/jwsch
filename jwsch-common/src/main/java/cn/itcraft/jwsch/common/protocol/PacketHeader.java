package cn.itcraft.jwsch.common.protocol;

import cn.itcraft.jwsch.common.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 数据包头部。
 * 
 * <p>头部字段布局（字节偏移）：
 * <pre>
 * | Magic(0-1) | HeaderLen(2-3) | BodyLen(4-7) | Cmd(8) | ErrCode(9-10) |
 * | SrcId(11-18) | TgtId(19-26) | Topic(27-N) |
 * </pre>
 * 
 * <p>使用 Builder 模式创建：
 * <pre>
 * PacketHeader header = new PacketHeader.Builder()
 *     .command(Command.PUSH)
 *     .sourceId(123L)
 *     .topic("/topic/news")
 *     .build();
 * </pre>
 */
public final class PacketHeader {
    
    /** 头部长度（固定27字节 + Topic变长） */
    private final short headerLength;
    /** 包体长度 */
    private final int bodyLength;
    /** 命令类型，参见 {@link Command} */
    private final byte command;
    /** 错误码，参见 {@link ErrorCode} */
    private final short errorCode;
    /** 源连接ID */
    private final long sourceId;
    /** 目标连接ID */
    private final long targetId;
    /** Topic，可为 null */
    private final String topic;
    /** Topic 的 ASCII 字节缓存 */
    private final byte[] topicBytes;
    
    private PacketHeader(Builder builder) {
        this.command = builder.command;
        this.errorCode = builder.errorCode;
        this.sourceId = builder.sourceId;
        this.targetId = builder.targetId;
        this.topic = builder.topic;
        this.bodyLength = builder.bodyLength;
        
        this.topicBytes = topic != null ? topic.getBytes(StandardCharsets.US_ASCII) : null;
        this.headerLength = (short) (ProtocolConsts.FIXED_HEADER_LENGTH + 
            (topicBytes != null ? topicBytes.length : 0));
    }
    
    public short getHeaderLength() {
        return headerLength;
    }
    
    public int getBodyLength() {
        return bodyLength;
    }
    
    public byte getCommand() {
        return command;
    }
    
    public short getErrorCode() {
        return errorCode;
    }
    
    public long getSourceId() {
        return sourceId;
    }
    
    public long getTargetId() {
        return targetId;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public byte[] getTopicBytes() {
        return topicBytes;
    }
    
    public boolean isSuccess() {
        return errorCode == ErrorCode.SUCCESS.getCode();
    }
    
    public static final class Builder {
        private byte command;
        private short errorCode;
        private long sourceId;
        private long targetId;
        private String topic;
        private int bodyLength;
        
        public Builder command(byte command) {
            this.command = command;
            return this;
        }
        
        public Builder errorCode(short errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        public Builder errorCode(ErrorCode errorCode) {
            this.errorCode = errorCode.getCode();
            return this;
        }
        
        public Builder sourceId(long sourceId) {
            this.sourceId = sourceId;
            return this;
        }
        
        public Builder targetId(long targetId) {
            this.targetId = targetId;
            return this;
        }
        
        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }
        
        public Builder bodyLength(int bodyLength) {
            this.bodyLength = bodyLength;
            return this;
        }
        
        public PacketHeader build() {
            if (!Command.isValid(command)) {
                throw new IllegalArgumentException("Invalid command: " + command);
            }
            
            if (topic != null) {
                int topicLen = topic.length();
                if (topicLen > ProtocolConsts.MAX_TOPIC_LENGTH) {
                    throw new IllegalArgumentException(
                        "Topic length exceeds max: " + ProtocolConsts.MAX_TOPIC_LENGTH);
                }
            }
            
            return new PacketHeader(this);
        }
    }
}