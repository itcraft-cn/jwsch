package cn.itcraft.jwsch.bench;

/**
 * Benchmark 配置类。
 * 
 * <p>使用 Builder 模式创建配置，支持链式调用。
 */
public final class BenchConfig {
    
    public static final int MIN_PAYLOAD_SIZE = 2;
    public static final int MAX_PAYLOAD_SIZE = 500 * 1024;
    
    private final int wsPort;
    private final int tcpPort;
    private final int subscriberCount;
    private final int publisherCount;
    private final String topic;
    private final long sendIntervalMicros;
    private final int payloadSize;
    private final int durationMinutes;
    private final int reportIntervalSeconds;
    private final int workerThreads;
    
    private BenchConfig(Builder builder) {
        this.wsPort = builder.wsPort;
        this.tcpPort = builder.tcpPort;
        this.subscriberCount = builder.subscriberCount;
        this.publisherCount = builder.publisherCount;
        this.topic = builder.topic;
        this.sendIntervalMicros = builder.sendIntervalMicros;
        this.payloadSize = builder.payloadSize;
        this.durationMinutes = builder.durationMinutes;
        this.reportIntervalSeconds = builder.reportIntervalSeconds;
        this.workerThreads = builder.workerThreads;
    }
    
    public int getWsPort() { return wsPort; }
    public int getTcpPort() { return tcpPort; }
    public int getSubscriberCount() { return subscriberCount; }
    public int getPublisherCount() { return publisherCount; }
    public String getTopic() { return topic; }
    public long getSendIntervalMicros() { return sendIntervalMicros; }
    public int getPayloadSize() { return payloadSize; }
    public int getMessageSizeBytes() { return 8 + payloadSize; }
    public int getDurationMinutes() { return durationMinutes; }
    public int getReportIntervalSeconds() { return reportIntervalSeconds; }
    public int getWorkerThreads() { return workerThreads; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        private int wsPort = 8080;
        private int tcpPort = 9090;
        private int subscriberCount = 5;
        private int publisherCount = 1;
        private String topic = "/topic/bench";
        private long sendIntervalMicros = 10;
        private int payloadSize = 2;
        private int durationMinutes = 5;
        private int reportIntervalSeconds = 5;
        private int workerThreads = 16;
        
        public Builder wsPort(int wsPort) {
            this.wsPort = wsPort;
            return this;
        }
        
        public Builder tcpPort(int tcpPort) {
            this.tcpPort = tcpPort;
            return this;
        }
        
        public Builder subscriberCount(int subscriberCount) {
            this.subscriberCount = subscriberCount;
            return this;
        }
        
        public Builder publisherCount(int publisherCount) {
            this.publisherCount = publisherCount;
            return this;
        }
        
        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }
        
        public Builder sendIntervalMicros(long sendIntervalMicros) {
            this.sendIntervalMicros = sendIntervalMicros;
            return this;
        }
        
        public Builder payloadSize(int payloadSize) {
            this.payloadSize = Math.max(MIN_PAYLOAD_SIZE, Math.min(MAX_PAYLOAD_SIZE, payloadSize));
            return this;
        }
        
        public Builder durationMinutes(int durationMinutes) {
            this.durationMinutes = durationMinutes;
            return this;
        }
        
        public Builder reportIntervalSeconds(int reportIntervalSeconds) {
            this.reportIntervalSeconds = reportIntervalSeconds;
            return this;
        }
        
        public Builder workerThreads(int workerThreads) {
            this.workerThreads = workerThreads;
            return this;
        }
        
        public BenchConfig build() {
            return new BenchConfig(this);
        }
    }
}