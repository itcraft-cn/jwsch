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
 * Topic级别背压管理器。
 *
 * <p>实现per-topic背压隔离，一个慢Topic不拖垮其他Topic。
 * 
 * <p>工作原理：
 * <pre>
 * 当某Topic的订阅者不可写比例超过阈值时：
 * 1. 标记该Topic为背压状态
 * 2. PacketRouter.broadcastToTopic() 检查背压状态
 * 3. 背压Topic的消息被丢弃，其他Topic正常投递
 * </pre>
 */
public final class TopicBackpressureManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicBackpressureManager.class);
    
    private final ConcurrentHashMap<Long, TopicBackpressureState> topicStates;
    private final TopicSubscription topicSubscription;
    private final double triggerThreshold;
    private final double releaseThreshold;
    private final LongAdder backpressureDropCount;
    
    public TopicBackpressureManager(TopicSubscription topicSubscription, FlowControlConfig config) {
        this.topicStates = new ConcurrentHashMap<>();
        this.topicSubscription = topicSubscription;
        this.triggerThreshold = config.getTopicTriggerThreshold();
        this.releaseThreshold = config.getTopicReleaseThreshold();
        this.backpressureDropCount = new LongAdder();
    }
    
    public boolean isTopicBackpressured(long topicHash) {
        TopicBackpressureState state = topicStates.get(topicHash);
        return state != null && state.isBackpressured();
    }
    
    public boolean isTopicBackpressured(String topic) {
        return isTopicBackpressured(hashTopic(topic));
    }
    
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
    
    public void onSubscriberWritabilityChanged(String topic, long connectionId, boolean writable) {
        onSubscriberWritabilityChanged(hashTopic(topic), connectionId, writable);
    }
    
    public void incrementTopicDrop(long topicHash) {
        backpressureDropCount.increment();
        LOGGER.debug("Topic backpressure drop: topicHash={}", topicHash);
    }
    
    public long getBackpressureDropCount() {
        return backpressureDropCount.sum();
    }
    
    public int getTopicStateCount() {
        return topicStates.size();
    }
    
    public void clear() {
        topicStates.clear();
        backpressureDropCount.reset();
    }
    
    private long hashTopic(String topic) {
        return TopicHash.hash(topic);
    }
}