package cn.itcraft.jwsch.srv.flowcontrol;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Topic背压状态。
 *
 * <p>跟踪单个Topic的订阅者可写状态，用于per-topic背压隔离。
 */
final class TopicBackpressureState {
    
    private final long topicHash;
    private final Set<Long> nonWritableSubscribers;
    private final AtomicInteger totalSubscribers;
    
    private volatile boolean backpressured = false;
    private volatile long lastStateChangeTime = 0;
    
    private final double triggerThreshold;
    private final double releaseThreshold;
    
    TopicBackpressureState(long topicHash, double triggerThreshold, double releaseThreshold) {
        this.topicHash = topicHash;
        this.nonWritableSubscribers = ConcurrentHashMap.newKeySet();
        this.totalSubscribers = new AtomicInteger();
        this.triggerThreshold = triggerThreshold;
        this.releaseThreshold = releaseThreshold;
    }
    
    void incrementSubscribers() {
        totalSubscribers.incrementAndGet();
    }
    
    void decrementSubscribers() {
        totalSubscribers.decrementAndGet();
    }
    
    void updateSubscriberState(long connectionId, boolean writable) {
        if (writable) {
            nonWritableSubscribers.remove(connectionId);
            checkAndReleaseBackpressure();
        } else {
            nonWritableSubscribers.add(connectionId);
            checkAndActivateBackpressure();
        }
    }
    
    void removeSubscriber(long connectionId) {
        nonWritableSubscribers.remove(connectionId);
    }
    
    boolean isBackpressured() {
        return backpressured;
    }
    
    int getNonWritableCount() {
        return nonWritableSubscribers.size();
    }
    
    int getTotalSubscribers() {
        return totalSubscribers.get();
    }
    
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