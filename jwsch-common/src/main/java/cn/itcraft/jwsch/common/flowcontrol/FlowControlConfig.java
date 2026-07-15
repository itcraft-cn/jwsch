package cn.itcraft.jwsch.common.flowcontrol;

/**
 * 流量控制配置。
 *
 * <p>使用Builder模式构建不可变配置：
 * <pre>
 * FlowControlConfig config = FlowControlConfig.builder()
 *     .inboundEnabled(true)
 *     .maxTokensPerSecond(10000)
 *     .build();
 * </pre>
 */
public final class FlowControlConfig {
    
    private final boolean inboundEnabled;
    private final int maxTokensPerSecond;
    private final int burstSize;
    private final OverflowStrategy inboundOverflowStrategy;
    
    private final boolean globalBackpressureEnabled;
    private final double globalTriggerThreshold;
    private final double globalReleaseThreshold;
    private final long globalReleaseCooldownMs;
    
    private final boolean topicBackpressureEnabled;
    private final double topicTriggerThreshold;
    private final double topicReleaseThreshold;
    
    private final boolean outboundEnabled;
    private final int maxQueueSize;
    private final int disconnectThreshold;
    private final OverflowStrategy outboundOverflowStrategy;
    
    private FlowControlConfig(Builder builder) {
        this.inboundEnabled = builder.inboundEnabled;
        this.maxTokensPerSecond = builder.maxTokensPerSecond;
        this.burstSize = builder.burstSize;
        this.inboundOverflowStrategy = builder.inboundOverflowStrategy;
        
        this.globalBackpressureEnabled = builder.globalBackpressureEnabled;
        this.globalTriggerThreshold = builder.globalTriggerThreshold;
        this.globalReleaseThreshold = builder.globalReleaseThreshold;
        this.globalReleaseCooldownMs = builder.globalReleaseCooldownMs;
        
        this.topicBackpressureEnabled = builder.topicBackpressureEnabled;
        this.topicTriggerThreshold = builder.topicTriggerThreshold;
        this.topicReleaseThreshold = builder.topicReleaseThreshold;
        
        this.outboundEnabled = builder.outboundEnabled;
        this.maxQueueSize = builder.maxQueueSize;
        this.disconnectThreshold = builder.disconnectThreshold;
        this.outboundOverflowStrategy = builder.outboundOverflowStrategy;
    }
    
    public boolean isInboundEnabled() { return inboundEnabled; }
    public int getMaxTokensPerSecond() { return maxTokensPerSecond; }
    public int getBurstSize() { return burstSize; }
    public OverflowStrategy getInboundOverflowStrategy() { return inboundOverflowStrategy; }
    
    public boolean isGlobalBackpressureEnabled() { return globalBackpressureEnabled; }
    public double getGlobalTriggerThreshold() { return globalTriggerThreshold; }
    public double getGlobalReleaseThreshold() { return globalReleaseThreshold; }
    public long getGlobalReleaseCooldownMs() { return globalReleaseCooldownMs; }
    
    public boolean isTopicBackpressureEnabled() { return topicBackpressureEnabled; }
    public double getTopicTriggerThreshold() { return topicTriggerThreshold; }
    public double getTopicReleaseThreshold() { return topicReleaseThreshold; }
    
    public boolean isOutboundEnabled() { return outboundEnabled; }
    public int getMaxQueueSize() { return maxQueueSize; }
    public int getDisconnectThreshold() { return disconnectThreshold; }
    public OverflowStrategy getOutboundOverflowStrategy() { return outboundOverflowStrategy; }
    
    public static Builder builder() { return new Builder(); }
    
    public static final class Builder {
        private boolean inboundEnabled = true;
        private int maxTokensPerSecond = 10000;
        private int burstSize = 12000;
        private OverflowStrategy inboundOverflowStrategy = OverflowStrategy.DROP_NEWEST;
        
        private boolean globalBackpressureEnabled = true;
        private double globalTriggerThreshold = 0.2;
        private double globalReleaseThreshold = 0.05;
        private long globalReleaseCooldownMs = 500;
        
        private boolean topicBackpressureEnabled = true;
        private double topicTriggerThreshold = 0.3;
        private double topicReleaseThreshold = 0.1;
        
        private boolean outboundEnabled = true;
        private int maxQueueSize = 1024;
        private int disconnectThreshold = 4096;
        private OverflowStrategy outboundOverflowStrategy = OverflowStrategy.DROP_OLDEST_THEN_DISCONNECT;
        
        public Builder inboundEnabled(boolean inboundEnabled) { this.inboundEnabled = inboundEnabled; return this; }
        public Builder maxTokensPerSecond(int maxTokensPerSecond) { this.maxTokensPerSecond = maxTokensPerSecond; return this; }
        public Builder burstSize(int burstSize) { this.burstSize = burstSize; return this; }
        public Builder inboundOverflowStrategy(OverflowStrategy inboundOverflowStrategy) { this.inboundOverflowStrategy = inboundOverflowStrategy; return this; }
        
        public Builder globalBackpressureEnabled(boolean globalBackpressureEnabled) { this.globalBackpressureEnabled = globalBackpressureEnabled; return this; }
        public Builder globalTriggerThreshold(double globalTriggerThreshold) { this.globalTriggerThreshold = globalTriggerThreshold; return this; }
        public Builder globalReleaseThreshold(double globalReleaseThreshold) { this.globalReleaseThreshold = globalReleaseThreshold; return this; }
        public Builder globalReleaseCooldownMs(long globalReleaseCooldownMs) { this.globalReleaseCooldownMs = globalReleaseCooldownMs; return this; }
        
        public Builder topicBackpressureEnabled(boolean topicBackpressureEnabled) { this.topicBackpressureEnabled = topicBackpressureEnabled; return this; }
        public Builder topicTriggerThreshold(double topicTriggerThreshold) { this.topicTriggerThreshold = topicTriggerThreshold; return this; }
        public Builder topicReleaseThreshold(double topicReleaseThreshold) { this.topicReleaseThreshold = topicReleaseThreshold; return this; }
        
        public Builder outboundEnabled(boolean outboundEnabled) { this.outboundEnabled = outboundEnabled; return this; }
        public Builder maxQueueSize(int maxQueueSize) { this.maxQueueSize = maxQueueSize; return this; }
        public Builder disconnectThreshold(int disconnectThreshold) { this.disconnectThreshold = disconnectThreshold; return this; }
        public Builder outboundOverflowStrategy(OverflowStrategy outboundOverflowStrategy) { this.outboundOverflowStrategy = outboundOverflowStrategy; return this; }
        
        public FlowControlConfig build() { return new FlowControlConfig(this); }
    }
    
    public static FlowControlConfig defaultConfig() { return builder().build(); }
}