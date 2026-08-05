package cn.itcraft.jwsch.common.protocol;

import cn.itcraft.jwsch.common.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Packet header.
 * 
 * <p>Header field layout (byte offsets):
 * <pre>
 * | Magic(0-1) | HeaderLen(2-3) | BodyLen(4-7) | Cmd(8) | ErrCode(9-10) |
 * | SrcId(11-18) | TgtId(19-26) | Topic(27-N) |
 * </pre>
 * 
 * <p>Use Builder pattern to create:
 * <pre>
 * PacketHeader header = new PacketHeader.Builder()
 *     .command(Command.PUSH)
 *     .sourceId(123L)
 *     .topic("/topic/news")
 *     .build();
 * </pre>
 */
public final class PacketHeader {
    
    /** Header length (fixed 27 bytes + variable topic length) */
    private final short headerLength;
    /** Body length */
    private final int bodyLength;
    /** Command type, see {@link Command} */
    private final byte command;
    /** Error code, see {@link ErrorCode} */
    private final short errorCode;
    /** Source connection ID */
    private final long sourceId;
    /** Target connection ID */
    private final long targetId;
    /** Topic, can be null */
    private final String topic;
    /** Topic ASCII byte cache */
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
    
    /**
     * Returns the total header length in bytes.
     * 
     * <p>Includes fixed header (27 bytes) plus variable-length topic.
     */
    public short getHeaderLength() {
        return headerLength;
    }
    
    /**
     * Returns the body length in bytes.
     * 
     * <p>0 indicates no body.
     */
    public int getBodyLength() {
        return bodyLength;
    }
    
    /**
     * Returns the command byte.
     */
    public byte getCommand() {
        return command;
    }
    
    /**
     * Returns the error code.
     */
    public short getErrorCode() {
        return errorCode;
    }
    
    /**
     * Returns the source connection ID.
     */
    public long getSourceId() {
        return sourceId;
    }
    
    /**
     * Returns the target connection ID.
     */
    public long getTargetId() {
        return targetId;
    }
    
    /**
     * Returns the topic string.
     * 
     * @return topic string or null if no topic
     */
    public String getTopic() {
        return topic;
    }
    
    /**
     * Returns the topic bytes cached for efficient encoding.
     * 
     * <p>Used by {@link PacketWriter} to avoid repeated encoding.
     * Returns null if topic is null.
     */
    public byte[] getTopicBytes() {
        return topicBytes;
    }
    
    /**
     * Checks if this header indicates success (errorCode == SUCCESS).
     */
    public boolean isSuccess() {
        return errorCode == ErrorCode.SUCCESS.getCode();
    }
    
    /**
     * Builder for PacketHeader.
     * 
     * <p>Provides fluent API for constructing PacketHeader instances.
     * Validates command and topic length on build().
     */
    public static final class Builder {
        private byte command;
        private short errorCode;
        private long sourceId;
        private long targetId;
        private String topic;
        private int bodyLength;
        
        /**
         * Sets the command byte.
         */
        public Builder command(byte command) {
            this.command = command;
            return this;
        }
        
        /**
         * Sets the error code as short value.
         */
        public Builder errorCode(short errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        /**
         * Sets the error code using ErrorCode enum.
         */
        public Builder errorCode(ErrorCode errorCode) {
            this.errorCode = errorCode.getCode();
            return this;
        }
        
        /**
         * Sets the source connection ID.
         */
        public Builder sourceId(long sourceId) {
            this.sourceId = sourceId;
            return this;
        }
        
        /**
         * Sets the target connection ID.
         */
        public Builder targetId(long targetId) {
            this.targetId = targetId;
            return this;
        }
        
        /**
         * Sets the topic string.
         * 
         * @param topic topic string (ASCII only, max {@link ProtocolConsts#MAX_TOPIC_LENGTH} bytes)
         */
        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }
        
        /**
         * Sets the body length.
         * 
         * @param bodyLength body length in bytes (0 for no body)
         */
        public Builder bodyLength(int bodyLength) {
            this.bodyLength = bodyLength;
            return this;
        }
        
        /**
         * Builds the PacketHeader instance.
         * 
         * @throws IllegalArgumentException if command is invalid or topic length exceeds limit
         */
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