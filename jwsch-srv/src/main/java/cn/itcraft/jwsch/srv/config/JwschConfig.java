package cn.itcraft.jwsch.srv.config;

import cn.itcraft.jwsch.common.flowcontrol.FlowControlConfig;
import cn.itcraft.jwsch.srv.cluster.ClusterConfig;

/**
 * Main jwsch server configuration.
 *
 * <p>Uses Builder pattern to create immutable configuration:
 * <pre>
 * JwschConfig config = JwschConfig.builder()
 *     .enabled(true)
 *     .bossThreads(1)
 *     .workerThreads(0)
 *     .webSocket(WebSocketConfig.builder().build())
 *     .tcp(TcpConfig.builder().build())
 *     .cluster(new ClusterConfig())
 *     .health(HealthConfig.builder().build())
 *     .metrics(MetricsConfig.builder().build())
 *     .flowControl(FlowControlConfig.defaultConfig())
 *     .slowQueryThresholdMs(100)
 *     .jmxEnabled(false)
 *     .build();
 * </pre>
 */
public final class JwschConfig {
    
    private static final int DEFAULT_SLOW_QUERY_THRESHOLD_MS = 100;
    
    /** Whether the jwsch server is enabled */
    private final boolean enabled;
    /** Number of boss threads for Netty event loop */
    private final int bossThreads;
    /** Number of worker threads for Netty event loop (0 = auto-detect) */
    private final int workerThreads;
    /** WebSocket server configuration */
    private final WebSocketConfig webSocket;
    /** TCP server configuration */
    private final TcpConfig tcp;
    /** Cluster configuration */
    private final ClusterConfig cluster;
    /** Health check configuration */
    private final HealthConfig health;
    /** Metrics configuration */
    private final MetricsConfig metrics;
    /** Flow control configuration */
    private final FlowControlConfig flowControl;
    /** Slow query threshold in milliseconds */
    private final int slowQueryThresholdMs;
    /** Whether JMX monitoring is enabled */
    private final boolean jmxEnabled;
    
    private JwschConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.bossThreads = builder.bossThreads;
        this.workerThreads = builder.workerThreads;
        this.webSocket = builder.webSocket;
        this.tcp = builder.tcp;
        this.cluster = builder.cluster;
        this.health = builder.health;
        this.metrics = builder.metrics;
        this.flowControl = builder.flowControl;
        this.slowQueryThresholdMs = builder.slowQueryThresholdMs;
        this.jmxEnabled = builder.jmxEnabled;
    }
    
    public boolean isEnabled() { return enabled; }
    public int getBossThreads() { return bossThreads; }
    public int getWorkerThreads() { return workerThreads; }
    public WebSocketConfig getWebSocket() { return webSocket; }
    public TcpConfig getTcp() { return tcp; }
    public ClusterConfig getCluster() { return cluster; }
    public HealthConfig getHealth() { return health; }
    public MetricsConfig getMetrics() { return metrics; }
    public FlowControlConfig getFlowControl() { return flowControl; }
    public int getSlowQueryThresholdMs() { return slowQueryThresholdMs; }
    public boolean isJmxEnabled() { return jmxEnabled; }
    
    public static final class Builder {
        private boolean enabled = true;
        private int bossThreads = 1;
        private int workerThreads = 0;
        private WebSocketConfig webSocket = new WebSocketConfig.Builder().build();
        private TcpConfig tcp = new TcpConfig.Builder().build();
        private ClusterConfig cluster = new ClusterConfig();
        private HealthConfig health = new HealthConfig.Builder().build();
        private MetricsConfig metrics = new MetricsConfig.Builder().build();
        private FlowControlConfig flowControl = FlowControlConfig.defaultConfig();
        private int slowQueryThresholdMs = DEFAULT_SLOW_QUERY_THRESHOLD_MS;
        private boolean jmxEnabled = false;
        
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder bossThreads(int bossThreads) { this.bossThreads = bossThreads; return this; }
        public Builder workerThreads(int workerThreads) { this.workerThreads = workerThreads; return this; }
        public Builder webSocket(WebSocketConfig webSocket) { this.webSocket = webSocket; return this; }
        public Builder tcp(TcpConfig tcp) { this.tcp = tcp; return this; }
        public Builder cluster(ClusterConfig cluster) { this.cluster = cluster; return this; }
        public Builder health(HealthConfig health) { this.health = health; return this; }
        public Builder metrics(MetricsConfig metrics) { this.metrics = metrics; return this; }
        public Builder flowControl(FlowControlConfig flowControl) { this.flowControl = flowControl; return this; }
        public Builder slowQueryThresholdMs(int slowQueryThresholdMs) { 
            this.slowQueryThresholdMs = slowQueryThresholdMs; 
            return this; 
        }
        public Builder jmxEnabled(boolean jmxEnabled) {
            this.jmxEnabled = jmxEnabled;
            return this;
        }
        
        public JwschConfig build() { return new JwschConfig(this); }
    }
    
    public static Builder builder() { return new Builder(); }
}