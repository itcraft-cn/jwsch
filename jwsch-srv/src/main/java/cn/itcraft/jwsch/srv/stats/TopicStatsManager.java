package cn.itcraft.jwsch.srv.stats;

import java.util.List;

/**
 * TopicStatsManager 接口。
 * 
 * <p>提供 Topic 统计功能，支持多态实现。
 */
public interface TopicStatsManager {
    
    void start();
    
    void stop();
    
    void recordSubscribe(String topic);
    
    void recordUnsubscribe(String topic);
    
    void recordMessage(String topic, byte command, int bytes);
    
    List<TopicSubscriptionStats> getTop10Subscriptions();
    
    List<TopicMessageStats> getTop10MessageCount();
    
    List<TopicTrafficStats> getTop10Traffic();
    
    int getTotalTopics();
    
    TopicSubscriptionTracker getSubscriptionTracker();
    
    TopicMessageTracker getMessageTracker();
    
    TopicTrafficTracker getTrafficTracker();
    
    void resetStats();
    
    String scrapeTop10Metrics();
}