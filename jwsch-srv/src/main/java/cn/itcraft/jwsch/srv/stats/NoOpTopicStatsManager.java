package cn.itcraft.jwsch.srv.stats;

import java.util.Collections;
import java.util.List;

/**
 * TopicStatsManager 空实现。
 * 
 * <p>所有方法均为空操作，用于不需要 Topic 统计的场景。
 */
public final class NoOpTopicStatsManager implements TopicStatsManager {
    
    public static final NoOpTopicStatsManager INSTANCE = new NoOpTopicStatsManager();
    
    private NoOpTopicStatsManager() {
    }
    
    @Override
    public void start() {
    }
    
    @Override
    public void stop() {
    }
    
    @Override
    public void recordSubscribe(String topic) {
    }
    
    @Override
    public void recordUnsubscribe(String topic) {
    }
    
    @Override
    public void recordMessage(String topic, byte command, int bytes) {
    }
    
    @Override
    public List<TopicSubscriptionStats> getTop10Subscriptions() {
        return Collections.emptyList();
    }
    
    @Override
    public List<TopicMessageStats> getTop10MessageCount() {
        return Collections.emptyList();
    }
    
    @Override
    public List<TopicTrafficStats> getTop10Traffic() {
        return Collections.emptyList();
    }
    
    @Override
    public int getTotalTopics() {
        return 0;
    }
    
    @Override
    public TopicSubscriptionTracker getSubscriptionTracker() {
        return null;
    }
    
    @Override
    public TopicMessageTracker getMessageTracker() {
        return null;
    }
    
    @Override
    public TopicTrafficTracker getTrafficTracker() {
        return null;
    }
    
    @Override
    public void resetStats() {
    }
    
    @Override
    public String scrapeTop10Metrics() {
        return "";
    }
}