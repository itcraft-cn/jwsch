package cn.itcraft.jwsch.srv;

import cn.itcraft.jwsch.srv.config.JwschConfig;
import cn.itcraft.jwsch.srv.cluster.ClusterClient;
import cn.itcraft.jwsch.srv.cluster.ClusterConfig;
import cn.itcraft.jwsch.srv.cluster.ClusterConnectionRegistry;
import cn.itcraft.jwsch.srv.cluster.ClusterForwarder;
import cn.itcraft.jwsch.srv.cluster.ClusterMeshManager;
import cn.itcraft.jwsch.srv.cluster.ClusterSyncService;
import cn.itcraft.jwsch.srv.cluster.InMemoryClusterNodeRegistry;
import cn.itcraft.jwsch.srv.cluster.NodeBloomFilter;
import cn.itcraft.jwsch.srv.config.HealthConfig;
import cn.itcraft.jwsch.srv.session.RouterConnectionManager;
import cn.itcraft.jwsch.srv.config.MetricsConfig;
import cn.itcraft.jwsch.srv.config.TcpConfig;
import cn.itcraft.jwsch.srv.config.WebSocketConfig;
import cn.itcraft.jwsch.srv.health.HealthCheckServer;
import cn.itcraft.jwsch.srv.loadbalance.LoadBalance;
import cn.itcraft.jwsch.srv.loadbalance.RoundRobinLoadBalance;
import cn.itcraft.jwsch.srv.metrics.DefaultServerMetrics;
import cn.itcraft.jwsch.srv.metrics.MetricsServer;
import cn.itcraft.jwsch.srv.metrics.NoOpServerMetrics;
import cn.itcraft.jwsch.srv.metrics.ServerMetrics;
import cn.itcraft.jwsch.srv.registry.InMemoryServiceRegistry;
import cn.itcraft.jwsch.srv.registry.ServiceRegistry;
import cn.itcraft.jwsch.srv.router.PacketRouter;
import cn.itcraft.jwsch.srv.server.tcp.TcpServer;
import cn.itcraft.jwsch.srv.server.websocket.WebSocketServer;
import cn.itcraft.jwsch.srv.flowcontrol.TopicBackpressureManager;
import cn.itcraft.jwsch.srv.stats.DefaultTopicStatsManager;
import cn.itcraft.jwsch.srv.stats.NoOpTopicStatsManager;
import cn.itcraft.jwsch.srv.stats.TopicStatsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Jwsch 服务器主类。
 * 
 * <p>管理所有服务组件的生命周期：
 * <ul>
 *   <li>WebSocketServer：前端 WebSocket 连接（必选）</li>
 *   <li>TcpServer：后端 TCP 连接（必选）</li>
 *   <li>HealthCheckServer：健康检查端点（可选）</li>
 *   <li>MetricsServer：Prometheus 指标端点（可选）</li>
 * </ul>
 * 
 * <p>EventLoopGroup 架构（分离模式）：
 * <ul>
 *   <li>WebSocketServer：独立的 boss/worker EventLoopGroup</li>
 *   <li>TcpServer：独立的 boss/worker EventLoopGroup</li>
 *   <li>Health/Metrics：共享 WebSocket 的 EventLoopGroup（低流量）</li>
 * </ul>
 * 
 * <p>分离模式优势：
 * <ul>
 *   <li>WebSocket fan-out 不阻塞 TCP 接收</li>
 *   <li>前端/后端流量隔离，互不影响</li>
 *   <li>原生传输（Epoll）支持，Linux 性能提升</li>
 * </ul>
 * 
 * <p>启动流程：
 * <pre>
 * JwschConfig config = JwschConfig.builder()
 *     .webSocket(WebSocketConfig.builder().port(8080).build())
 *     .tcp(TcpConfig.builder().port(9090).build())
 *     .build();
 * 
 * JwschServer server = new JwschServer(config);
 * server.start();
 * </pre>
 */
public class JwschServer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JwschServer.class);
    
    private final JwschConfig config;
    private final ServiceRegistry serviceRegistry;
    private final LoadBalance loadBalance;
    private final PacketRouter packetRouter;
    private final ServerMetrics serverMetrics;
    private final TopicStatsManager topicStatsManager;
    
    private WebSocketServer webSocketServer;
    private TcpServer tcpServer;
    private HealthCheckServer healthCheckServer;
    private MetricsServer metricsServer;
    private ClusterMeshManager clusterMeshManager;
    private ClusterForwarder clusterForwarder;
    private ClusterSyncService clusterSyncService;
    private ClusterConnectionRegistry clusterConnectionRegistry;
    private InMemoryClusterNodeRegistry clusterNodeRegistry;
    private NodeBloomFilter nodeBloomFilter;
    private TopicBackpressureManager topicBackpressureManager;
    private volatile boolean started;
    
    public JwschServer() {
        this(JwschConfig.builder().build());
    }
    
    public JwschServer(JwschConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        
        this.serviceRegistry = new InMemoryServiceRegistry();
        this.loadBalance = new RoundRobinLoadBalance();
        this.packetRouter = new PacketRouter(serviceRegistry, loadBalance);
        
        boolean metricsEnabled = config.getMetrics().isEnabled();
        this.serverMetrics = metricsEnabled 
            ? new DefaultServerMetrics(config.isJmxEnabled()) 
            : NoOpServerMetrics.INSTANCE;
        this.topicStatsManager = metricsEnabled 
            ? new DefaultTopicStatsManager() 
            : NoOpTopicStatsManager.INSTANCE;
        
        this.packetRouter.setTopicStatsManager(topicStatsManager);
        this.serverMetrics.setTopicStatsManager(topicStatsManager);
        
        this.started = false;
    }
    
    public void start() {
        if (started) {
            LOGGER.warn("JwschServer already started");
            return;
        }
        
        if (!config.isEnabled()) {
            LOGGER.info("JwschServer is disabled");
            return;
        }
        
        WebSocketConfig wsConfig = config.getWebSocket();
        int slowQueryThresholdMs = config.getSlowQueryThresholdMs();
        webSocketServer = new WebSocketServer(wsConfig, packetRouter, serverMetrics, slowQueryThresholdMs, config.getFlowControl());
        webSocketServer.start();
        
        TcpConfig tcpConfig = config.getTcp();
        tcpServer = new TcpServer(tcpConfig, packetRouter, serverMetrics, config.getFlowControl());
        tcpServer.start();
        
        HealthConfig healthConfig = config.getHealth();
        if (healthConfig.isEnabled()) {
            healthCheckServer = new HealthCheckServer(healthConfig);
            healthCheckServer.start();
        }
        
        MetricsConfig metricsConfig = config.getMetrics();
        if (metricsConfig.isEnabled()) {
            metricsServer = new MetricsServer(metricsConfig, serverMetrics);
            metricsServer.start();
        }
        
        topicStatsManager.start();
        
        if (config.getFlowControl().isTopicBackpressureEnabled()) {
            topicBackpressureManager = new TopicBackpressureManager(
                packetRouter.getTopicSubscription(), 
                config.getFlowControl()
            );
            packetRouter.setTopicBackpressureManager(topicBackpressureManager);
            LOGGER.info("Topic-level backpressure enabled: trigger={}, release={}",
                config.getFlowControl().getTopicTriggerThreshold(),
                config.getFlowControl().getTopicReleaseThreshold());
        }
        
        ClusterConfig clusterConfig = config.getCluster();
        if (clusterConfig != null && clusterConfig.isEnabled()) {
            String localNodeId = clusterConfig.getNodeId();
            
            clusterConnectionRegistry = new ClusterConnectionRegistry(localNodeId);
            clusterNodeRegistry = new InMemoryClusterNodeRegistry(localNodeId);
            nodeBloomFilter = new NodeBloomFilter(10000);
            
            clusterMeshManager = new ClusterMeshManager(
                clusterConfig,
                clusterConnectionRegistry,
                clusterNodeRegistry
            );
            
            RouterConnectionManager connectionManager = new RouterConnectionManager(
                packetRouter.getFrontendConnectionsMap(),
                packetRouter.getTopicSubscription()
            );
            
            ClusterClient clusterClient = clusterMeshManager.getClusterClient();
            
            clusterForwarder = new ClusterForwarder(
                clusterConfig,
                clusterClient,
                clusterConnectionRegistry,
                clusterNodeRegistry,
                connectionManager,
                nodeBloomFilter
            );
            
            clusterSyncService = new ClusterSyncService(
                clusterConfig,
                clusterClient,
                clusterConnectionRegistry,
                packetRouter.getTopicSubscription(),
                nodeBloomFilter
            );
            
            try {
                clusterMeshManager.start();
            } catch (Exception e) {
                LOGGER.error("Failed to start cluster mesh", e);
            }
            
            packetRouter.setClusterForwarder(clusterForwarder);
            
            LOGGER.info("Cluster mesh started: nodeId={}, basePort={}", 
                clusterConfig.getNodeId(), clusterConfig.getBasePort());
        }
        
        started = true;
        
        LOGGER.info("JwschServer started (WebSocket: {}, TCP: {}, Health: {}, Metrics: {})",
            wsConfig.getPort(),
            tcpConfig.getPort(),
            healthConfig.isEnabled() ? healthConfig.getPort() : "disabled",
            metricsConfig.isEnabled() ? metricsConfig.getPort() : "disabled");
    }
    
    public void shutdown() {
        if (!started) {
            return;
        }
        
        if (webSocketServer != null) {
            webSocketServer.shutdown();
        }
        
        if (tcpServer != null) {
            tcpServer.shutdown();
        }
        
        if (healthCheckServer != null) {
            healthCheckServer.shutdown();
        }
        
        if (metricsServer != null) {
            metricsServer.shutdown();
        }
        
        if (clusterMeshManager != null) {
            clusterMeshManager.stop();
        }
        
        topicStatsManager.stop();
        serverMetrics.unregisterMBean();
        
        started = false;
        LOGGER.info("JwschServer shutdown");
    }
    
    public boolean isStarted() {
        return started;
    }
    
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }
    
    public LoadBalance getLoadBalance() {
        return loadBalance;
    }
    
    public PacketRouter getPacketRouter() {
        return packetRouter;
    }
    
    public ServerMetrics getServerMetrics() {
        return serverMetrics;
    }
    
    public HealthCheckServer getHealthCheckServer() {
        return healthCheckServer;
    }
    
    public MetricsServer getMetricsServer() {
        return metricsServer;
    }
    
    public TopicStatsManager getTopicStatsManager() {
        return topicStatsManager;
    }
}