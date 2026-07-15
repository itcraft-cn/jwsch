package cn.itcraft.jwsch.srv.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.itcraft.jwsch.common.protocol.TopicHash;
import cn.itcraft.jwsch.srv.cluster.message.ClusterSync;
import cn.itcraft.jwsch.srv.router.TopicSubscription;

/**
 * Service for synchronizing connection and subscription information across cluster.
 * 
 * <p>Handles:
 * <ul>
 *   <li>Event-driven incremental sync on connection/subscription changes</li>
 *   <li>Periodic full sync for consistency</li>
 * </ul>
 * 
 * <p>Sync operations:
 * <ul>
 *   <li>ADD_CONNECTION: New connection established</li>
 *   <li>REMOVE_CONNECTION: Connection closed</li>
 *   <li>ADD_SUBSCRIPTION: Client subscribed to topic</li>
 *   <li>REMOVE_SUBSCRIPTION: Client unsubscribed from topic</li>
 * </ul>
 */
public class ClusterSyncService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSyncService.class);
    
    private final ClusterConfig config;
    private final ClusterClient client;
    private final ClusterConnectionRegistry connectionRegistry;
    private final TopicSubscription topicSubscription;
    private final NodeBloomFilter localBloomFilter;
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
        r -> {
            Thread t = new Thread(r, "cluster-sync-scheduler");
            t.setDaemon(true);
            return t;
        }
    );
    
    private final AtomicBoolean started = new AtomicBoolean(false);
    private volatile ClusterSync pendingSync;
    private final Object syncLock = new Object();
    
    public ClusterSyncService(ClusterConfig config,
                              ClusterClient client,
                              ClusterConnectionRegistry connectionRegistry,
                              TopicSubscription topicSubscription,
                              NodeBloomFilter localBloomFilter) {
        this.config = config;
        this.client = client;
        this.connectionRegistry = connectionRegistry;
        this.topicSubscription = topicSubscription;
        this.localBloomFilter = localBloomFilter;
    }
    
    /**
     * Start sync service.
     */
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        
        int fullSyncIntervalSeconds = config.getSyncIntervalSeconds();
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendFullSync();
            } catch (Exception e) {
                LOGGER.error("Full sync error", e);
            }
        }, fullSyncIntervalSeconds, fullSyncIntervalSeconds, TimeUnit.SECONDS);
        
        LOGGER.info("ClusterSyncService started, fullSyncInterval={}s", fullSyncIntervalSeconds);
    }
    
    /**
     * Stop sync service.
     */
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        
        scheduler.shutdown();
        LOGGER.info("ClusterSyncService stopped");
    }
    
    /**
     * Called when a local connection is added.
     */
    public void onLocalConnectionAdd(long connectionId) {
        connectionRegistry.addLocalConnection(connectionId, 
            new ConnectionMeta(connectionId, "websocket", "local"));
        
        ClusterSync.SyncOp op = new ClusterSync.SyncOp(
            ClusterSync.OP_ADD_CONNECTION,
            connectionId
        );
        
        sendIncrementalSync(op);
        LOGGER.debug("Local connection added: {}", connectionId);
    }
    
    /**
     * Called when a local connection is removed.
     */
    public void onLocalConnectionRemove(long connectionId) {
        connectionRegistry.removeLocalConnection(connectionId);
        
        ClusterSync.SyncOp op = new ClusterSync.SyncOp(
            ClusterSync.OP_REMOVE_CONNECTION,
            connectionId
        );
        
        sendIncrementalSync(op);
        LOGGER.debug("Local connection removed: {}", connectionId);
    }
    
    /**
     * Called when a client subscribes to a topic.
     */
    public void onLocalSubscribe(long connectionId, String topic) {
        long topicHash = TopicHash.hash(topic);
        
        localBloomFilter.addTopic(topicHash);
        
        Set<Long> topicHashes = new HashSet<>();
        topicHashes.add(topicHash);
        
        ClusterSync.SyncOp op = new ClusterSync.SyncOp(
            ClusterSync.OP_ADD_SUBSCRIPTION,
            connectionId,
            topicHashes
        );
        
        sendIncrementalSync(op);
        LOGGER.debug("Local subscription added: conn={}, topic={}", connectionId, topic);
    }
    
    /**
     * Called when a client unsubscribes from a topic.
     */
    public void onLocalUnsubscribe(long connectionId, String topic) {
        long topicHash = TopicHash.hash(topic);
        
        Set<Long> topicHashes = new HashSet<>();
        topicHashes.add(topicHash);
        
        ClusterSync.SyncOp op = new ClusterSync.SyncOp(
            ClusterSync.OP_REMOVE_SUBSCRIPTION,
            connectionId,
            topicHashes
        );
        
        sendIncrementalSync(op);
        LOGGER.debug("Local subscription removed: conn={}, topic={}", connectionId, topic);
    }
    
    /**
     * Send full sync to all nodes.
     */
    public void sendFullSync() {
        if (!client.hasConnectedNodes()) {
            return;
        }
        
        List<ClusterSync.SyncOp> operations = buildFullSyncOps();
        
        if (operations.isEmpty()) {
            LOGGER.debug("No local connections, skipping full sync");
            return;
        }
        
        ClusterSync sync = new ClusterSync(ClusterSync.SYNC_FULL, operations);
        client.broadcastClusterMessage(sync);
        
        LOGGER.debug("Sent full sync: {} operations", operations.size());
    }
    
    /**
     * Send incremental sync to all nodes.
     */
    private void sendIncrementalSync(ClusterSync.SyncOp op) {
        if (!client.hasConnectedNodes()) {
            return;
        }
        
        List<ClusterSync.SyncOp> operations = new ArrayList<>(1);
        operations.add(op);
        
        ClusterSync sync = new ClusterSync(ClusterSync.SYNC_INCREMENTAL, operations);
        client.broadcastClusterMessage(sync);
        
        LOGGER.debug("Sent incremental sync: {}", op);
    }
    
    /**
     * Build full sync operations from current state.
     */
    private List<ClusterSync.SyncOp> buildFullSyncOps() {
        List<ClusterSync.SyncOp> operations = new ArrayList<>();
        
        Set<Long> localConnectionIds = connectionRegistry.getLocalConnectionIds();
        
        for (Long connectionId : localConnectionIds) {
        Set<Long> topicHashes = topicSubscription.getTopicHashesForConnection(connectionId);
            
            ClusterSync.SyncOp op = new ClusterSync.SyncOp(
                ClusterSync.OP_ADD_CONNECTION,
                connectionId,
                topicHashes
            );
            operations.add(op);
        }
        
        return operations;
    }
    
    public boolean isStarted() {
        return started.get();
    }
}
