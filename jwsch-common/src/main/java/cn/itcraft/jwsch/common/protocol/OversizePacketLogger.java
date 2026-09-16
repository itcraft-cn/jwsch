package cn.itcraft.jwsch.common.protocol;

import io.netty.buffer.ByteBuf;

/**
 * 过大数据包丢弃内容的记录器。
 *
 * <p>热路径只做两件事：打印原日志（大小/限制/hash）并调用实际实现。
 * 具体是否提取并打印包内容由启动时注入的实现决定：
 * <ul>
 *   <li>{@link NoopOversizePacketLogger}：默认实现，不打印包内容</li>
 *   <li>{@link QueuedOversizePacketLogger}：将包片段投递到有界队列，
 *       由独立日志线程拆解并打印前 200 字节</li>
 * </ul>
 *
 * <p>两个实现打印同一 hash，用于独立日志线程与原丢弃日志的关联。
 */
public interface OversizePacketLogger {

    /**
     * 处理一次过大数据包丢弃事件。
     *
     * @param packetLength 被丢弃数据包总长度（字节）
     * @param limit        当前生效的软上限（字节）
     * @param hash         包关联 hash（原日志与超限日志一致）
     * @param packetBytes  包原始字节（可读副本，调用方负责 retain/release 生命周期；可为 null）
     */
    void onDropped(int packetLength, int limit, long hash, ByteBuf packetBytes);
}
