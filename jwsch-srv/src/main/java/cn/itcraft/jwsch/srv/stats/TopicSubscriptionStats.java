package cn.itcraft.jwsch.srv.stats;

/**
 * Statistics data class for topic subscription counts.
 * 
 * <p>Immutable record of subscription count for a specific topic.
 * Used by {@link TopicSubscriptionTracker} to track and report subscription statistics.
 * 
 * <p>Includes a timestamp ({@link #updateTime}) indicating when the statistics were last updated.
 */
public class TopicSubscriptionStats {
    
    private final String topic;
    private final int subscriptionCount;
    private final long updateTime;
    
    /**
     * Creates a new TopicSubscriptionStats instance with the given count.
     * 
     * @param topic the topic name
     * @param subscriptionCount number of active subscriptions for this topic
     */
    public TopicSubscriptionStats(String topic, int subscriptionCount) {
        this.topic = topic;
        this.subscriptionCount = subscriptionCount;
        this.updateTime = System.currentTimeMillis();
    }
    
    /**
     * Returns the topic name.
     * 
     * @return topic name
     */
    public String getTopic() {
        return topic;
    }
    
    /**
     * Returns the subscription count for this topic.
     * 
     * @return number of active subscriptions
     */
    public int getSubscriptionCount() {
        return subscriptionCount;
    }
    
    /**
     * Returns the timestamp when this statistics object was created.
     * <p>Value is System.currentTimeMillis() at construction time.
     * 
     * @return update timestamp in milliseconds
     */
    public long getUpdateTime() {
        return updateTime;
    }
    
    /**
     * Returns a string representation of this object for debugging.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        return "TopicSubscriptionStats{" +
            "topic='" + topic + '\'' +
            ", subscriptionCount=" + subscriptionCount +
            ", updateTime=" + updateTime +
            '}';
    }
}