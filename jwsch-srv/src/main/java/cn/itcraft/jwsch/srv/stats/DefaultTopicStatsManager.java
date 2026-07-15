package cn.itcraft.jwsch.srv.stats;

import cn.itcraft.jwsch.common.protocol.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultTopicStatsManager implements TopicStatsManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTopicStatsManager.class);
    
    private static final int DEFAULT_MAX_TOPICS = 10000;
    private static final long DEFAULT_CLEANUP_INTERVAL = 3600L;
    
    private final TopicSubscriptionTracker subscriptionTracker;
    private final TopicMessageTracker messageTracker;
    private final TopicTrafficTracker trafficTracker;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean started = new AtomicBoolean(false);
    
    private final int maxTopics;
    private final long cleanupIntervalSeconds;
    
    public DefaultTopicStatsManager() {
        this(DEFAULT_MAX_TOPICS, DEFAULT_CLEANUP_INTERVAL);
    }
    
    public DefaultTopicStatsManager(int maxTopics, long cleanupIntervalSeconds) {
        this.maxTopics = maxTopics > 0 ? maxTopics : DEFAULT_MAX_TOPICS;
        this.cleanupIntervalSeconds = cleanupIntervalSeconds > 0 ? cleanupIntervalSeconds : DEFAULT_CLEANUP_INTERVAL;
        
        this.subscriptionTracker = new TopicSubscriptionTracker();
        this.messageTracker = new TopicMessageTracker();
        this.trafficTracker = new TopicTrafficTracker();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "topic-stats-cleaner");
            t.setDaemon(true);
            return t;
        });
    }
    
    public void start() {
        if (started.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(
                this::checkTopicLimit,
                cleanupIntervalSeconds,
                cleanupIntervalSeconds,
                TimeUnit.SECONDS
            );
            LOGGER.info("TopicStatsManager started with maxTopics={}, cleanupInterval={}s", 
                maxTopics, cleanupIntervalSeconds);
        }
    }
    
    public void stop() {
        if (started.compareAndSet(true, false)) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            LOGGER.info("TopicStatsManager stopped");
        }
    }
    
    public void recordSubscribe(String topic) {
        subscriptionTracker.subscribe(topic);
    }
    
    public void recordUnsubscribe(String topic) {
        subscriptionTracker.unsubscribe(topic);
    }
    
    public void recordMessage(String topic, byte command, int bytes) {
        messageTracker.record(topic, command);
        trafficTracker.record(topic, command, bytes);
    }
    
    public List<TopicSubscriptionStats> getTop10Subscriptions() {
        return subscriptionTracker.getTop10();
    }
    
    public List<TopicMessageStats> getTop10MessageCount() {
        return messageTracker.getTop10();
    }
    
    public List<TopicTrafficStats> getTop10Traffic() {
        return trafficTracker.getTop10();
    }
    
    public int getTotalTopics() {
        return Math.max(
            subscriptionTracker.getTotalTopics(),
            Math.max(messageTracker.getTotalTopics(), trafficTracker.getTotalTopics())
        );
    }
    
    public TopicSubscriptionTracker getSubscriptionTracker() {
        return subscriptionTracker;
    }
    
    public TopicMessageTracker getMessageTracker() {
        return messageTracker;
    }
    
    public TopicTrafficTracker getTrafficTracker() {
        return trafficTracker;
    }
    
    public void resetStats() {
        subscriptionTracker.clear();
        messageTracker.clear();
        trafficTracker.clear();
        LOGGER.info("Topic stats reset");
    }
    
    /**
     * 生成 Top 10 Topic 的 Prometheus 格式指标。
     */
    public String scrapeTop10Metrics() {
        StringBuilder sb = new StringBuilder(1024);
        
        sb.append("# HELP jwsch_topic_total Total topics\n");
        sb.append("# TYPE jwsch_topic_total gauge\n");
        sb.append("jwsch_topic_total ").append(getTotalTopics()).append("\n\n");
        
        List<TopicSubscriptionStats> top10Subs = getTop10Subscriptions();
        if (!top10Subs.isEmpty()) {
            sb.append("# HELP jwsch_topic_subscriptions Top 10 topic subscriptions\n");
            sb.append("# TYPE jwsch_topic_subscriptions gauge\n");
            for (TopicSubscriptionStats stats : top10Subs) {
                String escapedTopic = escapePrometheusLabel(stats.getTopic());
                sb.append("jwsch_topic_subscriptions{topic=\"").append(escapedTopic).append("\"} ")
                    .append(stats.getSubscriptionCount()).append("\n");
            }
            sb.append("\n");
        }
        
        List<TopicMessageStats> top10Msgs = getTop10MessageCount();
        if (!top10Msgs.isEmpty()) {
            sb.append("# HELP jwsch_topic_messages Top 10 topic messages\n");
            sb.append("# TYPE jwsch_topic_messages gauge\n");
            for (TopicMessageStats stats : top10Msgs) {
                String escapedTopic = escapePrometheusLabel(stats.getTopic());
                sb.append("jwsch_topic_messages{topic=\"").append(escapedTopic)
                    .append("\",type=\"total\"} ").append(stats.getMessageCount()).append("\n");
                sb.append("jwsch_topic_messages{topic=\"").append(escapedTopic)
                    .append("\",type=\"push\"} ").append(stats.getPushCount()).append("\n");
                sb.append("jwsch_topic_messages{topic=\"").append(escapedTopic)
                    .append("\",type=\"broadcast\"} ").append(stats.getBroadcastCount()).append("\n");
            }
            sb.append("\n");
        }
        
        List<TopicTrafficStats> top10Traffic = getTop10Traffic();
        if (!top10Traffic.isEmpty()) {
            sb.append("# HELP jwsch_topic_bytes Top 10 topic traffic bytes\n");
            sb.append("# TYPE jwsch_topic_bytes gauge\n");
            for (TopicTrafficStats stats : top10Traffic) {
                String escapedTopic = escapePrometheusLabel(stats.getTopic());
                sb.append("jwsch_topic_bytes{topic=\"").append(escapedTopic)
                    .append("\",type=\"total\"} ").append(stats.getTotalBytes()).append("\n");
                sb.append("jwsch_topic_bytes{topic=\"").append(escapedTopic)
                    .append("\",type=\"push\"} ").append(stats.getPushBytes()).append("\n");
                sb.append("jwsch_topic_bytes{topic=\"").append(escapedTopic)
                    .append("\",type=\"broadcast\"} ").append(stats.getBroadcastBytes()).append("\n");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    private String escapePrometheusLabel(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n");
    }
    
    private void checkTopicLimit() {
        int totalTopics = getTotalTopics();
        if (totalTopics > maxTopics) {
            LOGGER.warn("Topic count {} exceeds limit {}, consider resetting stats", 
                totalTopics, maxTopics);
        }
    }
}