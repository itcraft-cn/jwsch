package cn.itcraft.jwsch.common.protocol;

import io.netty.buffer.ByteBuf;

/**
 * 空实现：不在热路径做任何额外动作（默认配置）。
 */
final class NoopOversizePacketLogger implements OversizePacketLogger {

    static final NoopOversizePacketLogger INSTANCE = new NoopOversizePacketLogger();

    private NoopOversizePacketLogger() {
    }

    @Override
    public void onDropped(int packetLength, int limit, long hash, ByteBuf packetBytes) {
        if (packetBytes != null) {
            packetBytes.release();
        }
    }
}
