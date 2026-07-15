package cn.itcraft.jwsch.srv.server.websocket;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class WebSocketConnectionManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketConnectionManager.class);
    
    private static final WebSocketConnectionManager INSTANCE = new WebSocketConnectionManager();
    
    private static final long DEFAULT_INACTIVE_CHECK_INTERVAL_MS = 60000;
    private static final long DEFAULT_MAX_INACTIVE_TIME_MS = 300000;
    
    private final Map<Long, Channel> channelMap;
    private final List<Channel> channels;
    private final Map<String, List<Channel>> topicChannels;
    private final Map<Long, Long> lastActiveTimeMap;
    private final AtomicLong connectionIdCounter;
    
    private ScheduledExecutorService cleanupExecutor;
    private long inactiveCheckIntervalMs = DEFAULT_INACTIVE_CHECK_INTERVAL_MS;
    private long maxInactiveTimeMs = DEFAULT_MAX_INACTIVE_TIME_MS;
    private volatile boolean cleanupEnabled = false;
    
    private WebSocketConnectionManager() {
        this.channelMap = new ConcurrentHashMap<>();
        this.channels = new CopyOnWriteArrayList<>();
        this.topicChannels = new ConcurrentHashMap<>();
        this.lastActiveTimeMap = new ConcurrentHashMap<>();
        this.connectionIdCounter = new AtomicLong(0);
    }
    
    public static WebSocketConnectionManager getInstance() {
        return INSTANCE;
    }
    
    public long addChannel(Channel channel) {
        if (channel == null) {
            return -1;
        }
        
        long connectionId = connectionIdCounter.incrementAndGet();
        channelMap.put(connectionId, channel);
        channels.add(channel);
        lastActiveTimeMap.put(connectionId, System.currentTimeMillis());
        
        LOGGER.debug("Added channel: connectionId={}, total={}", connectionId, channels.size());
        return connectionId;
    }
    
    public void addChannel(Long connectionId, Channel channel) {
        if (channel == null) {
            return;
        }
        
        channelMap.put(connectionId, channel);
        channels.add(channel);
        lastActiveTimeMap.put(connectionId, System.currentTimeMillis());
        LOGGER.debug("Added channel: connectionId={}, total={}", connectionId, channels.size());
    }
    
    public void removeChannel(Long connectionId) {
        Channel channel = channelMap.remove(connectionId);
        if (channel != null) {
            channels.remove(channel);
            lastActiveTimeMap.remove(connectionId);
            
            for (List<Channel> topicChannelList : topicChannels.values()) {
                topicChannelList.remove(channel);
            }
            
            LOGGER.debug("Removed channel: connectionId={}, total={}", connectionId, channels.size());
        }
    }
    
    public void updateActiveTime(Long connectionId) {
        if (connectionId != null) {
            lastActiveTimeMap.put(connectionId, System.currentTimeMillis());
        }
    }
    
    public void subscribeTopic(Long connectionId, String topic) {
        Channel channel = channelMap.get(connectionId);
        if (channel == null || topic == null) {
            return;
        }
        
        topicChannels.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(channel);
        updateActiveTime(connectionId);
        LOGGER.debug("Subscribed topic: connectionId={}, topic={}", connectionId, topic);
    }
    
    public void unsubscribeTopic(Long connectionId, String topic) {
        Channel channel = channelMap.get(connectionId);
        if (channel == null || topic == null) {
            return;
        }
        
        List<Channel> topicChannelList = topicChannels.get(topic);
        if (topicChannelList != null) {
            topicChannelList.remove(channel);
        }
        LOGGER.debug("Unsubscribed topic: connectionId={}, topic={}", connectionId, topic);
    }
    
    public void broadcast(String message) {
        LOGGER.debug("Broadcasting message to {} channels", channels.size());
        
        Iterator<Channel> iterator = channels.iterator();
        while (iterator.hasNext()) {
            Channel channel = iterator.next();
            if (channel.isActive()) {
                channel.writeAndFlush(new TextWebSocketFrame(message));
            }
        }
    }
    
    public void broadcast(byte[] data) {
        LOGGER.debug("Broadcasting binary to {} channels", channels.size());
        
        Iterator<Channel> iterator = channels.iterator();
        while (iterator.hasNext()) {
            Channel channel = iterator.next();
            if (channel.isActive()) {
                channel.writeAndFlush(new BinaryWebSocketFrame(
                    channel.alloc().buffer(data.length).writeBytes(data)));
            }
        }
    }
    
    public void broadcastToTopic(String topic, String message) {
        List<Channel> topicChannelList = topicChannels.get(topic);
        if (topicChannelList == null || topicChannelList.isEmpty()) {
            LOGGER.debug("No channels subscribed to topic: {}", topic);
            return;
        }
        
        LOGGER.debug("Broadcasting to topic {}: {} channels", topic, topicChannelList.size());
        
        for (Channel channel : topicChannelList) {
            if (channel.isActive()) {
                channel.writeAndFlush(new TextWebSocketFrame(message));
            }
        }
    }
    
    public void removeInactiveChannels() {
        long now = System.currentTimeMillis();
        List<Long> toRemove = new ArrayList<>();
        
        for (Map.Entry<Long, Long> entry : lastActiveTimeMap.entrySet()) {
            Long connectionId = entry.getKey();
            Long lastActive = entry.getValue();
            
            Channel channel = channelMap.get(connectionId);
            if (channel == null || !channel.isActive()) {
                toRemove.add(connectionId);
            } else if (now - lastActive > maxInactiveTimeMs) {
                channel.close();
                toRemove.add(connectionId);
                LOGGER.info("Closed inactive channel: connectionId={}", connectionId);
            }
        }
        
        for (Long connectionId : toRemove) {
            removeChannel(connectionId);
        }
        
        if (!toRemove.isEmpty()) {
            LOGGER.info("Removed {} inactive channels", toRemove.size());
        }
    }
    
    public void startInactiveCheck() {
        if (cleanupEnabled) {
            return;
        }
        
        cleanupEnabled = true;
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "jwsch-ws-cleanup");
            t.setDaemon(true);
            return t;
        });
        
        cleanupExecutor.scheduleAtFixedRate(
            this::removeInactiveChannels,
            inactiveCheckIntervalMs,
            inactiveCheckIntervalMs,
            TimeUnit.MILLISECONDS);
        
        LOGGER.info("Started inactive channel check, interval={}ms, maxInactive={}ms", 
            inactiveCheckIntervalMs, maxInactiveTimeMs);
    }
    
    public void stopInactiveCheck() {
        cleanupEnabled = false;
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            cleanupExecutor = null;
        }
        LOGGER.info("Stopped inactive channel check");
    }
    
    public ConnectionStats getConnectionStats() {
        int activeCount = 0;
        int inactiveCount = 0;
        
        for (Channel channel : channels) {
            if (channel.isActive()) {
                activeCount++;
            } else {
                inactiveCount++;
            }
        }
        
        return new ConnectionStats(channels.size(), activeCount, inactiveCount, topicChannels.size());
    }
    
    public int getConnectionCount() {
        return channels.size();
    }
    
    public List<Channel> getAllChannels() {
        return Collections.unmodifiableList(new ArrayList<>(channels));
    }
    
    public void setInactiveCheckIntervalMs(long inactiveCheckIntervalMs) {
        this.inactiveCheckIntervalMs = inactiveCheckIntervalMs;
    }
    
    public void setMaxInactiveTimeMs(long maxInactiveTimeMs) {
        this.maxInactiveTimeMs = maxInactiveTimeMs;
    }
    
    public void clear() {
        channelMap.clear();
        channels.clear();
        topicChannels.clear();
        lastActiveTimeMap.clear();
        stopInactiveCheck();
        LOGGER.info("Cleared all connections");
    }
    
    public static final class ConnectionStats {
        private final int total;
        private final int active;
        private final int inactive;
        private final int topicCount;
        
        public ConnectionStats(int total, int active, int inactive, int topicCount) {
            this.total = total;
            this.active = active;
            this.inactive = inactive;
            this.topicCount = topicCount;
        }
        
        public int getTotal() {
            return total;
        }
        
        public int getActive() {
            return active;
        }
        
        public int getInactive() {
            return inactive;
        }
        
        public int getTopicCount() {
            return topicCount;
        }
        
        @Override
        public String toString() {
            return "ConnectionStats{total=" + total + ", active=" + active + 
                ", inactive=" + inactive + ", topics=" + topicCount + "}";
        }
    }
}