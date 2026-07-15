package cn.itcraft.jwsch.srv.stats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TopicSubscriptionTracker {
    
    private final ConcurrentMap<String, AtomicInteger> subscriptionCounts = new ConcurrentHashMap<>();
    
    public void subscribe(String topic) {
        subscriptionCounts.computeIfAbsent(topic.intern(), k -> new AtomicInteger(0))
            .incrementAndGet();
    }
    
    public void unsubscribe(String topic) {
        AtomicInteger counter = subscriptionCounts.get(topic);
        if (counter != null) {
            int value = counter.decrementAndGet();
            if (value <= 0) {
                subscriptionCounts.remove(topic, counter);
            }
        }
    }
    
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
    
    public int getSubscriptionCount(String topic) {
        AtomicInteger counter = subscriptionCounts.get(topic);
        return counter != null ? counter.get() : 0;
    }
    
    public int getTotalTopics() {
        return subscriptionCounts.size();
    }
    
    public void clear() {
        subscriptionCounts.clear();
    }
}