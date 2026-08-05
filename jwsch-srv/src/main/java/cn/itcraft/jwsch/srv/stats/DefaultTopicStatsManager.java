package cn.itcraft.jwsch.srv.stats;

import cn.itcraft.jwsch.common.protocol.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of {@link TopicStatsManager} that provides topic-based statistics tracking.
 * 
 * <p>This manager tracks three categories of statistics per topic:
 * <ul>
 *   <li>Subscription counts - number of active subscribers per topic</li>
 *   <li>Message counts - number of messages by command type per topic</li>
 *   <li>Traffic bytes - total bytes transmitted by command type per topic</li>
 * </ul>
 * 
 * <p>Statistics are collected in real-time and can be queried for top 10 topics in each category.
 * A scheduled cleanup task periodically checks if the number of tracked topics exceeds a configurable limit.
 * 
 * <p>Thread-safe: all methods are thread-safe and designed for high-concurrency environments.
 */
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
    
    /**
     * Creates a new DefaultTopicStatsManager with default configuration.
     * <p>Maximum topics: {@value #DEFAULT_MAX_TOPICS}, cleanup interval: {@value #DEFAULT_CLEANUP_INTERVAL} seconds.
     */
    public DefaultTopicStatsManager() {
        this(DEFAULT_MAX_TOPICS, DEFAULT_CLEANUP_INTERVAL);
    }
    
    /**
     * Creates a new DefaultTopicStatsManager with custom configuration.
     * 
     * @param maxTopics maximum number of topics to track before warning, must be positive
     * @param cleanupIntervalSeconds interval in seconds for periodic cleanup checks, must be positive
     */
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
    
    /**
     * Starts the statistics manager and schedules periodic cleanup tasks.
     * <p>Idempotent: subsequent calls have no effect if already started.
     */
    @Override
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
    
    /**
     * Stops the statistics manager and shuts down the scheduled executor.
     * <p>Idempotent: subsequent calls have no effect if already stopped.
     */
    @Override
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
    
    /**
     * Records a new subscription to a topic.
     * 
     * @param topic the topic name, must not be null
     */
    @Override
    public void recordSubscribe(String topic) {
        subscriptionTracker.subscribe(topic);
    }
    
    /**
     * Records an unsubscribe event from a topic.
     * 
     * @param topic the topic name, must not be null
     */
    @Override
    public void recordUnsubscribe(String topic) {
        subscriptionTracker.unsubscribe(topic);
    }
    
    /**
     * Records a message sent to a topic.
     * 
     * @param topic the topic name, must not be null
     * @param command the command byte indicating message type
     * @param bytes the size of the message in bytes
     */
    @Override
    public void recordMessage(String topic, byte command, int bytes) {
        messageTracker.record(topic, command);
        trafficTracker.record(topic, command, bytes);
    }
    
    /**
     * Returns the top 10 topics by subscription count.
     * 
     * @return list of {@link TopicSubscriptionStats} sorted descending by subscription count
     */
    @Override
    public List<TopicSubscriptionStats> getTop10Subscriptions() {
        return subscriptionTracker.getTop10();
    }
    
    /**
     * Returns the top 10 topics by message count.
     * 
     * @return list of {@link TopicMessageStats} sorted descending by total message count
     */
    @Override
    public List<TopicMessageStats> getTop10MessageCount() {
        return messageTracker.getTop10();
    }
    
    /**
     * Returns the top 10 topics by traffic bytes.
     * 
     * @return list of {@link TopicTrafficStats} sorted descending by total bytes
     */
    @Override
    public List<TopicTrafficStats> getTop10Traffic() {
        return trafficTracker.getTop10();
    }
    
    /**
     * Returns the total number of unique topics being tracked across all categories.
     * <p>This is the maximum of subscription tracker, message tracker, and traffic tracker topic counts.
     * 
     * @return total unique topics tracked
     */
    @Override
    public int getTotalTopics() {
        return Math.max(
            subscriptionTracker.getTotalTopics(),
            Math.max(messageTracker.getTotalTopics(), trafficTracker.getTotalTopics())
        );
    }
    
    /**
     * Returns the subscription tracker instance used by this manager.
     * 
     * @return the subscription tracker
     */
    @Override
    public TopicSubscriptionTracker getSubscriptionTracker() {
        return subscriptionTracker;
    }
    
    /**
     * Returns the message tracker instance used by this manager.
     * 
     * @return the message tracker
     */
    @Override
    public TopicMessageTracker getMessageTracker() {
        return messageTracker;
    }
    
    /**
     * Returns the traffic tracker instance used by this manager.
     * 
     * @return the traffic tracker
     */
    @Override
    public TopicTrafficTracker getTrafficTracker() {
        return trafficTracker;
    }
    
    /**
     * Resets all statistics across all trackers.
     * <p>Clears all collected data; useful for resetting counters during maintenance.
     */
    @Override
    public void resetStats() {
        subscriptionTracker.clear();
        messageTracker.clear();
        trafficTracker.clear();
        LOGGER.info("Topic stats reset");
    }
    
    /**
     * 生成 Top 10 Topic 的 Prometheus 格式指标。
     * 
     * <p>Generates Prometheus metrics in text format for the top 10 topics in each category.
     * Includes gauge metrics for total topics, subscription counts, message counts by type,
     * and traffic bytes by type.
     * 
     * <p>Example output:
     * <pre>
     * # HELP jwsch_topic_total Total topics
     * # TYPE jwsch_topic_total gauge
     * jwsch_topic_total 42
     * 
     * # HELP jwsch_topic_subscriptions Top 10 topic subscriptions
     * # TYPE jwsch_topic_subscriptions gauge
     * jwsch_topic_subscriptions{topic="news"} 15
     * </pre>
     * 
     * @return Prometheus metrics as a formatted string
     */
    @Override
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
    
    /**
     * Escapes special characters in Prometheus label values.
     * 
     * @param value the label value to escape
     * @return escaped label value, empty string if input is null
     */
    private String escapePrometheusLabel(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n");
    }
    
    /**
     * Checks if the number of tracked topics exceeds the configured maximum.
     * <p>Called periodically by the scheduled executor; logs a warning if limit exceeded.
     */
    private void checkTopicLimit() {
        int totalTopics = getTotalTopics();
        if (totalTopics > maxTopics) {
            LOGGER.warn("Topic count {} exceeds limit {}, consider resetting stats", 
                totalTopics, maxTopics);
        }
    }
}