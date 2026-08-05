package cn.itcraft.jwsch.srv.stats;

import java.util.Collections;
import java.util.List;

/**
 * TopicStatsManager 空实现。
 * 
 * <p>所有方法均为空操作，用于不需要 Topic 统计的场景。
 * 
 * <p>This is a no-operation implementation of {@link TopicStatsManager} that performs no actual
 * statistics tracking. Useful for environments where statistics collection is disabled,
 * or as a null object pattern to avoid null checks.
 * 
 * <p>All methods are empty or return empty/default values:
 * <ul>
 *   <li>{@link #start()} and {@link #stop()} do nothing</li>
 *   <li>Recording methods ({@link #recordSubscribe}, {@link #recordUnsubscribe}, {@link #recordMessage}) are no-ops</li>
 *   <li>Query methods return empty lists or zero values</li>
 *   <li>Tracker getters return null</li>
 * </ul>
 * 
 * <p>Thread-safe: singleton instance can be shared across threads.
 */
public final class NoOpTopicStatsManager implements TopicStatsManager {
    
    /** Singleton instance of NoOpTopicStatsManager. */
    public static final NoOpTopicStatsManager INSTANCE = new NoOpTopicStatsManager();
    
    private NoOpTopicStatsManager() {
    }
    
    /**
     * No-op: does nothing.
     */
    @Override
    public void start() {
    }
    
    /**
     * No-op: does nothing.
     */
    @Override
    public void stop() {
    }
    
    /**
     * No-op: does nothing.
     * 
     * @param topic ignored
     */
    @Override
    public void recordSubscribe(String topic) {
    }
    
    /**
     * No-op: does nothing.
     * 
     * @param topic ignored
     */
    @Override
    public void recordUnsubscribe(String topic) {
    }
    
    /**
     * No-op: does nothing.
     * 
     * @param topic ignored
     * @param command ignored
     * @param bytes ignored
     */
    @Override
    public void recordMessage(String topic, byte command, int bytes) {
    }
    
    /**
     * Returns an empty list.
     * 
     * @return empty list
     */
    @Override
    public List<TopicSubscriptionStats> getTop10Subscriptions() {
        return Collections.emptyList();
    }
    
    /**
     * Returns an empty list.
     * 
     * @return empty list
     */
    @Override
    public List<TopicMessageStats> getTop10MessageCount() {
        return Collections.emptyList();
    }
    
    /**
     * Returns an empty list.
     * 
     * @return empty list
     */
    @Override
    public List<TopicTrafficStats> getTop10Traffic() {
        return Collections.emptyList();
    }
    
    /**
     * Returns zero.
     * 
     * @return 0
     */
    @Override
    public int getTotalTopics() {
        return 0;
    }
    
    /**
     * Returns null.
     * 
     * @return null
     */
    @Override
    public TopicSubscriptionTracker getSubscriptionTracker() {
        return null;
    }
    
    /**
     * Returns null.
     * 
     * @return null
     */
    @Override
    public TopicMessageTracker getMessageTracker() {
        return null;
    }
    
    /**
     * Returns null.
     * 
     * @return null
     */
    @Override
    public TopicTrafficTracker getTrafficTracker() {
        return null;
    }
    
    /**
     * No-op: does nothing.
     */
    @Override
    public void resetStats() {
    }
    
    /**
     * Returns an empty string.
     * 
     * @return empty string
     */
    @Override
    public String scrapeTop10Metrics() {
        return "";
    }
}