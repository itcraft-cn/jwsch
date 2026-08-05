package cn.itcraft.jwsch.srv.flowcontrol;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Topic backpressure state.
 *
 * <p>Tracks writability state of subscribers for a single topic,
 * used for per-topic backpressure isolation.
 *
 * <p>Maintains counts of non-writable subscribers and triggers
 * backpressure when the proportion exceeds the configured threshold.
 */
final class TopicBackpressureState {
    
    private final long topicHash;
    private final Set<Long> nonWritableSubscribers;
    private final AtomicInteger totalSubscribers;
    
    private volatile boolean backpressured = false;
    private volatile long lastStateChangeTime = 0;
    
    private final double triggerThreshold;
    private final double releaseThreshold;
    
    /**
     * Creates a TopicBackpressureState for the specified topic.
     *
     * @param topicHash the topic hash
     * @param triggerThreshold the threshold to activate backpressure (0.0-1.0)
     * @param releaseThreshold the threshold to release backpressure (0.0-1.0)
     */
    TopicBackpressureState(long topicHash, double triggerThreshold, double releaseThreshold) {
        this.topicHash = topicHash;
        this.nonWritableSubscribers = ConcurrentHashMap.newKeySet();
        this.totalSubscribers = new AtomicInteger();
        this.triggerThreshold = triggerThreshold;
        this.releaseThreshold = releaseThreshold;
    }
    
    /**
     * Increments the total subscriber count.
     */
    void incrementSubscribers() {
        totalSubscribers.incrementAndGet();
    }
    
    /**
     * Decrements the total subscriber count.
     */
    void decrementSubscribers() {
        totalSubscribers.decrementAndGet();
    }
    
    /**
     * Updates the writability state for a subscriber.
     *
     * @param connectionId the connection ID
     * @param writable true if the subscriber is writable, false otherwise
     */
    void updateSubscriberState(long connectionId, boolean writable) {
        if (writable) {
            nonWritableSubscribers.remove(connectionId);
            checkAndReleaseBackpressure();
        } else {
            nonWritableSubscribers.add(connectionId);
            checkAndActivateBackpressure();
        }
    }
    
    /**
     * Removes a subscriber from tracking.
     *
     * @param connectionId the connection ID
     */
    void removeSubscriber(long connectionId) {
        nonWritableSubscribers.remove(connectionId);
    }
    
    /**
     * Returns whether the topic is currently backpressured.
     *
     * @return true if backpressure is active, false otherwise
     */
    boolean isBackpressured() {
        return backpressured;
    }
    
    /**
     * Returns the count of non-writable subscribers.
     *
     * @return the number of non-writable subscribers
     */
    int getNonWritableCount() {
        return nonWritableSubscribers.size();
    }
    
    /**
     * Returns the total subscriber count.
     *
     * @return the total number of subscribers
     */
    int getTotalSubscribers() {
        return totalSubscribers.get();
    }
    
    /**
     * Returns the topic hash.
     *
     * @return the topic hash
     */
    long getTopicHash() {
        return topicHash;
    }
    
    private void checkAndActivateBackpressure() {
        int total = totalSubscribers.get();
        if (total == 0) {
            return;
        }
        
        double ratio = (double) nonWritableSubscribers.size() / total;
        
        if (ratio >= triggerThreshold && !backpressured) {
            backpressured = true;
            lastStateChangeTime = System.currentTimeMillis();
        }
    }
    
    private void checkAndReleaseBackpressure() {
        int total = totalSubscribers.get();
        if (total == 0) {
            backpressured = false;
            return;
        }
        
        double ratio = (double) nonWritableSubscribers.size() / total;
        
        if (ratio <= releaseThreshold && backpressured) {
            backpressured = false;
            lastStateChangeTime = System.currentTimeMillis();
        }
    }
}