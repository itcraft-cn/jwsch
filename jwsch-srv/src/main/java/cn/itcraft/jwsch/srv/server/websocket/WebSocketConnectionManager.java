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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket 连接管理器（单例）。
 * 
 * <p>管理所有 WebSocket 连接的生命周期，支持：
 * <ul>
 *   <li>连接注册、注销和查询</li>
 *   <li>基于 Topic 的订阅/取消订阅</li>
 *   <li>广播消息（文本和二进制）</li>
 *   <li>按 Topic 广播</li>
 *   <li>空闲连接检测和清理</li>
 *   <li>连接统计信息</li>
 * </ul>
 * 
 * <p>采用多级存储结构：
 * <ol>
 *   <li>connectionId → Channel 映射（channelMap）</li>
 *   <li>所有 Channel 列表（channels）</li>
 *   <li>Topic → Channel 列表映射（topicChannels）</li>
 *   <li>连接最后活跃时间映射（lastActiveNanosMap）</li>
 * </ol>
 * 
 * <p>线程安全，支持高并发访问。
 *
 * <p><b>架构收敛说明</b>：生产环境的连接与订阅管理已统一由
 * {@link cn.itcraft.jwsch.srv.router.PacketRouter} 承担（基于 TopicHash 的反向索引实现），
 * 本类保留暴露与 Subscription 同源的公益性能力，仅推荐测试环境使用，
 * 计划在后续版本按整体收敛目标进行移除。
 */
@Deprecated
public class WebSocketConnectionManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketConnectionManager.class);
    
    private static final WebSocketConnectionManager INSTANCE = new WebSocketConnectionManager();
    
    private static final long DEFAULT_INACTIVE_CHECK_INTERVAL_MS = 60000;
    private static final long DEFAULT_MAX_INACTIVE_TIME_MS = 300000;
    
    private final Map<Long, Channel> channelMap;
    private final List<Channel> channels;
    private final Map<String, List<Channel>> topicChannels;
    private final Map<Channel, Set<String>> channelTopicIndex;
    private final Map<Long, Long> lastActiveNanosMap;
    private final AtomicLong connectionIdCounter;
    
    private ScheduledExecutorService cleanupExecutor;
    private long inactiveCheckIntervalMs = DEFAULT_INACTIVE_CHECK_INTERVAL_MS;
    private long maxInactiveTimeMs = DEFAULT_MAX_INACTIVE_TIME_MS;
    private volatile boolean cleanupEnabled = false;
    
    private WebSocketConnectionManager() {
        this.channelMap = new ConcurrentHashMap<>();
        this.channels = new CopyOnWriteArrayList<>();
        this.topicChannels = new ConcurrentHashMap<>();
        this.channelTopicIndex = new ConcurrentHashMap<>();
        this.lastActiveNanosMap = new ConcurrentHashMap<>();
        this.connectionIdCounter = new AtomicLong(0);
    }
    
    /**
     * 获取 WebSocketConnectionManager 单例实例。
     *
     * @return 单例实例
     */
    public static WebSocketConnectionManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 添加 Channel 并生成新的连接 ID。
     *
     * @param channel WebSocket Channel
     * @return 分配的唯一连接 ID，如果 channel 为 null 则返回 -1
     */
    public long addChannel(Channel channel) {
        if (channel == null) {
            return -1;
        }
        
        long connectionId = connectionIdCounter.incrementAndGet();
        channelMap.put(connectionId, channel);
        channels.add(channel);
        lastActiveNanosMap.put(connectionId, System.nanoTime());
        
        LOGGER.debug("Added channel: connectionId={}, total={}", connectionId, channels.size());
        return connectionId;
    }
    
    /**
     * 使用指定的连接 ID 添加 Channel。
     *
     * @param connectionId 连接 ID
     * @param channel WebSocket Channel
     */
    public void addChannel(Long connectionId, Channel channel) {
        if (channel == null) {
            return;
        }
        
        channelMap.put(connectionId, channel);
        channels.add(channel);
        lastActiveNanosMap.put(connectionId, System.nanoTime());
        LOGGER.debug("Added channel: connectionId={}, total={}", connectionId, channels.size());
    }
    
    /**
     * 移除指定连接 ID 的 Channel。
     * <p>同时按连接反向索引从 Topic 订阅列表中移除该 Channel。
     *
     * @param connectionId 连接 ID
     */
    public void removeChannel(Long connectionId) {
        Channel channel = channelMap.remove(connectionId);
        if (channel != null) {
            channels.remove(channel);
            lastActiveNanosMap.remove(connectionId);
            
            Set<String> topics = channelTopicIndex.remove(channel);
            if (topics != null) {
                for (String topic : topics) {
                    List<Channel> topicChannelList = topicChannels.get(topic);
                    if (topicChannelList != null) {
                        topicChannelList.remove(channel);
                        if (topicChannelList.isEmpty()) {
                            topicChannels.remove(topic, topicChannelList);
                        }
                    }
                }
            }
            
            LOGGER.debug("Removed channel: connectionId={}, total={}", connectionId, channels.size());
        }
    }
    
    /**
     * 更新连接的活跃时间。
     *
     * @param connectionId 连接 ID
     */
    public void updateActiveTime(Long connectionId) {
        if (connectionId != null) {
            lastActiveNanosMap.put(connectionId, System.nanoTime());
        }
    }
    
    /**
     * 订阅 Topic（幂等，重复订阅不会重复接收消息）。
     *
     * @param connectionId 连接 ID
     * @param topic Topic 名称
     */
    public void subscribeTopic(Long connectionId, String topic) {
        Channel channel = channelMap.get(connectionId);
        if (channel == null || topic == null) {
            return;
        }
        
        List<Channel> topicChannelList =
            topicChannels.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>());
        if (!topicChannelList.contains(channel)) {
            topicChannelList.add(channel);
        }
        channelTopicIndex.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).add(topic);
        updateActiveTime(connectionId);
        LOGGER.debug("Subscribed topic: connectionId={}, topic={}", connectionId, topic);
    }
    
    /**
     * 取消订阅 Topic。
     *
     * @param connectionId 连接 ID
     * @param topic Topic 名称
     */
    public void unsubscribeTopic(Long connectionId, String topic) {
        Channel channel = channelMap.get(connectionId);
        if (channel == null || topic == null) {
            return;
        }
        
        List<Channel> topicChannelList = topicChannels.get(topic);
        if (topicChannelList != null) {
            topicChannelList.remove(channel);
            if (topicChannelList.isEmpty()) {
                topicChannels.remove(topic, topicChannelList);
            }
        }
        Set<String> topics = channelTopicIndex.get(channel);
        if (topics != null) {
            topics.remove(topic);
        }
        LOGGER.debug("Unsubscribed topic: connectionId={}, topic={}", connectionId, topic);
    }
    
    /**
     * 广播文本消息给所有活跃连接。
     *
     * @param message 文本消息
     */
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
    
    /**
     * 广播二进制数据给所有活跃连接。
     *
     * @param data 二进制数据
     */
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
    
    /**
     * 广播文本消息给订阅指定 Topic 的所有活跃连接。
     *
     * @param topic Topic 名称
     * @param message 文本消息
     */
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
    
    /**
     * 移除非活跃连接。
     * <p>检查所有连接，如果 Channel 不活跃或超过最大空闲时间，则关闭并移除。
     */
    public void removeInactiveChannels() {
        long now = System.nanoTime();
        List<Long> toRemove = new ArrayList<>();
        
        for (Map.Entry<Long, Long> entry : lastActiveNanosMap.entrySet()) {
            Long connectionId = entry.getKey();
            Long lastActive = entry.getValue();
            
            Channel channel = channelMap.get(connectionId);
            if (channel == null || !channel.isActive()) {
                toRemove.add(connectionId);
            } else if (lastActive != null && TimeUnit.NANOSECONDS.toMillis(now - lastActive) > maxInactiveTimeMs) {
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
    
    /**
     * 启动空闲连接检查定时任务。
     * <p>定时调用 {@link #removeInactiveChannels()} 清理空闲连接。
     */
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
    
    /**
     * 停止空闲连接检查定时任务。
     */
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
    
    /**
     * 获取连接统计信息。
     *
     * @return 连接统计信息
     */
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
    
    /**
     * 获取当前连接总数。
     *
     * @return 连接总数
     */
    public int getConnectionCount() {
        return channels.size();
    }
    
    /**
     * 获取所有 Channel 的不可修改列表。
     *
     * @return 所有 Channel 列表
     */
    public List<Channel> getAllChannels() {
        return Collections.unmodifiableList(new ArrayList<>(channels));
    }
    
    /**
     * 设置空闲连接检查间隔（毫秒）。
     *
     * @param inactiveCheckIntervalMs 检查间隔（毫秒）
     */
    public void setInactiveCheckIntervalMs(long inactiveCheckIntervalMs) {
        this.inactiveCheckIntervalMs = inactiveCheckIntervalMs;
    }
    
    /**
     * 设置最大空闲时间（毫秒）。
     * <p>超过此时间的连接将被视为空闲并关闭。
     *
     * @param maxInactiveTimeMs 最大空闲时间（毫秒）
     */
    public void setMaxInactiveTimeMs(long maxInactiveTimeMs) {
        this.maxInactiveTimeMs = maxInactiveTimeMs;
    }
    
    /**
     * 清空所有连接和订阅信息。
     */
    public void clear() {
        channelMap.clear();
        channels.clear();
        topicChannels.clear();
        channelTopicIndex.clear();
        lastActiveNanosMap.clear();
        stopInactiveCheck();
        LOGGER.info("Cleared all connections");
    }
    
    /**
     * 连接统计信息。
     */
    public static final class ConnectionStats {
        private final int total;
        private final int active;
        private final int inactive;
        private final int topicCount;
        
        /**
         * 创建连接统计信息。
         *
         * @param total 总连接数
         * @param active 活跃连接数
         * @param inactive 非活跃连接数
         * @param topicCount Topic 数量
         */
        public ConnectionStats(int total, int active, int inactive, int topicCount) {
            this.total = total;
            this.active = active;
            this.inactive = inactive;
            this.topicCount = topicCount;
        }
        
        /**
         * 获取总连接数。
         *
         * @return 总连接数
         */
        public int getTotal() {
            return total;
        }
        
        /**
         * 获取活跃连接数。
         *
         * @return 活跃连接数
         */
        public int getActive() {
            return active;
        }
        
        /**
         * 获取非活跃连接数。
         *
         * @return 非活跃连接数
         */
        public int getInactive() {
            return inactive;
        }
        
        /**
         * 获取 Topic 数量。
         *
         * @return Topic 数量
         */
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