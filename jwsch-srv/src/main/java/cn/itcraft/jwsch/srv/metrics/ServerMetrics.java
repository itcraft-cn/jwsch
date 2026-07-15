package cn.itcraft.jwsch.srv.metrics;

import cn.itcraft.jwsch.common.exception.ErrorCode;
import cn.itcraft.jwsch.srv.stats.TopicStatsManager;

import java.util.concurrent.TimeUnit;

/**
 * ServerMetrics 接口。
 * 
 * <p>提供服务器指标收集功能，支持多态实现。
 */
public interface ServerMetrics extends ServerMetricsMBean {
    
    void incrementWebSocketConnections();
    
    void decrementWebSocketConnections();
    
    void incrementTcpConnections();
    
    void decrementTcpConnections();
    
    void recordPacketReceived(int bytes);
    
    void recordPacketSent(int bytes);
    
    void recordPacketDropped();
    
    void recordError(ErrorCode errorCode);
    
    void recordProcessTime(long duration, TimeUnit unit);
    
    void recordRouteTime(long duration, TimeUnit unit);
    
    void setTopicStatsManager(TopicStatsManager topicStatsManager);
    
    String scrapePrometheus();
    
    void unregisterMBean();
}