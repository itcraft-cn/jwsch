package cn.itcraft.jwsch.srv.stats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Topic 消息追踪器。
 * 
 * <p>统计每个 Topic 的消息数量，按命令类型分类。
 */
public class TopicMessageTracker {
    
    private final ConcurrentMap<String, LongAdder> messageCounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> responseCounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> pushCounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> broadcastCounts = new ConcurrentHashMap<>();
    
    public void record(String topic, byte command) {
        String internedTopic = topic.intern();
        messageCounts.computeIfAbsent(internedTopic, k -> new LongAdder()).increment();
        
        CommandType commandType = CommandType.fromCommand(command);
        switch (commandType) {
            case REQUEST:
                requestCounts.computeIfAbsent(internedTopic, k -> new LongAdder()).increment();
                break;
            case RESPONSE:
                responseCounts.computeIfAbsent(internedTopic, k -> new LongAdder()).increment();
                break;
            case PUSH:
                pushCounts.computeIfAbsent(internedTopic, k -> new LongAdder()).increment();
                break;
            case BROADCAST:
                broadcastCounts.computeIfAbsent(internedTopic, k -> new LongAdder()).increment();
                break;
            default:
                break;
        }
    }
    
    public List<TopicMessageStats> getTop10() {
        List<TopicMessageStats> stats = new ArrayList<>();
        
        messageCounts.forEach((topic, count) -> {
            stats.add(new TopicMessageStats(
                topic,
                count.sum(),
                getSum(requestCounts, topic),
                getSum(responseCounts, topic),
                getSum(pushCounts, topic),
                getSum(broadcastCounts, topic)
            ));
        });
        
        stats.sort(Comparator.comparingLong(TopicMessageStats::getMessageCount).reversed());
        
        if (stats.size() > 10) {
            return stats.subList(0, 10);
        }
        return stats;
    }
    
    public TopicMessageStats getStats(String topic) {
        LongAdder messageCount = messageCounts.get(topic);
        if (messageCount == null) {
            return null;
        }
        
        return new TopicMessageStats(
            topic,
            messageCount.sum(),
            getSum(requestCounts, topic),
            getSum(responseCounts, topic),
            getSum(pushCounts, topic),
            getSum(broadcastCounts, topic)
        );
    }
    
    private long getSum(ConcurrentMap<String, LongAdder> map, String topic) {
        LongAdder adder = map.get(topic);
        return adder != null ? adder.sum() : 0;
    }
    
    public int getTotalTopics() {
        return messageCounts.size();
    }
    
    public void clear() {
        messageCounts.clear();
        requestCounts.clear();
        responseCounts.clear();
        pushCounts.clear();
        broadcastCounts.clear();
    }
}