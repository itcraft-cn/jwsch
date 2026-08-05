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
    
    /**
     * 增加 WebSocket 连接计数。
     */
    void incrementWebSocketConnections();
    
    /**
     * 减少 WebSocket 连接计数。
     */
    void decrementWebSocketConnections();
    
    /**
     * 增加 TCP 连接计数。
     */
    void incrementTcpConnections();
    
    /**
     * 减少 TCP 连接计数。
     */
    void decrementTcpConnections();
    
    /**
     * 记录收到一个数据包。
     *
     * @param bytes 数据包字节数
     */
    void recordPacketReceived(int bytes);
    
    /**
     * 记录发送一个数据包。
     *
     * @param bytes 数据包字节数
     */
    void recordPacketSent(int bytes);
    
    /**
     * 记录丢弃一个数据包。
     */
    void recordPacketDropped();
    
    /**
     * 记录一个错误。
     *
     * @param errorCode 错误码
     */
    void recordError(ErrorCode errorCode);
    
    /**
     * 记录数据包处理时间。
     *
     * @param duration 时长
     * @param unit 时间单位
     */
    void recordProcessTime(long duration, TimeUnit unit);
    
    /**
     * 记录数据包路由时间。
     *
     * @param duration 时长
     * @param unit 时间单位
     */
    void recordRouteTime(long duration, TimeUnit unit);
    
    /**
     * 设置主题统计管理器。
     *
     * @param topicStatsManager 主题统计管理器
     */
    void setTopicStatsManager(TopicStatsManager topicStatsManager);
    
    /**
     * 生成 Prometheus 格式的指标数据。
     *
     * @return Prometheus 格式的指标字符串
     */
    String scrapePrometheus();
    
    /**
     * 注销 JMX MBean（如果已注册）。
     */
    void unregisterMBean();
}