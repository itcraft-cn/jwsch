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
    
    /**
     * 获取 WebSocket 端口。
     * 
     * @return WebSocket 监听端口
     */
    public int getWsPort() { return wsPort; }
    /**
     * 获取 TCP 端口。
     * 
     * @return TCP 监听端口
     */
    public int getTcpPort() { return tcpPort; }
    /**
     * 获取订阅者数量。
     * 
     * @return 订阅者总数
     */
    public int getSubscriberCount() { return subscriberCount; }
    /**
     * 获取发布者数量。
     * 
     * @return 发布者总数
     */
    public int getPublisherCount() { return publisherCount; }
    /**
     * 获取测试主题。
     * 
     * @return 测试主题路径
     */
    public String getTopic() { return topic; }
    /**
     * 获取发送间隔（微秒）。
     * 
     * @return 消息发送间隔，单位微秒
     */
    public long getSendIntervalMicros() { return sendIntervalMicros; }
    /**
     * 获取负载大小（字节）。
     * 
     * @return 消息负载大小，单位字节
     */
    public int getPayloadSize() { return payloadSize; }
    /**
     * 获取完整消息大小（字节）。
     * 
     * <p>包含 8 字节序列号和负载大小。
     * 
     * @return 完整消息大小，单位字节
     */
    public int getMessageSizeBytes() { return 8 + payloadSize; }
    /**
     * 获取测试持续时间（分钟）。
     * 
     * @return 测试持续时间，单位分钟
     */
    public int getDurationMinutes() { return durationMinutes; }
    /**
     * 获取报告间隔（秒）。
     * 
     * @return TPS 报告间隔，单位秒
     */
    public int getReportIntervalSeconds() { return reportIntervalSeconds; }
    /**
     * 获取工作线程数。
     * 
     * @return 工作线程数量
     */
    public int getWorkerThreads() { return workerThreads; }
    
    /**
     * 创建 Builder 实例。
     * 
     * @return 新的 {@link Builder} 实例
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Benchmark 配置 Builder 类。
     * 
     * <p>提供链式调用接口，支持默认值和范围校验。
     */
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