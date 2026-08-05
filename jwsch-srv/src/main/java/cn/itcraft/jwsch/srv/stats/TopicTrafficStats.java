package cn.itcraft.jwsch.srv.stats;

/**
 * Statistics data class for topic traffic bytes.
 * 
 * <p>Immutable record of traffic bytes for a specific topic, categorized by command type.
 * Used by {@link TopicTrafficTracker} to track and report traffic statistics.
 * 
 * <p>Bytes include:
 * <ul>
 *   <li>Total bytes</li>
 *   <li>Request bytes</li>
 *   <li>Response bytes</li>
 *   <li>Push bytes</li>
 *   <li>Broadcast bytes</li>
 * </ul>
 * 
 * <p>Includes a timestamp ({@link #updateTime}) indicating when the statistics were last updated.
 */
public class TopicTrafficStats {
    
    private final String topic;
    private final long totalBytes;
    private final long requestBytes;
    private final long responseBytes;
    private final long pushBytes;
    private final long broadcastBytes;
    private final long updateTime;
    
    /**
     * Creates a new TopicTrafficStats instance with the given byte counts.
     * 
     * @param topic the topic name
     * @param totalBytes total number of bytes for this topic
     * @param requestBytes number of request bytes
     * @param responseBytes number of response bytes
     * @param pushBytes number of push bytes
     * @param broadcastBytes number of broadcast bytes
     */
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
    
    /**
     * Returns the topic name.
     * 
     * @return topic name
     */
    public String getTopic() {
        return topic;
    }
    
    /**
     * Returns the total bytes for this topic.
     * 
     * @return total bytes
     */
    public long getTotalBytes() {
        return totalBytes;
    }
    
    /**
     * Returns the request bytes for this topic.
     * 
     * @return request bytes
     */
    public long getRequestBytes() {
        return requestBytes;
    }
    
    /**
     * Returns the response bytes for this topic.
     * 
     * @return response bytes
     */
    public long getResponseBytes() {
        return responseBytes;
    }
    
    /**
     * Returns the push bytes for this topic.
     * 
     * @return push bytes
     */
    public long getPushBytes() {
        return pushBytes;
    }
    
    /**
     * Returns the broadcast bytes for this topic.
     * 
     * @return broadcast bytes
     */
    public long getBroadcastBytes() {
        return broadcastBytes;
    }
    
    /**
     * Returns the timestamp when this statistics object was created.
     * <p>Value is System.currentTimeMillis() at construction time.
     * 
     * @return update timestamp in milliseconds
     */
    public long getUpdateTime() {
        return updateTime;
    }
    
    /**
     * Returns a string representation of this object for debugging.
     * 
     * @return string representation
     */
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