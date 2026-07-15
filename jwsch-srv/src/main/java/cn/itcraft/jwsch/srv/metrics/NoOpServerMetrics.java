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
    
    @Override
    public void incrementWebSocketConnections() {
    }
    
    @Override
    public void decrementWebSocketConnections() {
    }
    
    @Override
    public void incrementTcpConnections() {
    }
    
    @Override
    public void decrementTcpConnections() {
    }
    
    @Override
    public void recordPacketReceived(int bytes) {
    }
    
    @Override
    public void recordPacketSent(int bytes) {
    }
    
    @Override
    public void recordPacketDropped() {
    }
    
    @Override
    public void recordError(ErrorCode errorCode) {
    }
    
    @Override
    public void recordProcessTime(long duration, TimeUnit unit) {
    }
    
    @Override
    public void recordRouteTime(long duration, TimeUnit unit) {
    }
    
    @Override
    public void setTopicStatsManager(TopicStatsManager topicStatsManager) {
    }
    
    @Override
    public String scrapePrometheus() {
        return "";
    }
    
    @Override
    public void unregisterMBean() {
    }
    
    @Override
    public long getWebSocketConnections() {
        return 0;
    }
    
    @Override
    public long getTcpConnections() {
        return 0;
    }
    
    @Override
    public long getTotalPacketsReceived() {
        return 0;
    }
    
    @Override
    public long getTotalPacketsSent() {
        return 0;
    }
    
    @Override
    public long getTotalErrors() {
        return 0;
    }
    
    @Override
    public int getTotalTopics() {
        return 0;
    }
}