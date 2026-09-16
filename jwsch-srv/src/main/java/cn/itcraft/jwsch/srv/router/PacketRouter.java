package cn.itcraft.jwsch.srv.router;

import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketWriter;
import cn.itcraft.jwsch.common.protocol.TopicHash;
import cn.itcraft.jwsch.srv.cluster.ClusterForwarder;
import cn.itcraft.jwsch.srv.flowcontrol.TopicBackpressureManager;
import cn.itcraft.jwsch.srv.loadbalance.LoadBalance;
import cn.itcraft.jwsch.srv.registry.ServiceInstance;
import cn.itcraft.jwsch.srv.registry.ServiceRegistry;
import cn.itcraft.jwsch.srv.stats.NoOpTopicStatsManager;
import cn.itcraft.jwsch.srv.stats.TopicStatsManager;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据包路由器。
 * 
 * <p>负责消息的路由和转发，核心功能包括：
 * <ul>
 *   <li>前端连接管理：WebSocket 客户端连接</li>
 *   <li>后端连接管理：TCP 后端服务连接</li>
 *   <li>Topic 订阅管理：基于 Topic 的消息推送</li>
 *   <li>消息广播：支持全量广播和 Topic 广播</li>
 *   <li>背压管理：当订阅者处理能力不足时，自动降低发布者速率</li>
 * </ul>
 * 
 * <p>广播流程：
 * <pre>
 * TCP Backend (PUSH) → PacketRouter.broadcastToTopic() → WebSocket Clients
 * </pre>
 * 
 * <p>背压机制：
 * <pre>
 * 当 WebSocket 通道不可写时 → 禁用 TCP 通道 AUTO_READ → 发布者降速
 * </pre>
 */
public class PacketRouter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketRouter.class);
    
    private final Map<Long, Channel> frontendConnections;
    private final Map<String, Channel> backendConnections;
    private final ServiceRegistry serviceRegistry;
    private final LoadBalance loadBalance;
    private final ResponseMapping responseMapping;
    private final TopicSubscription topicSubscription;
    private final BackpressureManager backpressureManager;
    private volatile TopicBackpressureManager topicBackpressureManager;
    private volatile TopicStatsManager topicStatsManager = NoOpTopicStatsManager.INSTANCE;
    private volatile ClusterForwarder clusterForwarder;
    
    /**
     * 构造函数。
     *
     * @param serviceRegistry 服务注册中心，不可为 null
     * @param loadBalance 负载均衡器，不可为 null
     */
    public PacketRouter(ServiceRegistry serviceRegistry, LoadBalance loadBalance) {
        this.frontendConnections = new ConcurrentHashMap<>();
        this.backendConnections = new ConcurrentHashMap<>();
        this.serviceRegistry = Objects.requireNonNull(serviceRegistry, "serviceRegistry cannot be null");
        this.loadBalance = Objects.requireNonNull(loadBalance, "loadBalance cannot be null");
        this.responseMapping = new ResponseMapping();
        this.topicSubscription = new TopicSubscription();
        this.backpressureManager = new BackpressureManager();
    }
    
    /**
     * 设置 Topic 统计管理器。
     *
     * @param topicStatsManager Topic 统计管理器，如果为 null 则使用无操作实现
     */
    public void setTopicStatsManager(TopicStatsManager topicStatsManager) {
        this.topicStatsManager = topicStatsManager != null ? topicStatsManager : NoOpTopicStatsManager.INSTANCE;
    }
    
    /**
     * 设置集群转发器。
     *
     * @param clusterForwarder 集群转发器
     */
    public void setClusterForwarder(ClusterForwarder clusterForwarder) {
        this.clusterForwarder = clusterForwarder;
    }
    
    /**
     * 设置 Topic 背压管理器。
     *
     * @param topicBackpressureManager Topic 背压管理器
     */
    public void setTopicBackpressureManager(TopicBackpressureManager topicBackpressureManager) {
        this.topicBackpressureManager = topicBackpressureManager;
    }
    
    /**
     * 获取 Topic 背压管理器。
     *
     * @return Topic 背压管理器
     */
    public TopicBackpressureManager getTopicBackpressureManager() {
        return topicBackpressureManager;
    }
    
    /**
     * 获取集群转发器。
     *
     * @return 集群转发器
     */
    public ClusterForwarder getClusterForwarder() {
        return clusterForwarder;
    }
    
    /**
     * 获取 Topic 统计管理器。
     *
     * @return Topic 统计管理器
     */
    public TopicStatsManager getTopicStatsManager() {
        return topicStatsManager;
    }
    
    /**
     * 路由数据包到合适的服务实例。
     *
     * <p>从数据包中提取服务名称，通过服务注册中心获取可用实例，
     * 使用负载均衡算法选择目标实例。
     *
     * @param packet 待路由的数据包
     * @return 选中的服务实例，如果没有可用的实例则返回 null
     */
    public ServiceInstance route(Packet packet) {
        String serviceName = extractServiceName(packet);
        
        if (serviceName == null || serviceName.isEmpty()) {
            LOGGER.warn("Cannot extract service name from packet");
            return null;
        }
        
        List<ServiceInstance> instances = serviceRegistry.getInstances(serviceName);
        
        if (instances.isEmpty()) {
            LOGGER.warn("No available instances for service: {}", serviceName);
            return null;
        }
        
        ServiceInstance selected = loadBalance.select(instances);
        
        if (selected != null) {
            LOGGER.debug("Routed packet to instance: {}", selected);
        }
        
        return selected;
    }
    
    /**
     * 添加前端连接（WebSocket 客户端）。
     *
     * @param connectionId 连接 ID
     * @param channel Netty 通道
     */
    public void addFrontendConnection(long connectionId, Channel channel) {
        frontendConnections.put(connectionId, channel);
        backpressureManager.registerFrontendChannel(channel);
        LOGGER.info("Frontend connection added: id={}", connectionId);
    }
    
    /**
     * 移除前端连接。
     *
     * <p>移除连接时会取消该连接的所有 Topic 订阅，
     * 并从背压管理器中注销通道。
     *
     * @param connectionId 连接 ID
     */
    public void removeFrontendConnection(long connectionId) {
        Channel removed = frontendConnections.remove(connectionId);
        if (removed != null) {
            topicSubscription.unsubscribeAll(connectionId);
            backpressureManager.unregisterFrontendChannel(removed);
            LOGGER.info("Frontend connection removed: id={}", connectionId);
        }
    }
    
    /**
     * 获取前端连接。
     *
     * @param connectionId 连接 ID
     * @return Netty 通道，如果连接不存在则返回 null
     */
    public Channel getFrontendConnection(long connectionId) {
        return frontendConnections.get(connectionId);
    }
    
    /**
     * 获取前端连接数量。
     *
     * @return 前端连接数量
     */
    public int getFrontendConnectionCount() {
        return frontendConnections.size();
    }
    
    /**
     * 获取前端连接映射。
     *
     * @return 连接 ID 到通道的映射
     */
    public Map<Long, Channel> getFrontendConnectionsMap() {
        return frontendConnections;
    }
    
    /**
     * 添加后端连接（TCP 服务端）。
     *
     * @param serviceName 服务名称
     * @param channel Netty 通道
     */
    public void addBackendConnection(String serviceName, Channel channel) {
        backendConnections.put(serviceName, channel);
        backpressureManager.registerTcpChannel(channel);
        LOGGER.info("Backend connection added: service={}", serviceName);
    }
    
    /**
     * 移除后端连接。
     *
     * @param serviceName 服务名称
     */
    public void removeBackendConnection(String serviceName) {
        Channel removed = backendConnections.remove(serviceName);
        if (removed != null) {
            backpressureManager.unregisterTcpChannel(removed);
            LOGGER.info("Backend connection removed: service={}", serviceName);
        }
    }
    
    /**
     * 获取后端连接。
     *
     * @param serviceName 服务名称
     * @return Netty 通道，如果连接不存在则返回 null
     */
    public Channel getBackendConnection(String serviceName) {
        return backendConnections.get(serviceName);
    }
    
    /**
     * 路由数据包到后端服务。
     *
     * <p>这是一个异步方法，返回 CompletableFuture 用于处理响应。
     * 方法会为请求生成唯一的请求 ID，并在响应映射中创建对应的 Future。
     *
     * @param packet 待路由的数据包
     * @return 响应数据的 Future
     */
    public CompletableFuture<Packet> routeToBackend(Packet packet) {
        String serviceName = extractServiceName(packet);
        if (serviceName == null) {
            CompletableFuture<Packet> failed = new CompletableFuture<>();
            failed.completeExceptionally(
                new IllegalArgumentException("Cannot extract service name from packet"));
            return failed;
        }
        
        List<ServiceInstance> instances = serviceRegistry.getInstances(serviceName);
        if (instances.isEmpty()) {
            CompletableFuture<Packet> failed = new CompletableFuture<>();
            failed.completeExceptionally(
                new IllegalStateException("No available instances for service: " + serviceName));
            return failed;
        }
        
        ServiceInstance selected = loadBalance.select(instances);
        
        long requestId = responseMapping.generateRequestId();
        CompletableFuture<Packet> future = responseMapping.createFuture(requestId);
        
        Channel channel = backendConnections.get(serviceName);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(packet);
        } else {
            responseMapping.completeExceptionally(requestId, 
                new IllegalStateException("No active connection to service: " + serviceName));
        }
        
        return future;
    }
    
    /**
     * 路由数据包到前端连接。
     *
     * <p>将数据包编码为 WebSocket 二进制帧，发送到指定的前端连接。
     *
     * @param packet 待路由的数据包
     */
    public void routeToFrontend(Packet packet) {
        long targetId = packet.getTargetId();
        Channel channel = frontendConnections.get(targetId);
        
        if (channel != null && channel.isActive()) {
            ByteBuf encoded = PacketWriter.write(packet, channel.alloc());
            channel.writeAndFlush(new BinaryWebSocketFrame(encoded));
        } else {
            LOGGER.warn("Frontend connection not found or inactive: id={}", targetId);
        }
    }
    
    /**
     * 广播数据包到所有前端连接。
     *
     * <p>使用零拷贝技术（retainedSlice）高效广播相同的消息到所有活跃连接。
     *
     * @param packet 待广播的数据包
     */
    public void broadcast(Packet packet) {
        List<Channel> activeChannels = new ArrayList<>();
        for (Channel channel : frontendConnections.values()) {
            if (channel.isActive()) {
                activeChannels.add(channel);
            }
        }
        
        if (activeChannels.isEmpty()) {
            return;
        }
        
        ByteBuf encoded = PacketWriter.writeToPooledDirectBuffer(packet, PooledByteBufAllocator.DEFAULT);
        
        try {
            for (Channel channel : activeChannels) {
                ByteBuf slice = encoded.retainedSlice();
                try {
                    channel.writeAndFlush(new BinaryWebSocketFrame(slice));
                } catch (Exception e) {
                    slice.release();
                    LOGGER.debug("Broadcast write failed: channel={}, error={}", channel, e.getMessage());
                }
            }
        } finally {
            encoded.release();
        }
    }
    
    /**
     * 广播消息到指定 Topic 的所有订阅者。
     * 
     * <p>优化策略：
     * <ul>
     *   <li>编码为池化 Direct ByteBuf，通过 retainedSlice() 零拷贝分发</li>
     *   <li>批量 write + 单次 flush，减少系统调用次数</li>
     * </ul>
     * 
     * <p>内存优化：
     * <pre>
     * 原方案：byte[] + Unpooled.wrappedBuffer() × N → N 个非池化 HeapByteBuf + byte[] GC
     * 新方案：Pooled Direct ByteBuf + retainedSlice() × N → N 个轻量 slice，共享底层内存，池化回收
     * </pre>
     * 
     * <p>系统调用优化：
     * <pre>
     * 原方案：writeAndFlush() × N → N 次 flush 系统调用
     * 新方案：write() × N + flush() × N → 每个通道一次 flush，减少上下文切换
     * </pre>
     * 
     * @param topic  目标 Topic
     * @param packet 待广播的数据包
     */
    public void broadcastToTopic(String topic, Packet packet) {
        if (topic == null) {
            return;
        }
        
        long topicHash = TopicHash.hash(topic);
        
        if (topicBackpressureManager != null && topicBackpressureManager.isTopicBackpressured(topicHash)) {
            topicBackpressureManager.incrementTopicDrop(topicHash);
            LOGGER.debug("Topic backpressure drop: topic={}", topic);
            return;
        }
        
        Set<Long> subscribers = topicSubscription.getSubscribers(topic);
        LOGGER.debug("Broadcasting to topic={}, subscribers={}", topic, subscribers.size());
        
        if (subscribers.isEmpty()) {
            return;
        }
        
        List<Channel> activeChannels = new ArrayList<>(subscribers.size());
        for (Long connectionId : subscribers) {
            Channel channel = frontendConnections.get(connectionId);
            if (channel != null && channel.isActive()) {
                activeChannels.add(channel);
            }
        }
        
        if (activeChannels.isEmpty()) {
            return;
        }
        
        ByteBuf encoded = PacketWriter.writeToPooledDirectBuffer(packet, PooledByteBufAllocator.DEFAULT);
        int encodedSize = encoded.readableBytes();
        
        try {
            for (Channel channel : activeChannels) {
                ByteBuf slice = encoded.retainedSlice();
                try {
                    channel.write(new BinaryWebSocketFrame(slice));
                } catch (Exception e) {
                    slice.release();
                    LOGGER.debug("Topic broadcast write failed: channel={}, error={}", channel, e.getMessage());
                }
            }
            
            for (Channel channel : activeChannels) {
                channel.flush();
            }
        } finally {
            encoded.release();
        }
        
        topicStatsManager.recordMessage(topic, packet.getHeader().getCommand(), encodedSize);
    }
    
    /**
     * Forward packet to cluster nodes.
     * 
     * <p>Handles cluster routing:
     * <ul>
     *   <li>REQUEST: Forward to target node (if target not local)</li>
     *   <li>PUSH: Broadcast to nodes that may have subscribers</li>
     *   <li>BROADCAST: Broadcast to all nodes</li>
     * </ul>
     * 
     * @param packet Packet to route to cluster
     */
    public void routeToCluster(Packet packet) {
        if (clusterForwarder == null) {
            return;
        }
        
        byte cmd = packet.getCommand();
        
        switch (cmd) {
            case Command.REQUEST:
                clusterForwarder.forwardRequest(packet);
                break;
                
            case Command.PUSH:
                clusterForwarder.broadcastPush(packet);
                break;
                
            case Command.BROADCAST:
                clusterForwarder.broadcastAll(packet);
                break;
                
            default:
                LOGGER.debug("No cluster routing for command: {}", cmd);
        }
    }
    
    /**
     * Check if cluster forwarding is enabled.
     * 
     * @return true if ClusterForwarder is set
     */
    public boolean isClusterEnabled() {
        return clusterForwarder != null;
    }
    
    /**
     * 处理订阅请求。
     *
     * @param topic Topic 名称
     * @param connectionId 连接 ID
     */
    public void handleSubscribe(String topic, long connectionId) {
        topicSubscription.subscribe(topic, connectionId);
        topicStatsManager.recordSubscribe(topic);
    }
    
    /**
     * 处理取消订阅请求。
     *
     * @param topic Topic 名称
     * @param connectionId 连接 ID
     */
    public void handleUnsubscribe(String topic, long connectionId) {
        topicSubscription.unsubscribe(topic, connectionId);
        topicStatsManager.recordUnsubscribe(topic);
    }
    
    /**
     * 获取 Topic 订阅管理器。
     *
     * @return Topic 订阅管理器
     */
    public TopicSubscription getTopicSubscription() {
        return topicSubscription;
    }
    
    /**
     * 获取背压管理器。
     *
     * @return 背压管理器
     */
    public BackpressureManager getBackpressureManager() {
        return backpressureManager;
    }
    
    /**
     * 获取响应映射管理器。
     *
     * @return 响应映射管理器
     */
    public ResponseMapping getResponseMapping() {
        return responseMapping;
    }
    
    private String extractServiceName(Packet packet) {
        if (packet == null || packet.getHeader() == null) {
            return null;
        }
        
        String topic = packet.getHeader().getTopic();
        if (topic == null || topic.isEmpty()) {
            return null;
        }
        
        if (topic.startsWith("/api/")) {
            String[] parts = topic.split("/");
            if (parts.length >= 3) {
                return parts[2];
            }
        }
        
        return topic;
    }
    
    /**
     * 关闭路由器。
     * 
     * <p>清理所有连接、订阅和背压状态。
     */
    public void shutdown() {
        responseMapping.shutdown();
        topicSubscription.clear();
        backpressureManager.clear();
        frontendConnections.clear();
        backendConnections.clear();
        LOGGER.info("PacketRouter shutdown");
    }
}