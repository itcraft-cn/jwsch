package cn.itcraft.jwsch.srv.metrics;

/**
 * ServerMetrics JMX MBean 接口。
 */
public interface ServerMetricsMBean {
    
    /**
     * 获取当前 WebSocket 连接数。
     *
     * @return WebSocket 连接数
     */
    long getWebSocketConnections();
    
    /**
     * 获取当前 TCP 连接数。
     *
     * @return TCP 连接数
     */
    long getTcpConnections();
    
    /**
     * 获取总接收数据包数。
     *
     * @return 总接收数据包数
     */
    long getTotalPacketsReceived();
    
    /**
     * 获取总发送数据包数。
     *
     * @return 总发送数据包数
     */
    long getTotalPacketsSent();
    
    /**
     * 获取总错误数。
     *
     * @return 总错误数
     */
    long getTotalErrors();
    
    /**
     * 获取总主题数。
     *
     * @return 总主题数
     */
    int getTotalTopics();
}