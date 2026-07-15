package cn.itcraft.jwsch.common.bytebuf;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;

public final class ByteBufConfig {
    
    private final boolean pooled;
    private final boolean direct;
    private final String leakDetection;
    
    private ByteBufConfig(Builder builder) {
        this.pooled = builder.pooled;
        this.direct = builder.direct;
        this.leakDetection = builder.leakDetection;
    }
    
    public boolean isPooled() {
        return pooled;
    }
    
    public boolean isDirect() {
        return direct;
    }
    
    public String getLeakDetection() {
        return leakDetection;
    }
    
    public static final class Builder {
        private boolean pooled = true;
        private boolean direct = true;
        private String leakDetection = "SIMPLE";
        
        public Builder pooled(boolean pooled) {
            this.pooled = pooled;
            return this;
        }
        
        public Builder direct(boolean direct) {
            this.direct = direct;
            return this;
        }
        
        public Builder leakDetection(String leakDetection) {
            this.leakDetection = leakDetection;
            return this;
        }
        
        public ByteBufConfig build() {
            return new ByteBufConfig(this);
        }
    }
}