package cn.itcraft.jwsch.srv.flowcontrol;

import cn.itcraft.jwsch.srv.router.BackpressureManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Flow control metrics collector.
 *
 * <p>Unified collection of three-layer flow control metrics:
 * <ul>
 *   <li>L1 Inbound rate limiting: dropped packets count, current rate</li>
 *   <li>L2 Backpressure management: active status, non-writable connections count</li>
 *   <li>L3 Outbound buffering: queue size, dropped packets count, overflow events</li>
 * </ul>
 *
 * <p>All metrics are registered with Micrometer and can be exported to monitoring systems
 * like Prometheus, Graphite, or InfluxDB.
 */
public final class FlowControlMetrics {
    
    private final MeterRegistry meterRegistry;
    
    private final Counter inboundDropped;
    private final AtomicLong inboundRate = new AtomicLong(0);
    
    private final AtomicLong backpressureActive = new AtomicLong(0);
    private final AtomicLong nonWritableCount = new AtomicLong(0);
    private final Counter topicDropped;
    
    private final Counter outboundDropped;
    private final AtomicLong outboundQueueSize = new AtomicLong(0);
    private final Counter outboundOverflow;
    
    /**
     * Creates a FlowControlMetrics instance with the specified MeterRegistry.
     *
     * @param meterRegistry the Micrometer MeterRegistry for metrics registration
     */
    public FlowControlMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.inboundDropped = Counter.builder("jwsch.flowcontrol.inbound.dropped")
            .description("Packets dropped by inbound rate limiter (L1)")
            .register(meterRegistry);
        
        Gauge.builder("jwsch.flowcontrol.inbound.rate", inboundRate, AtomicLong::get)
            .description("Current inbound rate limit (tokens/sec)")
            .register(meterRegistry);
        
        Gauge.builder("jwsch.flowcontrol.backpressure.active", backpressureActive, AtomicLong::get)
            .description("Whether backpressure is active (1=active, 0=inactive)")
            .register(meterRegistry);
        
        Gauge.builder("jwsch.flowcontrol.backpressure.nonwritable", nonWritableCount, AtomicLong::get)
            .description("Number of non-writable frontend connections")
            .register(meterRegistry);
        
        this.topicDropped = Counter.builder("jwsch.flowcontrol.topic.dropped")
            .description("Packets dropped by topic backpressure (L2)")
            .register(meterRegistry);
        
        this.outboundDropped = Counter.builder("jwsch.flowcontrol.outbound.dropped")
            .description("Packets dropped by outbound buffer overflow (L3)")
            .register(meterRegistry);
        
        Gauge.builder("jwsch.flowcontrol.outbound.queue", outboundQueueSize, AtomicLong::get)
            .description("Current outbound buffer queue size")
            .register(meterRegistry);
        
        this.outboundOverflow = Counter.builder("jwsch.flowcontrol.outbound.overflow")
            .description("Outbound buffer overflow events")
            .register(meterRegistry);
    }
    
    /**
     * Records a single inbound dropped packet.
     */
    public void recordInboundDropped() {
        inboundDropped.increment();
    }
    
    /**
     * Records multiple inbound dropped packets.
     *
     * @param count the number of dropped packets to record
     */
    public void recordInboundDropped(int count) {
        inboundDropped.increment(count);
    }
    
    /**
     * Sets the current inbound rate limit.
     *
     * @param rate the current inbound rate limit in tokens per second
     */
    public void setInboundRate(long rate) {
        inboundRate.set(rate);
    }
    
    /**
     * Sets the backpressure active status.
     *
     * @param active true if backpressure is active, false otherwise
     */
    public void setBackpressureActive(boolean active) {
        backpressureActive.set(active ? 1 : 0);
    }
    
    /**
     * Sets the count of non-writable frontend connections.
     *
     * @param count the number of non-writable connections
     */
    public void setNonWritableCount(int count) {
        nonWritableCount.set(count);
    }
    
    /**
     * Records a single topic-level dropped packet.
     */
    public void recordTopicDropped() {
        topicDropped.increment();
    }
    
    /**
     * Records multiple topic-level dropped packets.
     *
     * @param count the number of dropped packets to record
     */
    public void recordTopicDropped(int count) {
        topicDropped.increment(count);
    }
    
    /**
     * Records a single outbound dropped packet.
     */
    public void recordOutboundDropped() {
        outboundDropped.increment();
    }
    
    /**
     * Records multiple outbound dropped packets.
     *
     * @param count the number of dropped packets to record
     */
    public void recordOutboundDropped(int count) {
        outboundDropped.increment(count);
    }
    
    /**
     * Sets the current outbound buffer queue size.
     *
     * @param size the current queue size
     */
    public void setOutboundQueueSize(int size) {
        outboundQueueSize.set(size);
    }
    
    /**
     * Records an outbound buffer overflow event.
     */
    public void recordOutboundOverflow() {
        outboundOverflow.increment();
    }
    
    /**
     * Updates metrics from a BackpressureManager instance.
     *
     * @param manager the BackpressureManager to read data from
     */
    public void updateFromBackpressureManager(BackpressureManager manager) {
        setBackpressureActive(manager.isAutoReadDisabled());
        setNonWritableCount(manager.getNonWritableCount());
    }
    
    /**
     * Updates metrics from a TopicBackpressureManager instance.
     *
     * @param manager the TopicBackpressureManager to read data from
     */
    public void updateFromTopicBackpressureManager(TopicBackpressureManager manager) {
        long dropCount = manager.getBackpressureDropCount();
        recordTopicDropped((int) dropCount);
    }
    
    /**
     * Updates metrics from an OutboundBufferHandler instance.
     *
     * @param handler the OutboundBufferHandler to read data from
     */
    public void updateFromOutboundBufferHandler(OutboundBufferHandler handler) {
        setOutboundQueueSize(handler.getQueueSize());
    }
    
    /**
     * Returns the MeterRegistry used by this metrics collector.
     *
     * @return the MeterRegistry instance
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
    
    /**
     * Returns the total count of inbound dropped packets.
     *
     * @return the inbound dropped packets count
     */
    public double getInboundDroppedCount() {
        return inboundDropped.count();
    }
    
    /**
     * Returns the total count of topic-level dropped packets.
     *
     * @return the topic dropped packets count
     */
    public double getTopicDroppedCount() {
        return topicDropped.count();
    }
    
    /**
     * Returns the total count of outbound dropped packets.
     *
     * @return the outbound dropped packets count
     */
    public double getOutboundDroppedCount() {
        return outboundDropped.count();
    }
    
    /**
     * Returns the total count of outbound buffer overflow events.
     *
     * @return the outbound overflow events count
     */
    public double getOutboundOverflowCount() {
        return outboundOverflow.count();
    }
}
