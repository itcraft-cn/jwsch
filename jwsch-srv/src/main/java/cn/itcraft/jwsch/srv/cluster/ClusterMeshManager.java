package cn.itcraft.jwsch.srv.cluster;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.itcraft.jwsch.srv.cluster.message.ClusterHeartbeat;
import cn.itcraft.jwsch.srv.cluster.message.ClusterJoin;
import cn.itcraft.jwsch.srv.cluster.message.ClusterMembership;

/**
 * Manager for cluster mesh lifecycle.
 * 
 * <p>Responsible for:
 * <ul>
 *   <li>Starting cluster server and connecting to other nodes</li>
 *   <li>Node discovery and membership management</li>
 *   <li>Heartbeat scheduling</li>
 * </ul>
 * 
 * <p>Startup flow:
 * <ol>
 *   <li>Resolve advertise-host (JVM param > env var > auto-detect)</li>
 *   <li>Bind to base-port, if fails try base-port+1...</li>
 *   <li>Non-base-port nodes: wait startup-wait-seconds, connect to base-port</li>
 *   <li>Send CLUSTER_JOIN, receive CLUSTER_MEMBERSHIP</li>
 *   <li>Connect to other nodes</li>
 * </ol>
 */
public class ClusterMeshManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterMeshManager.class);
    
    private final ClusterConfig config;
    private final ClusterServer server;
    private final ClusterClient client;
    private final ClusterConnectionRegistry connectionRegistry;
    private final InMemoryClusterNodeRegistry nodeRegistry;
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
        r -> {
            Thread t = new Thread(r, "cluster-mesh-scheduler");
            t.setDaemon(true);
            return t;
        }
    );
    
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, Long> heartbeatTimestamps = new ConcurrentHashMap<>();
    
    public ClusterMeshManager(ClusterConfig config,
                              ClusterConnectionRegistry connectionRegistry,
                              InMemoryClusterNodeRegistry nodeRegistry) {
        this.config = config;
        this.connectionRegistry = connectionRegistry;
        this.nodeRegistry = nodeRegistry;
        this.server = new ClusterServer(config, connectionRegistry, nodeRegistry);
        this.client = new ClusterClient(config);
        
        this.server.setMeshManager(this);
    }
    
    /**
     * Start cluster mesh.
     */
    public void start() throws Exception {
        if (!config.isEnabled()) {
            LOGGER.info("Cluster disabled, skipping startup");
            return;
        }
        
        if (!started.compareAndSet(false, true)) {
            LOGGER.warn("Cluster already started");
            return;
        }
        
        int bindPort = tryBindPort();
        config.setBindPort(bindPort);
        
        LOGGER.info("Cluster starting: nodeId={}, port={}", config.getNodeId(), bindPort);
        
        boolean isBasePort = (bindPort == config.getBasePort());
        
        if (!isBasePort) {
            waitAndConnectToBasePort();
        }
        
        startHeartbeat();
        
        LOGGER.info("Cluster started: nodeId={}, isBasePort={}", config.getNodeId(), isBasePort);
    }
    
    /**
     * Stop cluster mesh.
     */
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        
        scheduler.shutdown();
        server.stop();
        client.shutdown();
        
        LOGGER.info("Cluster stopped: nodeId={}", config.getNodeId());
    }
    
    /**
     * Try to bind to a port, starting from base-port.
     */
    private int tryBindPort() throws InterruptedException {
        int basePort = config.getBasePort();
        int portRange = config.getPortRange();
        
        for (int port = basePort; port < basePort + portRange; port++) {
            try {
                config.setBindPort(port);
                server.start();
                return port;
            } catch (Exception e) {
                LOGGER.debug("Port {} already in use, trying next", port);
            }
        }
        
        throw new IllegalStateException("No available port in range " + basePort + "-" + (basePort + portRange - 1));
    }
    
    /**
     * Wait for startup-wait-seconds and connect to base-port node.
     */
    private void waitAndConnectToBasePort() throws InterruptedException {
        int waitSeconds = config.getStartupWaitSeconds();
        String baseNodeId = config.getNodePrefix() + "-" + config.getAdvertiseHost() + "-" + config.getBasePort();
        
        LOGGER.info("Waiting {} seconds before connecting to base node {}", waitSeconds, baseNodeId);
        
        Thread.sleep(waitSeconds * 1000L);
        
        connectToBasePort();
    }
    
    /**
     * Connect to base-port node and send JOIN.
     */
    private void connectToBasePort() {
        String baseHost = config.getAdvertiseHost();
        int basePort = config.getBasePort();
        
        NodeInfo baseNode = new NodeInfo(
            config.getNodePrefix() + "-" + baseHost + "-" + basePort,
            baseHost,
            basePort,
            config.getWebsocketPort(),
            config.getHttpPort()
        );
        
        client.connect(baseNode);
        
        Channel channel = client.getChannel(baseNode.getNodeId());
        if (channel != null && channel.isActive()) {
            ClusterJoin join = new ClusterJoin(
                config.getNodeId(),
                config.getAdvertiseHost(),
                config.getClusterPort()
            );
            channel.writeAndFlush(join);
            LOGGER.info("Sent CLUSTER_JOIN to base node {}", baseNode.getNodeId());
        }
    }
    
    /**
     * Connect to a node from membership list.
     */
    public void connectToNode(NodeInfo node) {
        if (node.getNodeId().equals(config.getNodeId())) {
            return;
        }
        
        if (client.isConnected(node.getNodeId())) {
            return;
        }
        
        client.connect(node);
        
        Channel channel = client.getChannel(node.getNodeId());
        if (channel != null && channel.isActive()) {
            ClusterJoin join = new ClusterJoin(
                config.getNodeId(),
                config.getAdvertiseHost(),
                config.getClusterPort()
            );
            channel.writeAndFlush(join);
            LOGGER.info("Sent CLUSTER_JOIN to node {}", node.getNodeId());
        }
    }
    
    /**
     * Handle received CLUSTER_MEMBERSHIP.
     */
    public void onMembershipReceived(ClusterMembership membership) {
        LOGGER.info("Received CLUSTER_MEMBERSHIP with {} nodes", membership.getNodes().size());
        
        for (NodeInfo node : membership.getNodes()) {
            nodeRegistry.register(node);
            connectToNode(node);
        }
    }
    
    /**
     * Handle received CLUSTER_JOIN.
     */
    public void onNodeJoin(ClusterJoin join, Channel channel) {
        String nodeId = join.getNodeId();
        
        NodeInfo nodeInfo = new NodeInfo(
            nodeId,
            join.getHost(),
            join.getPort(),
            config.getWebsocketPort(),
            config.getHttpPort()
        );
        
        nodeRegistry.register(nodeInfo);
        
        updateHeartbeat(nodeId);
        
        LOGGER.info("Node joined: {} at {}:{}", nodeId, join.getHost(), join.getPort());
        
        ClusterMembership membership = buildMembershipMessage();
        channel.writeAndFlush(membership);
    }
    
    /**
     * Handle received CLUSTER_HEARTBEAT.
     */
    public void onHeartbeat(String nodeId) {
        updateHeartbeat(nodeId);
    }
    
    /**
     * Update heartbeat timestamp for a node.
     */
    private void updateHeartbeat(String nodeId) {
        heartbeatTimestamps.put(nodeId, System.currentTimeMillis());
    }
    
    /**
     * Start heartbeat scheduler.
     */
    private void startHeartbeat() {
        int intervalSeconds = config.getHeartbeatIntervalSeconds();
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendHeartbeat();
                checkHeartbeatTimeout();
            } catch (Exception e) {
                LOGGER.error("Heartbeat error", e);
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }
    
    /**
     * Send heartbeat to all connected nodes.
     */
    private void sendHeartbeat() {
        ClusterHeartbeat heartbeat = new ClusterHeartbeat();
        client.broadcastClusterMessage(heartbeat);
    }
    
    /**
     * Check heartbeat timeout for all nodes.
     */
    private void checkHeartbeatTimeout() {
        long now = System.currentTimeMillis();
        long timeoutMs = config.getHeartbeatTimeoutSeconds() * 1000L;
        
        Set<String> timeoutNodes = new HashSet<>();
        
        for (String nodeId : heartbeatTimestamps.keySet()) {
            Long lastHeartbeat = heartbeatTimestamps.get(nodeId);
            if (lastHeartbeat != null && (now - lastHeartbeat) > timeoutMs) {
                timeoutNodes.add(nodeId);
            }
        }
        
        for (String nodeId : timeoutNodes) {
            LOGGER.warn("Node heartbeat timeout: {}", nodeId);
            heartbeatTimestamps.remove(nodeId);
            nodeRegistry.deregister(nodeId);
            client.disconnect(nodeId);
            connectionRegistry.removeNodeConnections(nodeId);
        }
    }
    
    /**
     * Build membership message with all known nodes.
     */
    public ClusterMembership buildMembershipMessage() {
        List<NodeInfo> nodes = new ArrayList<>();
        nodes.add(config.toNodeInfo());
        nodes.addAll(nodeRegistry.getNodes());
        return new ClusterMembership(nodes);
    }
    
    public ClusterClient getClusterClient() {
        return client;
    }
    
    public ClusterServer getClusterServer() {
        return server;
    }
    
    public List<NodeInfo> getKnownNodes() {
        return nodeRegistry.getNodes();
    }
    
    public String getLocalNodeId() {
        return config.getNodeId();
    }
    
    public boolean isStarted() {
        return started.get();
    }
}
