package cn.itcraft.jwsch.srv.stats;

/**
 * Statistics data class for topic message counts.
 * 
 * <p>Immutable record of message counts for a specific topic, categorized by command type.
 * Used by {@link TopicMessageTracker} to track and report message statistics.
 * 
 * <p>Counts include:
 * <ul>
 *   <li>Total message count</li>
 *   <li>Request count</li>
 *   <li>Response count</li>
 *   <li>Push count</li>
 *   <li>Broadcast count</li>
 * </ul>
 * 
 * <p>Includes a timestamp ({@link #updateTime}) indicating when the statistics were last updated.
 */
public class TopicMessageStats {
    
    private final String topic;
    private final long messageCount;
    private final long requestCount;
    private final long responseCount;
    private final long pushCount;
    private final long broadcastCount;
    private final long updateTime;
    
    /**
     * Creates a new TopicMessageStats instance with the given counts.
     * 
     * @param topic the topic name
     * @param messageCount total number of messages for this topic
     * @param requestCount number of request messages
     * @param responseCount number of response messages
     * @param pushCount number of push messages
     * @param broadcastCount number of broadcast messages
     */
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
    
    /**
     * Returns the topic name.
     * 
     * @return topic name
     */
    public String getTopic() {
        return topic;
    }
    
    /**
     * Returns the total message count for this topic.
     * 
     * @return total message count
     */
    public long getMessageCount() {
        return messageCount;
    }
    
    /**
     * Returns the request message count for this topic.
     * 
     * @return request count
     */
    public long getRequestCount() {
        return requestCount;
    }
    
    /**
     * Returns the response message count for this topic.
     * 
     * @return response count
     */
    public long getResponseCount() {
        return responseCount;
    }
    
    /**
     * Returns the push message count for this topic.
     * 
     * @return push count
     */
    public long getPushCount() {
        return pushCount;
    }
    
    /**
     * Returns the broadcast message count for this topic.
     * 
     * @return broadcast count
     */
    public long getBroadcastCount() {
        return broadcastCount;
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