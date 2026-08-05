package cn.itcraft.jwsch.common.protocol;

/**
 * Protocol constant definitions for jwsch binary protocol.
 * 
 * <p>Binary protocol format:
 * <pre>
 * | Magic(2B) | HeaderLen(2B) | BodyLen(4B) | Cmd(1B) | ErrCode(2B) | 
 * | SrcId(8B) | TgtId(8B) | Topic(NB) | Body(NB) |
 * </pre>
 * 
 * <p>Fixed header is 27 bytes, variable-length Topic and Body.
 */
public final class ProtocolConsts {
    
    /**
     * Magic number for protocol identification.
     * 
     * <p>Value is 0xE734, represented as [0xE7, 0x34] in byte stream.
     */
    public static final byte[] MAGIC = { (byte) 0xe7, (byte) 0x34 };
    
    /**
     * Fixed header length in bytes.
     * 
     * <p>Calculation: Magic(2) + HeaderLen(2) + BodyLen(4) + Cmd(1) + 
     * ErrCode(2) + SrcId(8) + TgtId(8) = 27 bytes
     */
    public static final int FIXED_HEADER_LENGTH = 27;
    
    /**
     * Maximum topic length in characters.
     * 
     * <p>Topics are encoded in ASCII, limited to 256 characters.
     */
    public static final int MAX_TOPIC_LENGTH = 256;
    
    /**
     * Maximum body length: 10MB.
     * 
     * <p>Maximum allowed payload size for message body.
     */
    public static final int MAX_BODY_LENGTH = 10 * 1024 * 1024;
    
    /**
     * Default maximum body length.
     * 
     * <p>Used when no explicit limit is configured.
     */
    public static final int DEFAULT_MAX_BODY_LENGTH = 99999;
    
    private ProtocolConsts() {
    }
}