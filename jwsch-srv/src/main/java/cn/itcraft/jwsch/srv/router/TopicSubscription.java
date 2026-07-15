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
    
    public TopicSubscription() {
        this.hashSubscribers = new ConcurrentHashMap<>();
        this.connectionTopics = new ConcurrentHashMap<>();
    }
    
    public void subscribe(String topic, long connectionId) {
        long topicHash = TopicHash.hash(topic);
        subscribeByHash(topicHash, connectionId);
        LOGGER.debug("Subscribed: topic={}, hash={}, connectionId={}", topic, topicHash, connectionId);
    }
    
    public void subscribeByHash(long topicHash, long connectionId) {
        hashSubscribers.computeIfAbsent(topicHash, k -> ConcurrentHashMap.newKeySet())
            .add(connectionId);
        
        connectionTopics.computeIfAbsent(connectionId, k -> ConcurrentHashMap.newKeySet())
            .add(topicHash);
    }
    
    public void unsubscribe(String topic, long connectionId) {
        long topicHash = TopicHash.hash(topic);
        unsubscribeByHash(topicHash, connectionId);
        LOGGER.debug("Unsubscribed: topic={}, hash={}, connectionId={}", topic, topicHash, connectionId);
    }
    
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
    
    public Set<Long> getSubscribers(String topic) {
        return getSubscribersByHash(TopicHash.hash(topic));
    }
    
    public Set<Long> getSubscribersByHash(long topicHash) {
        Set<Long> subscribers = hashSubscribers.get(topicHash);
        return subscribers != null ? Collections.unmodifiableSet(subscribers) : Collections.emptySet();
    }
    
    public int getSubscriberCount(String topic) {
        return getSubscriberCountByHash(TopicHash.hash(topic));
    }
    
    public int getSubscriberCountByHash(long topicHash) {
        Set<Long> subscribers = hashSubscribers.get(topicHash);
        return subscribers != null ? subscribers.size() : 0;
    }
    
    public boolean hasTopic(String topic) {
        return hasTopicHash(TopicHash.hash(topic));
    }
    
    public boolean hasTopicHash(long topicHash) {
        Set<Long> subscribers = hashSubscribers.get(topicHash);
        return subscribers != null && !subscribers.isEmpty();
    }
    
    public Set<Long> getTopicHashes() {
        return Collections.unmodifiableSet(hashSubscribers.keySet());
    }
    
    public Set<Long> getTopicHashesForConnection(long connectionId) {
        Set<Long> topics = connectionTopics.get(connectionId);
        return topics != null ? Collections.unmodifiableSet(topics) : Collections.emptySet();
    }
    
    public int getTopicCount() {
        return hashSubscribers.size();
    }
    
    public int getTotalSubscriptions() {
        int total = 0;
        for (Set<Long> subscribers : hashSubscribers.values()) {
            total += subscribers.size();
        }
        return total;
    }
    
    public void clear() {
        hashSubscribers.clear();
        connectionTopics.clear();
        LOGGER.info("TopicSubscription cleared");
    }
}
