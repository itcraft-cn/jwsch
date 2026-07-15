package cn.itcraft.jwsch.srv.stats;

public class TopicSubscriptionStats {
    
    private final String topic;
    private final int subscriptionCount;
    private final long updateTime;
    
    public TopicSubscriptionStats(String topic, int subscriptionCount) {
        this.topic = topic;
        this.subscriptionCount = subscriptionCount;
        this.updateTime = System.currentTimeMillis();
    }
    
    public String getTopic() {
        return topic;
    }
    
    public int getSubscriptionCount() {
        return subscriptionCount;
    }
    
    public long getUpdateTime() {
        return updateTime;
    }
    
    @Override
    public String toString() {
        return "TopicSubscriptionStats{" +
            "topic='" + topic + '\'' +
            ", subscriptionCount=" + subscriptionCount +
            ", updateTime=" + updateTime +
            '}';
    }
}