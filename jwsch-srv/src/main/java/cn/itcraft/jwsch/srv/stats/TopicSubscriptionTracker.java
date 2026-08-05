package cn.itcraft.jwsch.srv.stats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks subscription counts per topic.
 * 
 * <p>Maintains a thread-safe map of topic to active subscription count.
 * Uses {@link AtomicInteger} for thread-safe increment/decrement operations.
 * 
 * <p>Features:
 * <ul>
 *   <li>Increment count on subscribe</li>
 *   <li>Decrement count on unsubscribe</li>
 *   <li>Automatic removal when count reaches zero or below</li>
 *   <li>Top 10 topics by subscription count</li>
 *   <li>Total unique topics count</li>
 * </ul>
 * 
 * <p>Topic names are interned to reduce memory footprint when the same topic string
 * appears multiple times.
 */
public class TopicSubscriptionTracker {
    
    private final ConcurrentMap<String, AtomicInteger> subscriptionCounts = new ConcurrentHashMap<>();
    
    /**
     * Increments the subscription count for the given topic.
     * <p>If the topic is not yet tracked, initializes the count to 1.
     * 
     * @param topic the topic name
     */
    public void subscribe(String topic) {
        subscriptionCounts.computeIfAbsent(topic.intern(), k -> new AtomicInteger(0))
            .incrementAndGet();
    }
    
    /**
     * Decrements the subscription count for the given topic.
     * <p>If the count reaches zero or below, removes the topic from tracking.
     * 
     * @param topic the topic name
     */
    public void unsubscribe(String topic) {
        AtomicInteger counter = subscriptionCounts.get(topic);
        if (counter != null) {
            int value = counter.decrementAndGet();
            if (value <= 0) {
                subscriptionCounts.remove(topic, counter);
            }
        }
    }
    
    /**
     * Returns the top 10 topics by subscription count.
     * <p>The list is sorted in descending order by subscription count.
     * Only topics with positive subscription counts are included.
     * 
     * @return list of {@link TopicSubscriptionStats} for top 10 topics
     */
    public List<TopicSubscriptionStats> getTop10() {
        List<TopicSubscriptionStats> stats = new ArrayList<>();
        
        subscriptionCounts.forEach((topic, counter) -> {
            int count = counter.get();
            if (count > 0) {
                stats.add(new TopicSubscriptionStats(topic, count));
            }
        });
        
        stats.sort(Comparator.comparingInt(TopicSubscriptionStats::getSubscriptionCount).reversed());
        
        if (stats.size() > 10) {
            return stats.subList(0, 10);
        }
        return stats;
    }
    
    /**
     * Returns the subscription count for a specific topic.
     * 
     * @param topic the topic name
     * @return subscription count, or 0 if the topic is not tracked
     */
    public int getSubscriptionCount(String topic) {
        AtomicInteger counter = subscriptionCounts.get(topic);
        return counter != null ? counter.get() : 0;
    }
    
    /**
     * Returns the total number of unique topics being tracked.
     * <p>Note: this includes topics with zero or negative counts (should be rare).
     * 
     * @return number of unique topics in the map
     */
    public int getTotalTopics() {
        return subscriptionCounts.size();
    }
    
    /**
     * Clears all statistics, removing all tracked topics.
     */
    public void clear() {
        subscriptionCounts.clear();
    }
}