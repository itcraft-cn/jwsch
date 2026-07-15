package cn.itcraft.jwsch.common.protocol;

/**
 * 协议常量定义。
 * 
 * <p>二进制协议格式：
 * <pre>
 * | Magic(2B) | HeaderLen(2B) | BodyLen(4B) | Cmd(1B) | ErrCode(2B) | 
 * | SrcId(8B) | TgtId(8B) | Topic(NB) | Body(NB) |
 * </pre>
 * 
 * <p>固定头部 27 字节，变长 Topic 和 Body。
 */
public final class ProtocolConsts {
    
    /**
     * 魔数，用于协议识别。
     * 
     * <p>值为 0xE734，在字节流中为 [0xE7, 0x34]。
     */
    public static final byte[] MAGIC = { (byte) 0xe7, (byte) 0x34 };
    
    /**
     * 固定头部长度。
     * 
     * <p>计算：Magic(2) + HeaderLen(2) + BodyLen(4) + Cmd(1) + ErrCode(2) + SrcId(8) + TgtId(8) = 27
     */
    public static final int FIXED_HEADER_LENGTH = 27;
    
    /** Topic 最大长度 */
    public static final int MAX_TOPIC_LENGTH = 256;
    
    /** Body 最大长度：10MB */
    public static final int MAX_BODY_LENGTH = 10 * 1024 * 1024;
    
    /** Body 默认最大长度 */
    public static final int DEFAULT_MAX_BODY_LENGTH = 99999;
    
    private ProtocolConsts() {
    }
}