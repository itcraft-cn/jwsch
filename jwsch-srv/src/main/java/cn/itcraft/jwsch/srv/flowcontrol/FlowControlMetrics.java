package cn.itcraft.jwsch.srv.flowcontrol;

import cn.itcraft.jwsch.srv.router.BackpressureManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 流量控制指标收集器。
 *
 * <p>统一收集三层流量控制指标：
 * <ul>
 *   <li>L1 入口限速：丢弃数、当前速率</li>
 *   <li>L2 背压管理：激活状态、非可写连接数</li>
 *   <li>L3 出站缓冲：队列大小、丢弃数、溢出次数</li>
 * </ul>
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
    
    public void recordInboundDropped() {
        inboundDropped.increment();
    }
    
    public void recordInboundDropped(int count) {
        inboundDropped.increment(count);
    }
    
    public void setInboundRate(long rate) {
        inboundRate.set(rate);
    }
    
    public void setBackpressureActive(boolean active) {
        backpressureActive.set(active ? 1 : 0);
    }
    
    public void setNonWritableCount(int count) {
        nonWritableCount.set(count);
    }
    
    public void recordTopicDropped() {
        topicDropped.increment();
    }
    
    public void recordTopicDropped(int count) {
        topicDropped.increment(count);
    }
    
    public void recordOutboundDropped() {
        outboundDropped.increment();
    }
    
    public void recordOutboundDropped(int count) {
        outboundDropped.increment(count);
    }
    
    public void setOutboundQueueSize(int size) {
        outboundQueueSize.set(size);
    }
    
    public void recordOutboundOverflow() {
        outboundOverflow.increment();
    }
    
    public void updateFromBackpressureManager(BackpressureManager manager) {
        setBackpressureActive(manager.isAutoReadDisabled());
        setNonWritableCount(manager.getNonWritableCount());
    }
    
    public void updateFromTopicBackpressureManager(TopicBackpressureManager manager) {
        long dropCount = manager.getBackpressureDropCount();
        recordTopicDropped((int) dropCount);
    }
    
    public void updateFromOutboundBufferHandler(OutboundBufferHandler handler) {
        setOutboundQueueSize(handler.getQueueSize());
    }
    
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
    
    public double getInboundDroppedCount() {
        return inboundDropped.count();
    }
    
    public double getTopicDroppedCount() {
        return topicDropped.count();
    }
    
    public double getOutboundDroppedCount() {
        return outboundDropped.count();
    }
    
    public double getOutboundOverflowCount() {
        return outboundOverflow.count();
    }
}
