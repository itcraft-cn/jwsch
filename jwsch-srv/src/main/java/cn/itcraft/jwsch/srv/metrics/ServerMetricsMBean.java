package cn.itcraft.jwsch.srv.metrics;

/**
 * ServerMetrics JMX MBean 接口。
 */
public interface ServerMetricsMBean {
    
    long getWebSocketConnections();
    
    long getTcpConnections();
    
    long getTotalPacketsReceived();
    
    long getTotalPacketsSent();
    
    long getTotalErrors();
    
    int getTotalTopics();
}