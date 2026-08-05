package cn.itcraft.jwsch.srv.stats;

import java.util.List;

/**
 * TopicStatsManager 接口。
 * 
 * <p>提供 Topic 统计功能，支持多态实现。
 * 
 * <p>Interface for topic-based statistics management in the JWSch server.
 * Tracks subscription counts, message counts, and traffic bytes per topic,
 * with support for retrieving top 10 topics in each category.
 * 
 * <p>Implementations:
 * <ul>
 *   <li>{@link DefaultTopicStatsManager} - production implementation with scheduled cleanup</li>
 *   <li>{@link NoOpTopicStatsManager} - no-operation implementation for disabled statistics</li>
 * </ul>
 */
public interface TopicStatsManager {
    
    /**
     * Starts the statistics manager.
     * <p>May start background tasks such as scheduled cleanup.
     */
    void start();
    
    /**
     * Stops the statistics manager.
     * <p>Should clean up resources and stop any background tasks.
     */
    void stop();
    
    /**
     * Records a new subscription to a topic.
     * 
     * @param topic the topic name
     */
    void recordSubscribe(String topic);
    
    /**
     * Records an unsubscribe event from a topic.
     * 
     * @param topic the topic name
     */
    void recordUnsubscribe(String topic);
    
    /**
     * Records a message sent to a topic.
     * 
     * @param topic the topic name
     * @param command the command byte indicating message type
     * @param bytes the size of the message in bytes
     */
    void recordMessage(String topic, byte command, int bytes);
    
    /**
     * Returns the top 10 topics by subscription count.
     * 
     * @return list of {@link TopicSubscriptionStats} sorted descending by subscription count
     */
    List<TopicSubscriptionStats> getTop10Subscriptions();
    
    /**
     * Returns the top 10 topics by message count.
     * 
     * @return list of {@link TopicMessageStats} sorted descending by total message count
     */
    List<TopicMessageStats> getTop10MessageCount();
    
    /**
     * Returns the top 10 topics by traffic bytes.
     * 
     * @return list of {@link TopicTrafficStats} sorted descending by total bytes
     */
    List<TopicTrafficStats> getTop10Traffic();
    
    /**
     * Returns the total number of unique topics being tracked.
     * 
     * @return total unique topics tracked
     */
    int getTotalTopics();
    
    /**
     * Returns the subscription tracker instance.
     * 
     * @return the subscription tracker, or null if not supported
     */
    TopicSubscriptionTracker getSubscriptionTracker();
    
    /**
     * Returns the message tracker instance.
     * 
     * @return the message tracker, or null if not supported
     */
    TopicMessageTracker getMessageTracker();
    
    /**
     * Returns the traffic tracker instance.
     * 
     * @return the traffic tracker, or null if not supported
     */
    TopicTrafficTracker getTrafficTracker();
    
    /**
     * Resets all statistics across all trackers.
     */
    void resetStats();
    
    /**
     * Generates Prometheus metrics for the top 10 topics in each category.
     * 
     * @return Prometheus metrics in text format
     */
    String scrapeTop10Metrics();
}