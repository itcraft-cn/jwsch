package cn.itcraft.jwsch.srv.stats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Topic 流量追踪器。
 * 
 * <p>统计每个 Topic 的流量数据，按命令类型分类。
 * 
 * <p>This tracker maintains per-topic traffic bytes categorized by command type.
 * Uses {@link LongAdder} for thread-safe counting in high-concurrency environments.
 * 
 * <p>Bytes are stored in five separate maps:
 * <ul>
 *   <li>Total bytes</li>
 *   <li>Request bytes (REQUEST command)</li>
 *   <li>Response bytes (RESPONSE command)</li>
 *   <li>Push bytes (PUSH command)</li>
 *   <li>Broadcast bytes (BROADCAST command)</li>
 * </ul>
 * 
 * <p>Topic names are interned to reduce memory footprint when the same topic string
 * appears multiple times.
 */
public class TopicTrafficTracker {
    
    private final ConcurrentMap<String, LongAdder> totalBytes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> requestBytes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> responseBytes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> pushBytes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> broadcastBytes = new ConcurrentHashMap<>();
    
    /**
     * Records traffic bytes for the given topic and command.
     * 
     * @param topic the topic name
     * @param command the command byte (see {@link cn.itcraft.jwsch.common.protocol.Command})
     * @param bytes the number of bytes to record
     */
    public void record(String topic, byte command, int bytes) {
        String internedTopic = topic.intern();
        totalBytes.computeIfAbsent(internedTopic, k -> new LongAdder()).add(bytes);
        
        CommandType commandType = CommandType.fromCommand(command);
        switch (commandType) {
            case REQUEST:
                requestBytes.computeIfAbsent(internedTopic, k -> new LongAdder()).add(bytes);
                break;
            case RESPONSE:
                responseBytes.computeIfAbsent(internedTopic, k -> new LongAdder()).add(bytes);
                break;
            case PUSH:
                pushBytes.computeIfAbsent(internedTopic, k -> new LongAdder()).add(bytes);
                break;
            case BROADCAST:
                broadcastBytes.computeIfAbsent(internedTopic, k -> new LongAdder()).add(bytes);
                break;
            default:
                break;
        }
    }
    
    /**
     * Returns the top 10 topics by total traffic bytes.
     * <p>The list is sorted in descending order by total bytes.
     * 
     * @return list of {@link TopicTrafficStats} for top 10 topics
     */
    public List<TopicTrafficStats> getTop10() {
        List<TopicTrafficStats> stats = new ArrayList<>();
        
        totalBytes.forEach((topic, bytes) -> {
            stats.add(new TopicTrafficStats(
                topic,
                bytes.sum(),
                getSum(requestBytes, topic),
                getSum(responseBytes, topic),
                getSum(pushBytes, topic),
                getSum(broadcastBytes, topic)
            ));
        });
        
        stats.sort(Comparator.comparingLong(TopicTrafficStats::getTotalBytes).reversed());
        
        if (stats.size() > 10) {
            return stats.subList(0, 10);
        }
        return stats;
    }
    
    /**
     * Returns detailed statistics for a specific topic.
     * 
     * @param topic the topic name
     * @return {@link TopicTrafficStats} for the topic, or null if the topic has no recorded traffic
     */
    public TopicTrafficStats getStats(String topic) {
        LongAdder total = totalBytes.get(topic);
        if (total == null) {
            return null;
        }
        
        return new TopicTrafficStats(
            topic,
            total.sum(),
            getSum(requestBytes, topic),
            getSum(responseBytes, topic),
            getSum(pushBytes, topic),
            getSum(broadcastBytes, topic)
        );
    }
    
    /**
     * Helper method to safely get the sum from a LongAdder map.
     * 
     * @param map the map containing LongAdder counters
     * @param topic the topic key
     * @return the sum of the LongAdder, or 0 if no entry exists
     */
    private long getSum(ConcurrentMap<String, LongAdder> map, String topic) {
        LongAdder adder = map.get(topic);
        return adder != null ? adder.sum() : 0;
    }
    
    /**
     * Returns the total number of unique topics with recorded traffic.
     * 
     * @return number of unique topics
     */
    public int getTotalTopics() {
        return totalBytes.size();
    }
    
    /**
     * Clears all statistics, resetting all counters to zero.
     */
    public void clear() {
        totalBytes.clear();
        requestBytes.clear();
        responseBytes.clear();
        pushBytes.clear();
        broadcastBytes.clear();
    }
}