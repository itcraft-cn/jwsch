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

    /**
     * 数据包总长度（Header+Body）软上限默认值（200KB）。
     *
     * <p>可通过 TCP 配置调整，但不得超过 {@link #MAX_PACKET_LENGTH_LIMIT} 硬上限。
     * 超过软上限的数据包在收发两端直接丢弃，不关闭连接。
     */
    public static final int DEFAULT_MAX_PACKET_LENGTH = 200 * 1024;

    /**
     * 数据包硬上限（硬编码 500KB）。
     *
     * <p>任何配置值超过此上限时将被强制钳制为该值，不可放宽。
     */
    public static final int MAX_PACKET_LENGTH_LIMIT = 500 * 1024;

    private ProtocolConsts() {
    }
}