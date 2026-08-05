package cn.itcraft.jwsch.srv.flowcontrol;

import cn.itcraft.jwsch.common.flowcontrol.FlowControlConfig;
import cn.itcraft.jwsch.common.protocol.TopicHash;
import cn.itcraft.jwsch.srv.router.TopicSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Topic-level backpressure manager.
 *
 * <p>Implements per-topic backpressure isolation, ensuring a slow topic
 * doesn't affect other topics.
 * 
 * <p>How it works:
 * <pre>
 * When the proportion of non-writable subscribers for a topic exceeds threshold:
 * 1. Mark the topic as backpressured
 * 2. PacketRouter.broadcastToTopic() checks backpressure status
 * 3. Messages for backpressured topics are dropped, other topics deliver normally
 * </pre>
 *
 * <p>This is the second layer (L2) in the three-layer flow control system,
 * providing topic-level isolation to prevent a single slow topic from
 * affecting the entire system.
 */
public final class TopicBackpressureManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicBackpressureManager.class);
    
    private final ConcurrentHashMap<Long, TopicBackpressureState> topicStates;
    private final TopicSubscription topicSubscription;
    private final double triggerThreshold;
    private final double releaseThreshold;
    private final LongAdder backpressureDropCount;
    
    /**
     * Creates a TopicBackpressureManager with the specified configuration.
     *
     * @param topicSubscription the topic subscription manager
     * @param config the flow control configuration
     */
    public TopicBackpressureManager(TopicSubscription topicSubscription, FlowControlConfig config) {
        this.topicStates = new ConcurrentHashMap<>();
        this.topicSubscription = topicSubscription;
        this.triggerThreshold = config.getTopicTriggerThreshold();
        this.releaseThreshold = config.getTopicReleaseThreshold();
        this.backpressureDropCount = new LongAdder();
    }
    
    /**
     * Checks if a topic (by hash) is currently backpressured.
     *
     * @param topicHash the topic hash to check
     * @return true if the topic is backpressured, false otherwise
     */
    public boolean isTopicBackpressured(long topicHash) {
        TopicBackpressureState state = topicStates.get(topicHash);
        return state != null && state.isBackpressured();
    }
    
    /**
     * Checks if a topic (by name) is currently backpressured.
     *
     * @param topic the topic name to check
     * @return true if the topic is backpressured, false otherwise
     */
    public boolean isTopicBackpressured(String topic) {
        return isTopicBackpressured(hashTopic(topic));
    }
    
    /**
     * Updates subscriber writability state for a topic.
     *
     * @param topicHash the topic hash
     * @param connectionId the connection ID
     * @param writable true if the subscriber is writable, false otherwise
     */
    public void onSubscriberWritabilityChanged(long topicHash, long connectionId, boolean writable) {
        topicStates.compute(topicHash, (k, state) -> {
            if (state == null) {
                state = new TopicBackpressureState(topicHash, triggerThreshold, releaseThreshold);
            }
            state.updateSubscriberState(connectionId, writable);
            
            if (state.isBackpressured()) {
                LOGGER.info("Topic backpressure activated: topicHash={}, nonWritable={}/{}", 
                    topicHash, state.getNonWritableCount(), state.getTotalSubscribers());
            }
            
            return state;
        });
    }
    
    /**
     * Updates subscriber writability state for a topic.
     *
     * @param topic the topic name
     * @param connectionId the connection ID
     * @param writable true if the subscriber is writable, false otherwise
     */
    public void onSubscriberWritabilityChanged(String topic, long connectionId, boolean writable) {
        onSubscriberWritabilityChanged(hashTopic(topic), connectionId, writable);
    }
    
    /**
     * Increments the drop counter for a topic when a packet is dropped due to backpressure.
     *
     * @param topicHash the topic hash
     */
    public void incrementTopicDrop(long topicHash) {
        backpressureDropCount.increment();
        LOGGER.debug("Topic backpressure drop: topicHash={}", topicHash);
    }
    
    /**
     * Returns the total count of packets dropped due to topic backpressure.
     *
     * @return the total drop count
     */
    public long getBackpressureDropCount() {
        return backpressureDropCount.sum();
    }
    
    /**
     * Returns the number of topics being tracked.
     *
     * @return the count of topic states
     */
    public int getTopicStateCount() {
        return topicStates.size();
    }
    
    /**
     * Clears all topic states and resets the drop counter.
     */
    public void clear() {
        topicStates.clear();
        backpressureDropCount.reset();
    }
    
    private long hashTopic(String topic) {
        return TopicHash.hash(topic);
    }
}