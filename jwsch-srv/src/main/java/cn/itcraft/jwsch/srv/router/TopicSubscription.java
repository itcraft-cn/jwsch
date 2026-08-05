package cn.itcraft.jwsch.srv.router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cn.itcraft.jwsch.common.protocol.TopicHash;

/**
 * Topic subscription manager using topic hash for efficient routing.
 * 
 * <p>Thread-safe implementation using {@link ConcurrentHashMap}.
 * Topics are hashed to 64-bit long values using xxHash64 for:
 * <ul>
 *   <li>Fast comparison in routing logic</li>
 *   <li>Compact memory storage</li>
 *   <li>Efficient BloomFilter operations</li>
 * </ul>
 * 
 * <p>Data structures:
 * <pre>
 * hashSubscribers = {
 *   topicHash1: [connectionId1, connectionId2, ...],
 *   topicHash2: [connectionId3, ...],
 *   ...
 * }
 * connectionTopics = {
 *   connectionId1: [topicHash1, topicHash2, ...],
 *   ...
 * }
 * </pre>
 */
public class TopicSubscription {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicSubscription.class);
    
    private final ConcurrentMap<Long, Set<Long>> hashSubscribers;
    
    private final ConcurrentMap<Long, Set<Long>> connectionTopics;
    
    /**
     * 默认构造函数。
     */
    public TopicSubscription() {
        this.hashSubscribers = new ConcurrentHashMap<>();
        this.connectionTopics = new ConcurrentHashMap<>();
    }
    
    /**
     * 订阅指定 Topic。
     *
     * @param topic Topic 名称
     * @param connectionId 连接 ID
     */
    public void subscribe(String topic, long connectionId) {
        long topicHash = TopicHash.hash(topic);
        subscribeByHash(topicHash, connectionId);
        LOGGER.debug("Subscribed: topic={}, hash={}, connectionId={}", topic, topicHash, connectionId);
    }
    
    /**
     * 通过 Topic Hash 订阅。
     *
     * @param topicHash Topic 的哈希值
     * @param connectionId 连接 ID
     */
    public void subscribeByHash(long topicHash, long connectionId) {
        hashSubscribers.computeIfAbsent(topicHash, k -> ConcurrentHashMap.newKeySet())
            .add(connectionId);
        
        connectionTopics.computeIfAbsent(connectionId, k -> ConcurrentHashMap.newKeySet())
            .add(topicHash);
    }
    
    /**
     * 取消订阅指定 Topic。
     *
     * @param topic Topic 名称
     * @param connectionId 连接 ID
     */
    public void unsubscribe(String topic, long connectionId) {
        long topicHash = TopicHash.hash(topic);
        unsubscribeByHash(topicHash, connectionId);
        LOGGER.debug("Unsubscribed: topic={}, hash={}, connectionId={}", topic, topicHash, connectionId);
    }
    
    /**
     * 通过 Topic Hash 取消订阅。
     *
     * @param topicHash Topic 的哈希值
     * @param connectionId 连接 ID
     */
    public void unsubscribeByHash(long topicHash, long connectionId) {
        Set<Long> subscribers = hashSubscribers.get(topicHash);
        if (subscribers != null) {
            subscribers.remove(connectionId);
            if (subscribers.isEmpty()) {
                hashSubscribers.remove(topicHash);
            }
        }
        
        Set<Long> topics = connectionTopics.get(connectionId);
        if (topics != null) {
            topics.remove(topicHash);
            if (topics.isEmpty()) {
                connectionTopics.remove(connectionId);
            }
        }
    }
    
    /**
     * 取消指定连接的所有订阅。
     *
     * @param connectionId 连接 ID
     */
    public void unsubscribeAll(long connectionId) {
        Set<Long> topics = connectionTopics.remove(connectionId);
        if (topics != null) {
            for (Long topicHash : topics) {
                Set<Long> subscribers = hashSubscribers.get(topicHash);
                if (subscribers != null) {
                    subscribers.remove(connectionId);
                    if (subscribers.isEmpty()) {
                        hashSubscribers.remove(topicHash);
                    }
                }
            }
        }
    }
    
    /**
     * 获取指定 Topic 的订阅者列表。
     *
     * @param topic Topic 名称
     * @return 订阅者连接 ID 集合（不可修改）
     */
    public Set<Long> getSubscribers(String topic) {
        return getSubscribersByHash(TopicHash.hash(topic));
    }
    
    /**
     * 通过 Topic Hash 获取订阅者列表。
     *
     * @param topicHash Topic 的哈希值
     * @return 订阅者连接 ID 集合（不可修改）
     */
    public Set<Long> getSubscribersByHash(long topicHash) {
        Set<Long> subscribers = hashSubscribers.get(topicHash);
        return subscribers != null ? Collections.unmodifiableSet(subscribers) : Collections.emptySet();
    }
    
    /**
     * 获取指定 Topic 的订阅者数量。
     *
     * @param topic Topic 名称
     * @return 订阅者数量
     */
    public int getSubscriberCount(String topic) {
        return getSubscriberCountByHash(TopicHash.hash(topic));
    }
    
    /**
     * 通过 Topic Hash 获取订阅者数量。
     *
     * @param topicHash Topic 的哈希值
     * @return 订阅者数量
     */
    public int getSubscriberCountByHash(long topicHash) {
        Set<Long> subscribers = hashSubscribers.get(topicHash);
        return subscribers != null ? subscribers.size() : 0;
    }
    
    /**
     * 检查是否存在指定的 Topic。
     *
     * @param topic Topic 名称
     * @return 是否存在订阅者
     */
    public boolean hasTopic(String topic) {
        return hasTopicHash(TopicHash.hash(topic));
    }
    
    /**
     * 通过 Topic Hash 检查是否存在订阅者。
     *
     * @param topicHash Topic 的哈希值
     * @return 是否存在订阅者
     */
    public boolean hasTopicHash(long topicHash) {
        Set<Long> subscribers = hashSubscribers.get(topicHash);
        return subscribers != null && !subscribers.isEmpty();
    }
    
    /**
     * 获取所有有订阅者的 Topic Hash 集合。
     *
     * @return Topic Hash 集合（不可修改）
     */
    public Set<Long> getTopicHashes() {
        return Collections.unmodifiableSet(hashSubscribers.keySet());
    }
    
    /**
     * 获取指定连接订阅的所有 Topic Hash。
     *
     * @param connectionId 连接 ID
     * @return 该连接订阅的 Topic Hash 集合（不可修改）
     */
    public Set<Long> getTopicHashesForConnection(long connectionId) {
        Set<Long> topics = connectionTopics.get(connectionId);
        return topics != null ? Collections.unmodifiableSet(topics) : Collections.emptySet();
    }
    
    /**
     * 获取有订阅者的 Topic 数量。
     *
     * @return Topic 数量
     */
    public int getTopicCount() {
        return hashSubscribers.size();
    }
    
    /**
     * 获取总订阅数（所有 Topic 的订阅者总数）。
     *
     * @return 总订阅数
     */
    public int getTotalSubscriptions() {
        int total = 0;
        for (Set<Long> subscribers : hashSubscribers.values()) {
            total += subscribers.size();
        }
        return total;
    }
    
    /**
     * 清空所有订阅数据。
     */
    public void clear() {
        hashSubscribers.clear();
        connectionTopics.clear();
        LOGGER.info("TopicSubscription cleared");
    }
}
