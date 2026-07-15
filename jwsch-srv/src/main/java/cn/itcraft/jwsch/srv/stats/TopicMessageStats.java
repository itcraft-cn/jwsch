package cn.itcraft.jwsch.srv.stats;

public class TopicMessageStats {
    
    private final String topic;
    private final long messageCount;
    private final long requestCount;
    private final long responseCount;
    private final long pushCount;
    private final long broadcastCount;
    private final long updateTime;
    
    public TopicMessageStats(String topic, long messageCount, long requestCount,
                             long responseCount, long pushCount, long broadcastCount) {
        this.topic = topic;
        this.messageCount = messageCount;
        this.requestCount = requestCount;
        this.responseCount = responseCount;
        this.pushCount = pushCount;
        this.broadcastCount = broadcastCount;
        this.updateTime = System.currentTimeMillis();
    }
    
    public String getTopic() {
        return topic;
    }
    
    public long getMessageCount() {
        return messageCount;
    }
    
    public long getRequestCount() {
        return requestCount;
    }
    
    public long getResponseCount() {
        return responseCount;
    }
    
    public long getPushCount() {
        return pushCount;
    }
    
    public long getBroadcastCount() {
        return broadcastCount;
    }
    
    public long getUpdateTime() {
        return updateTime;
    }
    
    @Override
    public String toString() {
        return "TopicMessageStats{" +
            "topic='" + topic + '\'' +
            ", messageCount=" + messageCount +
            ", requestCount=" + requestCount +
            ", responseCount=" + responseCount +
            ", pushCount=" + pushCount +
            ", broadcastCount=" + broadcastCount +
            '}';
    }
}