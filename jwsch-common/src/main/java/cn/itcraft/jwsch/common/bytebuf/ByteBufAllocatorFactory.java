package cn.itcraft.jwsch.common.bytebuf;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.util.Objects;

public final class ByteBufAllocatorFactory {
    
    private final ByteBufConfig config;
    
    public ByteBufAllocatorFactory(ByteBufConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
    }
    
    public ByteBufAllocator create() {
        if (config.isPooled()) {
            return new PooledByteBufAllocator(config.isDirect());
        } else {
            return new UnpooledByteBufAllocator(config.isDirect());
        }
    }
}