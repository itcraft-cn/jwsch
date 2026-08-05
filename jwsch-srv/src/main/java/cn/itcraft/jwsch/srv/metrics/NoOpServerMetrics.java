package cn.itcraft.jwsch.srv.metrics;

import cn.itcraft.jwsch.common.exception.ErrorCode;
import cn.itcraft.jwsch.srv.stats.TopicStatsManager;

import java.util.concurrent.TimeUnit;

/**
 * ServerMetrics 空实现。
 * 
 * <p>所有方法均为空操作，用于不需要指标收集的场景。
 */
public final class NoOpServerMetrics implements ServerMetrics {
    
    public static final NoOpServerMetrics INSTANCE = new NoOpServerMetrics();
    
    private NoOpServerMetrics() {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void incrementWebSocketConnections() {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void decrementWebSocketConnections() {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void incrementTcpConnections() {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void decrementTcpConnections() {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void recordPacketReceived(int bytes) {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void recordPacketSent(int bytes) {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void recordPacketDropped() {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void recordError(ErrorCode errorCode) {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void recordProcessTime(long duration, TimeUnit unit) {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void recordRouteTime(long duration, TimeUnit unit) {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setTopicStatsManager(TopicStatsManager topicStatsManager) {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String scrapePrometheus() {
        return "";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterMBean() {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public long getWebSocketConnections() {
        return 0;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public long getTcpConnections() {
        return 0;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalPacketsReceived() {
        return 0;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalPacketsSent() {
        return 0;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalErrors() {
        return 0;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int getTotalTopics() {
        return 0;
    }
}