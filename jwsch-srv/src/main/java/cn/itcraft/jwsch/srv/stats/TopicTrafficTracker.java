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
 */
public class TopicTrafficTracker {
    
    private final ConcurrentMap<String, LongAdder> totalBytes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> requestBytes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> responseBytes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> pushBytes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> broadcastBytes = new ConcurrentHashMap<>();
    
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
    
    private long getSum(ConcurrentMap<String, LongAdder> map, String topic) {
        LongAdder adder = map.get(topic);
        return adder != null ? adder.sum() : 0;
    }
    
    public int getTotalTopics() {
        return totalBytes.size();
    }
    
    public void clear() {
        totalBytes.clear();
        requestBytes.clear();
        responseBytes.clear();
        pushBytes.clear();
        broadcastBytes.clear();
    }
}