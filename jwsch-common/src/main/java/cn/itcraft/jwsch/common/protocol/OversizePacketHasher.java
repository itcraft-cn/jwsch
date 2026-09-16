package cn.itcraft.jwsch.common.protocol;

import io.netty.buffer.ByteBuf;

/**
 * 过大数据包关联 hash。
 *
 * <p>仅读取协议头内固定字段与包头后 8 字节，
 * 热路径无循环、无复制；任何持有同一包字节的线程（如独立日志线程）
 * 都能从同一 buffer 上推导出同一 hash，用于关联原丢弃日志。
 */
public final class OversizePacketHasher {

    /**
     * 哈希扰动种子。
     */
    private static final long SEED = 0x9E3779B97F4A7C15L;

    private OversizePacketHasher() {
    }

    /**
     * 对从 base 偏移开始的一段 jwsch 协议字节计算 hash。
     * 固定读取：headerLen/bodyLen/cmd/errorCode/sourceId/targetId/body 前 8 字节。
     */
    public static long hash(ByteBuf buf, int base) {
        long headerLength = buf.getShort(base + 2) & 0xFFFFL;
        long bodyLength = buf.getInt(base + 4) & 0xFFFFFFFFL;
        long command = buf.getByte(base + 8) & 0xFFL;
        long errorCode = buf.getShort(base + 9) & 0xFFFFL;
        long sourceId = buf.getLong(base + 11);
        long targetId = buf.getLong(base + 19);
        
        long mixed = SEED;
        mixed ^= headerLength;
        mixed = Long.rotateLeft(mixed, 7) ^ bodyLength;
        mixed = Long.rotateLeft(mixed, 13) ^ command;
        mixed = Long.rotateLeft(mixed, 17) ^ errorCode;
        mixed = Long.rotateLeft(mixed, 23) ^ sourceId;
        mixed = Long.rotateLeft(mixed, 29) ^ targetId;
        
        if (buf.readableBytes() >= base + 35) {
            mixed = Long.rotateLeft(mixed, 31) ^ buf.getLong(base + 27);
        }
        return mixed;
    }
}
