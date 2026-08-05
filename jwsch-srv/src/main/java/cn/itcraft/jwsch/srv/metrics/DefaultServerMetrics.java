package cn.itcraft.jwsch.srv.metrics;

/**
 * DefaultServerMetrics 是 ServerMetrics 接口的默认实现，基于 Micrometer 和 JMX。
 * 
 * <p>提供以下指标的收集与暴露：
 * <ul>
 *   <li>WebSocket 和 TCP 连接数</li>
 *   <li>数据包收发计数与字节数</li>
 *   <li>错误计数</li>
 *   <li>处理时长与路由时长</li>
 * </ul>
 * 
 * <p>支持 Prometheus 格式的 scrape 输出，并可注册为 JMX MBean 供监控工具访问。
 * 
 * @author itcraft
 * @since 1.0
 */

import cn.itcraft.jwsch.common.exception.ErrorCode;
import cn.itcraft.jwsch.srv.stats.NoOpTopicStatsManager;
import cn.itcraft.jwsch.srv.stats.TopicStatsManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public final class DefaultServerMetrics implements ServerMetrics {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultServerMetrics.class);
    private static final String JMX_OBJECT_NAME = "cn.itcraft.jwsch.srv:type=ServerMetrics";
    
    private final MeterRegistry meterRegistry;
    private volatile TopicStatsManager topicStatsManager;
    
    private final AtomicLong websocketConnections = new AtomicLong(0);
    private final AtomicLong tcpConnections = new AtomicLong(0);
    
    private final Counter packetsReceived;
    private final Counter packetsSent;
    private final Counter packetsDropped;
    private final Counter bytesReceived;
    private final Counter bytesSent;
    private final Counter errors;
    
    private final Timer packetProcessTime;
    private final Timer routeTime;
    
    private ObjectName jmxObjectName;
    
    /**
     * 默认构造函数，使用 Prometheus MeterRegistry 且禁用 JMX。
     */
    public DefaultServerMetrics() {
        this(new PrometheusMeterRegistry(PrometheusConfig.DEFAULT), false);
    }
    
    /**
     * 使用指定 MeterRegistry 的构造函数，默认禁用 JMX。
     *
     * @param meterRegistry 指标注册器，不可为 null
     */
    public DefaultServerMetrics(MeterRegistry meterRegistry) {
        this(meterRegistry, false);
    }
    
    /**
     * 使用默认 Prometheus MeterRegistry 并指定 JMX 启用状态的构造函数。
     *
     * @param jmxEnabled 是否启用 JMX MBean 注册
     */
    public DefaultServerMetrics(boolean jmxEnabled) {
        this(new PrometheusMeterRegistry(PrometheusConfig.DEFAULT), jmxEnabled);
    }
    
    /**
     * 使用指定 MeterRegistry 和 JMX 启用状态的构造函数。
     *
     * @param meterRegistry 指标注册器，不可为 null
     * @param jmxEnabled 是否启用 JMX MBean 注册
     */
    public DefaultServerMetrics(MeterRegistry meterRegistry, boolean jmxEnabled) {
        this.meterRegistry = meterRegistry;
        this.topicStatsManager = NoOpTopicStatsManager.INSTANCE;
        
        Gauge.builder("jwsch.websocket.connections", websocketConnections, AtomicLong::get)
            .description("Current WebSocket connections")
            .register(meterRegistry);
        
        Gauge.builder("jwsch.tcp.connections", tcpConnections, AtomicLong::get)
            .description("Current TCP connections")
            .register(meterRegistry);
        
        this.packetsReceived = Counter.builder("jwsch.packets.received")
            .description("Total packets received")
            .register(meterRegistry);
        
        this.packetsSent = Counter.builder("jwsch.packets.sent")
            .description("Total packets sent")
            .register(meterRegistry);
        
        this.packetsDropped = Counter.builder("jwsch.packets.dropped")
            .description("Total packets dropped")
            .register(meterRegistry);
        
        this.bytesReceived = Counter.builder("jwsch.bytes.received")
            .description("Total bytes received")
            .register(meterRegistry);
        
        this.bytesSent = Counter.builder("jwsch.bytes.sent")
            .description("Total bytes sent")
            .register(meterRegistry);
        
        this.errors = Counter.builder("jwsch.errors")
            .description("Total errors")
            .register(meterRegistry);
        
        this.packetProcessTime = Timer.builder("jwsch.packet.process.time")
            .description("Packet processing time")
            .register(meterRegistry);
        
        this.routeTime = Timer.builder("jwsch.route.time")
            .description("Packet routing time")
            .register(meterRegistry);
        
        if (jmxEnabled) {
            registerMBean();
        }
    }
    
    private void registerMBean() {
        try {
            MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
            jmxObjectName = new ObjectName(JMX_OBJECT_NAME);
            mbeanServer.registerMBean(this, jmxObjectName);
            LOGGER.info("JMX MBean registered: {}", JMX_OBJECT_NAME);
        } catch (Exception e) {
            LOGGER.error("Failed to register JMX MBean", e);
        }
    }
    
    /**
     * 注销 JMX MBean（如果已注册）。
     * 
     * <p>如果未启用 JMX 或 MBean 未注册，则无操作。
     */
    public void unregisterMBean() {
        if (jmxObjectName != null) {
            try {
                MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
                mbeanServer.unregisterMBean(jmxObjectName);
                LOGGER.info("JMX MBean unregistered: {}", JMX_OBJECT_NAME);
            } catch (Exception e) {
                LOGGER.error("Failed to unregister JMX MBean", e);
            }
        }
    }
    
    /**
     * 设置主题统计管理器。
     *
     * @param topicStatsManager 主题统计管理器
     */
    public void setTopicStatsManager(TopicStatsManager topicStatsManager) {
        this.topicStatsManager = topicStatsManager;
    }
    
    /**
     * 获取指标注册器。
     *
     * @return 指标注册器
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
    
    /**
     * 增加 WebSocket 连接计数。
     */
    public void incrementWebSocketConnections() {
        websocketConnections.incrementAndGet();
    }
    
    /**
     * 减少 WebSocket 连接计数。
     */
    public void decrementWebSocketConnections() {
        websocketConnections.decrementAndGet();
    }
    
    /**
     * 增加 TCP 连接计数。
     */
    public void incrementTcpConnections() {
        tcpConnections.incrementAndGet();
    }
    
    /**
     * 减少 TCP 连接计数。
     */
    public void decrementTcpConnections() {
        tcpConnections.decrementAndGet();
    }
    
    /**
     * 记录收到一个数据包。
     *
     * @param bytes 数据包字节数
     */
    public void recordPacketReceived(int bytes) {
        packetsReceived.increment();
        bytesReceived.increment(bytes);
    }
    
    /**
     * 记录发送一个数据包。
     *
     * @param bytes 数据包字节数
     */
    public void recordPacketSent(int bytes) {
        packetsSent.increment();
        bytesSent.increment(bytes);
    }
    
    /**
     * 记录丢弃一个数据包。
     */
    public void recordPacketDropped() {
        packetsDropped.increment();
    }
    
    /**
     * 记录一个错误。
     *
     * @param errorCode 错误码
     */
    public void recordError(ErrorCode errorCode) {
        errors.increment();
    }
    
    /**
     * 记录数据包处理时间。
     *
     * @param duration 时长
     * @param unit 时间单位
     */
    public void recordProcessTime(long duration, TimeUnit unit) {
        packetProcessTime.record(duration, unit);
    }
    
    /**
     * 记录数据包路由时间。
     *
     * @param duration 时长
     * @param unit 时间单位
     */
    public void recordRouteTime(long duration, TimeUnit unit) {
        routeTime.record(duration, unit);
    }
    
    /**
     * 生成 Prometheus 格式的指标数据。
     *
     * @return Prometheus 格式的指标字符串
     */
    public String scrapePrometheus() {
        StringBuilder sb = new StringBuilder(4096);
        
        if (meterRegistry instanceof PrometheusMeterRegistry) {
            sb.append(((PrometheusMeterRegistry) meterRegistry).scrape());
        }
        
        sb.append("\n");
        sb.append(topicStatsManager.scrapeTop10Metrics());
        
        return sb.toString();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public long getWebSocketConnections() {
        return websocketConnections.get();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public long getTcpConnections() {
        return tcpConnections.get();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalPacketsReceived() {
        return (long) packetsReceived.count();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalPacketsSent() {
        return (long) packetsSent.count();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalErrors() {
        return (long) errors.count();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int getTotalTopics() {
        return topicStatsManager.getTotalTopics();
    }
}