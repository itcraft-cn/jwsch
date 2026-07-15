package cn.itcraft.jwsch.srv.metrics;

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
    
    public DefaultServerMetrics() {
        this(new PrometheusMeterRegistry(PrometheusConfig.DEFAULT), false);
    }
    
    public DefaultServerMetrics(MeterRegistry meterRegistry) {
        this(meterRegistry, false);
    }
    
    public DefaultServerMetrics(boolean jmxEnabled) {
        this(new PrometheusMeterRegistry(PrometheusConfig.DEFAULT), jmxEnabled);
    }
    
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
    
    public void setTopicStatsManager(TopicStatsManager topicStatsManager) {
        this.topicStatsManager = topicStatsManager;
    }
    
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
    
    public void incrementWebSocketConnections() {
        websocketConnections.incrementAndGet();
    }
    
    public void decrementWebSocketConnections() {
        websocketConnections.decrementAndGet();
    }
    
    public void incrementTcpConnections() {
        tcpConnections.incrementAndGet();
    }
    
    public void decrementTcpConnections() {
        tcpConnections.decrementAndGet();
    }
    
    public void recordPacketReceived(int bytes) {
        packetsReceived.increment();
        bytesReceived.increment(bytes);
    }
    
    public void recordPacketSent(int bytes) {
        packetsSent.increment();
        bytesSent.increment(bytes);
    }
    
    public void recordPacketDropped() {
        packetsDropped.increment();
    }
    
    public void recordError(ErrorCode errorCode) {
        errors.increment();
    }
    
    public void recordProcessTime(long duration, TimeUnit unit) {
        packetProcessTime.record(duration, unit);
    }
    
    public void recordRouteTime(long duration, TimeUnit unit) {
        routeTime.record(duration, unit);
    }
    
    public String scrapePrometheus() {
        StringBuilder sb = new StringBuilder(4096);
        
        if (meterRegistry instanceof PrometheusMeterRegistry) {
            sb.append(((PrometheusMeterRegistry) meterRegistry).scrape());
        }
        
        sb.append("\n");
        sb.append(topicStatsManager.scrapeTop10Metrics());
        
        return sb.toString();
    }
    
    @Override
    public long getWebSocketConnections() {
        return websocketConnections.get();
    }
    
    @Override
    public long getTcpConnections() {
        return tcpConnections.get();
    }
    
    @Override
    public long getTotalPacketsReceived() {
        return (long) packetsReceived.count();
    }
    
    @Override
    public long getTotalPacketsSent() {
        return (long) packetsSent.count();
    }
    
    @Override
    public long getTotalErrors() {
        return (long) errors.count();
    }
    
    @Override
    public int getTotalTopics() {
        return topicStatsManager.getTotalTopics();
    }
}