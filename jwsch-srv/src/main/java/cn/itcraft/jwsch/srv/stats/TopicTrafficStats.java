package cn.itcraft.jwsch.srv.stats;

public class TopicTrafficStats {
    
    private final String topic;
    private final long totalBytes;
    private final long requestBytes;
    private final long responseBytes;
    private final long pushBytes;
    private final long broadcastBytes;
    private final long updateTime;
    
    public TopicTrafficStats(String topic, long totalBytes, long requestBytes,
                             long responseBytes, long pushBytes, long broadcastBytes) {
        this.topic = topic;
        this.totalBytes = totalBytes;
        this.requestBytes = requestBytes;
        this.responseBytes = responseBytes;
        this.pushBytes = pushBytes;
        this.broadcastBytes = broadcastBytes;
        this.updateTime = System.currentTimeMillis();
    }
    
    public String getTopic() {
        return topic;
    }
    
    public long getTotalBytes() {
        return totalBytes;
    }
    
    public long getRequestBytes() {
        return requestBytes;
    }
    
    public long getResponseBytes() {
        return responseBytes;
    }
    
    public long getPushBytes() {
        return pushBytes;
    }
    
    public long getBroadcastBytes() {
        return broadcastBytes;
    }
    
    public long getUpdateTime() {
        return updateTime;
    }
    
    @Override
    public String toString() {
        return "TopicTrafficStats{" +
            "topic='" + topic + '\'' +
            ", totalBytes=" + totalBytes +
            ", requestBytes=" + requestBytes +
            ", responseBytes=" + responseBytes +
            ", pushBytes=" + pushBytes +
            ", broadcastBytes=" + broadcastBytes +
            '}';
    }
}