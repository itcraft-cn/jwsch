package cn.itcraft.jwsch.common.bytebuf;

/**
 * ByteBuf configuration for Netty memory management.
 *
 * <p>Controls buffer allocation strategy:
 * <ul>
 *   <li>Pooled vs unpooled allocation</li>
 *   <li>Direct (off-heap) vs heap buffers</li>
 *   <li>Memory leak detection level</li>
 * </ul>
 *
 * <p>Default configuration:
 * <ul>
 *   <li>pooled: true (PooledByteBufAllocator)</li>
 *   <li>direct: true (direct buffers)</li>
 *   <li>leakDetection: "SIMPLE"</li>
 * </ul>
 *
 * <p>Leak detection levels (Netty system property):
 * <ul>
 *   <li>DISABLED: No leak detection</li>
 *   <li>SIMPLE: Simple sampling (default)</li>
 *   <li>ADVANCED: Advanced sampling with stack traces</li>
 *   <li>PARANOID: Full tracking for all allocations</li>
 * </ul>
 */
public final class ByteBufConfig {
    
    private final boolean pooled;
    private final boolean direct;
    private final String leakDetection;
    
    private ByteBufConfig(Builder builder) {
        this.pooled = builder.pooled;
        this.direct = builder.direct;
        this.leakDetection = builder.leakDetection;
    }
    
    /**
     * Returns whether buffers are pooled.
     */
    public boolean isPooled() {
        return pooled;
    }
    
    /**
     * Returns whether buffers are direct (off-heap).
     */
    public boolean isDirect() {
        return direct;
    }
    
    /**
     * Returns the leak detection level.
     *
     * @return leak detection level string
     */
    public String getLeakDetection() {
        return leakDetection;
    }
    
    /**
     * Builder for ByteBufConfig.
     */
    public static final class Builder {
        private boolean pooled = true;
        private boolean direct = true;
        private String leakDetection = "SIMPLE";
        
        /**
         * Sets whether to use pooled allocation.
         *
         * <p>Pooled allocation reuses buffers for better performance
         * but adds complexity.
         */
        public Builder pooled(boolean pooled) {
            this.pooled = pooled;
            return this;
        }
        
        /**
         * Sets whether to use direct (off-heap) buffers.
         *
         * <p>Direct buffers enable zero-copy I/O but require
         * explicit memory management.
         */
        public Builder direct(boolean direct) {
            this.direct = direct;
            return this;
        }
        
        /**
         * Sets the leak detection level.
         *
         * <p>Valid values: "DISABLED", "SIMPLE", "ADVANCED", "PARANOID"
         *
         * @param leakDetection leak detection level
         */
        public Builder leakDetection(String leakDetection) {
            this.leakDetection = leakDetection;
            return this;
        }
        
        /**
         * Builds the ByteBufConfig.
         */
        public ByteBufConfig build() {
            return new ByteBufConfig(this);
        }
    }
}