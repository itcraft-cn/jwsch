# 流量控制实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现三层流量控制（L1入口限速、L2背压传导、L3堆积溢出）+ 监控可观测性

**Architecture:** 三层递进式：L1令牌桶限速防pub打爆 → L2磁滞回线背压传导 → L3有界队列+溢出策略防OOM → Micrometer指标监控

**Tech Stack:** Java 8 + Netty 4.2.x + Micrometer

---

## File Structure

### 新增文件

| 文件 | 职责 |
|------|------|
| `jwsch-common/.../flowcontrol/RateLimiter.java` | 令牌桶限速器 |
| `jwsch-common/.../flowcontrol/OverflowStrategy.java` | 溢出策略枚举 |
| `jwsch-common/.../flowcontrol/FlowControlConfig.java` | 流控配置（Builder模式） |
| `jwsch-srv/.../flowcontrol/InboundRateLimiterHandler.java` | L1入口限速Handler |
| `jwsch-srv/.../flowcontrol/OutboundBufferHandler.java` | L3堆积溢出Handler |
| `jwsch-srv/.../flowcontrol/TopicBackpressureManager.java` | Per-topic背压管理 |
| `jwsch-srv/.../flowcontrol/TopicBackpressureState.java` | Topic背压状态 |
| `jwsch-srv/.../flowcontrol/FlowControlMetrics.java` | 流控指标统一注册 |

### 修改文件

| 文件 | 修改内容 |
|------|----------|
| `BackpressureManager.java` | 磁滞回线修复，重新启用 |
| `TcpServerInitializer.java` | 集成L1限速Handler |
| `WebSocketServer.java` | 集成L3堆积Handler |
| `WebSocketHandler.java` | 上报writability到TopicBackpressureManager |
| `PacketRouter.java` | 集成L2背压检查 |
| `ErrorCode.java` | 新增RATE_LIMITED |
| `ProtocolConsts.java` | 新增流控常量 |

---

## Phase 1: 基础设施

### Task 1.1: OverflowStrategy枚举

**Files:**
- Create: `jwsch-common/src/main/java/cn/itcraft/jwsch/common/flowcontrol/OverflowStrategy.java`

- [ ] **Step 1: 创建OverflowStrategy枚举**

```java
package cn.itcraft.jwsch.common.flowcontrol;

/**
 * 溢出策略枚举。
 *
 * <p>定义当出站队列满时的处理策略：
 * <ul>
 *   <li>DROP_OLDEST: 丢弃最旧消息，保证新消息优先</li>
 *   <li>DROP_NEWEST: 丢弃新消息，保证已入队消息投递</li>
 *   <li>DISCONNECT: 断开慢消费者连接</li>
 *   <li>DROP_OLDEST_THEN_DISCONNECT: 先丢弃旧消息，超极限阈值后断开</li>
 * </ul>
 */
public enum OverflowStrategy {
    
    DROP_OLDEST,
    DROP_NEWEST,
    DISCONNECT,
    DROP_OLDEST_THEN_DISCONNECT
}
```

- [ ] **Step 2: 编译验证**

Run: `mvnd compile -pl jwsch-common -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add jwsch-common/src/main/java/cn/itcraft/jwsch/common/flowcontrol/OverflowStrategy.java
git commit -m "feat(flowcontrol): add OverflowStrategy enum for outbound buffer handling"
```

---

### Task 1.2: FlowControlConfig配置类

**Files:**
- Create: `jwsch-common/src/main/java/cn/itcraft/jwsch/common/flowcontrol/FlowControlConfig.java`

- [ ] **Step 1: 创建FlowControlConfig**

```java
package cn.itcraft.jwsch.common.flowcontrol;

/**
 * 流量控制配置。
 *
 * <p>使用Builder模式构建不可变配置：
 * <pre>
 * FlowControlConfig config = FlowControlConfig.builder()
 *     .inboundEnabled(true)
 *     .maxTokensPerSecond(10000)
 *     .build();
 * </pre>
 */
public final class FlowControlConfig {
    
    private final boolean inboundEnabled;
    private final int maxTokensPerSecond;
    private final int burstSize;
    private final OverflowStrategy inboundOverflowStrategy;
    
    private final boolean globalBackpressureEnabled;
    private final double globalTriggerThreshold;
    private final double globalReleaseThreshold;
    private final long globalReleaseCooldownMs;
    
    private final boolean topicBackpressureEnabled;
    private final double topicTriggerThreshold;
    private final double topicReleaseThreshold;
    
    private final boolean outboundEnabled;
    private final int maxQueueSize;
    private final int disconnectThreshold;
    private final OverflowStrategy outboundOverflowStrategy;
    
    private FlowControlConfig(Builder builder) {
        this.inboundEnabled = builder.inboundEnabled;
        this.maxTokensPerSecond = builder.maxTokensPerSecond;
        this.burstSize = builder.burstSize;
        this.inboundOverflowStrategy = builder.inboundOverflowStrategy;
        
        this.globalBackpressureEnabled = builder.globalBackpressureEnabled;
        this.globalTriggerThreshold = builder.globalTriggerThreshold;
        this.globalReleaseThreshold = builder.globalReleaseThreshold;
        this.globalReleaseCooldownMs = builder.globalReleaseCooldownMs;
        
        this.topicBackpressureEnabled = builder.topicBackpressureEnabled;
        this.topicTriggerThreshold = builder.topicTriggerThreshold;
        this.topicReleaseThreshold = builder.topicReleaseThreshold;
        
        this.outboundEnabled = builder.outboundEnabled;
        this.maxQueueSize = builder.maxQueueSize;
        this.disconnectThreshold = builder.disconnectThreshold;
        this.outboundOverflowStrategy = builder.outboundOverflowStrategy;
    }
    
    public boolean isInboundEnabled() { return inboundEnabled; }
    public int getMaxTokensPerSecond() { return maxTokensPerSecond; }
    public int getBurstSize() { return burstSize; }
    public OverflowStrategy getInboundOverflowStrategy() { return inboundOverflowStrategy; }
    
    public boolean isGlobalBackpressureEnabled() { return globalBackpressureEnabled; }
    public double getGlobalTriggerThreshold() { return globalTriggerThreshold; }
    public double getGlobalReleaseThreshold() { return globalReleaseThreshold; }
    public long getGlobalReleaseCooldownMs() { return globalReleaseCooldownMs; }
    
    public boolean isTopicBackpressureEnabled() { return topicBackpressureEnabled; }
    public double getTopicTriggerThreshold() { return topicTriggerThreshold; }
    public double getTopicReleaseThreshold() { return topicReleaseThreshold; }
    
    public boolean isOutboundEnabled() { return outboundEnabled; }
    public int getMaxQueueSize() { return maxQueueSize; }
    public int getDisconnectThreshold() { return disconnectThreshold; }
    public OverflowStrategy getOutboundOverflowStrategy() { return outboundOverflowStrategy; }
    
    public static Builder builder() { return new Builder(); }
    
    public static final class Builder {
        private boolean inboundEnabled = true;
        private int maxTokensPerSecond = 10000;
        private int burstSize = 12000;
        private OverflowStrategy inboundOverflowStrategy = OverflowStrategy.DROP;
        
        private boolean globalBackpressureEnabled = true;
        private double globalTriggerThreshold = 0.2;
        private double globalReleaseThreshold = 0.05;
        private long globalReleaseCooldownMs = 500;
        
        private boolean topicBackpressureEnabled = true;
        private double topicTriggerThreshold = 0.3;
        private double topicReleaseThreshold = 0.1;
        
        private boolean outboundEnabled = true;
        private int maxQueueSize = 1024;
        private int disconnectThreshold = 4096;
        private OverflowStrategy outboundOverflowStrategy = OverflowStrategy.DROP_OLDEST_THEN_DISCONNECT;
        
        public Builder inboundEnabled(boolean inboundEnabled) { this.inboundEnabled = inboundEnabled; return this; }
        public Builder maxTokensPerSecond(int maxTokensPerSecond) { this.maxTokensPerSecond = maxTokensPerSecond; return this; }
        public Builder burstSize(int burstSize) { this.burstSize = burstSize; return this; }
        public Builder inboundOverflowStrategy(OverflowStrategy inboundOverflowStrategy) { this.inboundOverflowStrategy = inboundOverflowStrategy; return this; }
        
        public Builder globalBackpressureEnabled(boolean globalBackpressureEnabled) { this.globalBackpressureEnabled = globalBackpressureEnabled; return this; }
        public Builder globalTriggerThreshold(double globalTriggerThreshold) { this.globalTriggerThreshold = globalTriggerThreshold; return this; }
        public Builder globalReleaseThreshold(double globalReleaseThreshold) { this.globalReleaseThreshold = globalReleaseThreshold; return this; }
        public Builder globalReleaseCooldownMs(long globalReleaseCooldownMs) { this.globalReleaseCooldownMs = globalReleaseCooldownMs; return this; }
        
        public Builder topicBackpressureEnabled(boolean topicBackpressureEnabled) { this.topicBackpressureEnabled = topicBackpressureEnabled; return this; }
        public Builder topicTriggerThreshold(double topicTriggerThreshold) { this.topicTriggerThreshold = topicTriggerThreshold; return this; }
        public Builder topicReleaseThreshold(double topicReleaseThreshold) { this.topicReleaseThreshold = topicReleaseThreshold; return this; }
        
        public Builder outboundEnabled(boolean outboundEnabled) { this.outboundEnabled = outboundEnabled; return this; }
        public Builder maxQueueSize(int maxQueueSize) { this.maxQueueSize = maxQueueSize; return this; }
        public Builder disconnectThreshold(int disconnectThreshold) { this.disconnectThreshold = disconnectThreshold; return this; }
        public Builder outboundOverflowStrategy(OverflowStrategy outboundOverflowStrategy) { this.outboundOverflowStrategy = outboundOverflowStrategy; return this; }
        
        public FlowControlConfig build() { return new FlowControlConfig(this); }
    }
    
    public static FlowControlConfig defaultConfig() { return builder().build(); }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvnd compile -pl jwsch-common -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add jwsch-common/src/main/java/cn/itcraft/jwsch/common/flowcontrol/FlowControlConfig.java
git commit -m "feat(flowcontrol): add FlowControlConfig with Builder pattern"
```

---

### Task 1.3: RateLimiter令牌桶

**Files:**
- Create: `jwsch-common/src/main/java/cn/itcraft/jwsch/common/flowcontrol/RateLimiter.java`

- [ ] **Step 1: 创建RateLimiter**

```java
package cn.itcraft.jwsch.common.flowcontrol;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 令牌桶限速器。
 *
 * <p>基于时间的令牌补充，无锁设计（单EventLoop使用）。
 * 
 * <p>工作原理：
 * <pre>
 * 1. 桶容量为 maxTokens
 * 2. 每 refillIntervalNanos 补充 refillTokens 个令牌
 * 3. tryAcquire() 消耗令牌，无令牌时返回 false
 * </pre>
 *
 * <p>配置示例（10000 msg/s，允许短时突发12000）：
 * <pre>
 * RateLimiter limiter = new RateLimiter(12000, 1000, 100_000_000L);
 * </pre>
 */
public final class RateLimiter {
    
    private final long maxTokens;
    private final long refillTokens;
    private final long refillIntervalNanos;
    
    private long availableTokens;
    private long lastRefillNanos;
    
    public RateLimiter(long maxTokens, long refillTokens, long refillIntervalNanos) {
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
        if (refillTokens <= 0) {
            throw new IllegalArgumentException("refillTokens must be positive");
        }
        if (refillIntervalNanos <= 0) {
            throw new IllegalArgumentException("refillIntervalNanos must be positive");
        }
        
        this.maxTokens = maxTokens;
        this.refillTokens = refillTokens;
        this.refillIntervalNanos = refillIntervalNanos;
        this.availableTokens = maxTokens;
        this.lastRefillNanos = System.nanoTime();
    }
    
    /**
     * 尝试获取令牌。
     *
     * @return true 表示获取成功，false 表示无可用令牌
     */
    public boolean tryAcquire() {
        return tryAcquire(1);
    }
    
    /**
     * 尝试获取指定数量令牌。
     *
     * @param permits 需要的令牌数
     * @return true 表示获取成功，false 表示无可用令牌
     */
    public boolean tryAcquire(int permits) {
        if (permits <= 0) {
            return true;
        }
        
        refill();
        
        if (availableTokens >= permits) {
            availableTokens -= permits;
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取当前可用令牌数。
     */
    public long getAvailableTokens() {
        refill();
        return availableTokens;
    }
    
    private void refill() {
        long now = System.nanoTime();
        long elapsed = now - lastRefillNanos;
        
        if (elapsed >= refillIntervalNanos) {
            long refillCycles = elapsed / refillIntervalNanos;
            long tokensToAdd = refillCycles * refillTokens;
            
            availableTokens = Math.min(maxTokens, availableTokens + tokensToAdd);
            lastRefillNanos = now;
        }
    }
    
    /**
     * 从速率创建限速器。
     *
     * @param ratePerSecond 每秒令牌数
     * @param burstSize 桶容量（允许突发）
     * @return 限速器实例
     */
    public static RateLimiter create(int ratePerSecond, int burstSize) {
        long refillIntervalNanos = 1_000_000_000L;
        return new RateLimiter(burstSize, ratePerSecond, refillIntervalNanos);
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvnd compile -pl jwsch-common -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add jwsch-common/src/main/java/cn/itcraft/jwsch/common/flowcontrol/RateLimiter.java
git commit -m "feat(flowcontrol): add RateLimiter token bucket implementation"
```

---

### Task 1.4: ErrorCode新增RATE_LIMITED

**Files:**
- Modify: `jwsch-common/src/main/java/cn/itcraft/jwsch/common/exception/ErrorCode.java`

- [ ] **Step 1: 在ErrorCode中新增RATE_LIMITED**

在`INVALID_TOPIC_LENGTH(6, "Invalid topic length"),`后添加：

```java
    RATE_LIMITED(10, "Rate limited"),
```

- [ ] **Step 2: 编译验证**

Run: `mvnd compile -pl jwsch-common -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add jwsch-common/src/main/java/cn/itcraft/jwsch/common/exception/ErrorCode.java
git commit -m "feat(flowcontrol): add RATE_LIMITED error code"
```

---

## Phase 2: L3 OutboundBufferHandler

### Task 2.1: OutboundBufferHandler

**Files:**
- Create: `jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/flowcontrol/OutboundBufferHandler.java`

- [ ] **Step 1: 创建OutboundBufferHandler**

```java
package cn.itcraft.jwsch.srv.flowcontrol;

import cn.itcraft.jwsch.common.flowcontrol.FlowControlConfig;
import cn.itcraft.jwsch.common.flowcontrol.OverflowStrategy;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketWriter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LongAdder;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 出站缓冲Handler。
 *
 * <p>实现L3堆积溢出控制：
 * <ul>
 *   <li>当Channel不可写时，消息入队缓冲</li>
 *   <li>队列满时根据OverflowStrategy处理</li>
 *   <li>Channel恢复可写时，排空队列</li>
 * </ul>
 *
 * <p>溢出策略：
 * <ul>
 *   <li>DROP_OLDEST: 丢弃最旧消息</li>
 *   <li>DROP_NEWEST: 丢弃新消息</li>
 *   <li>DISCONNECT: 断开慢消费者</li>
 *   <li>DROP_OLDEST_THEN_DISCONNECT: 先丢弃旧，超极限后断开</li>
 * </ul>
 */
public class OutboundBufferHandler extends ChannelDuplexHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboundBufferHandler.class);
    
    private final int maxQueueSize;
    private final int disconnectThreshold;
    private final OverflowStrategy overflowStrategy;
    private final ConcurrentLinkedQueue<Packet> pendingQueue;
    private final LongAdder dropCount;
    private final LongAdder enqueueCount;
    private final LongAdder drainCount;
    private final LongAdder disconnectCount;
    
    private volatile boolean metricsRegistered = false;
    
    public OutboundBufferHandler(FlowControlConfig config) {
        this.maxQueueSize = config.getMaxQueueSize();
        this.disconnectThreshold = config.getDisconnectThreshold();
        this.overflowStrategy = config.getOutboundOverflowStrategy();
        this.pendingQueue = new ConcurrentLinkedQueue<>();
        this.dropCount = new LongAdder();
        this.enqueueCount = new LongAdder();
        this.drainCount = new LongAdder();
        this.disconnectCount = new LongAdder();
    }
    
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!configuredForOutbound(msg)) {
            ctx.write(msg, promise);
            return;
        }
        
        if (ctx.channel().isWritable() && pendingQueue.isEmpty()) {
            ctx.write(msg, promise);
            return;
        }
        
        Packet packet = extractPacket(msg);
        if (packet == null) {
            ctx.write(msg, promise);
            return;
        }
        
        if (ctx.channel().isWritable()) {
            drainQueue(ctx);
            ctx.write(msg, promise);
            return;
        }
        
        if (pendingQueue.size() >= maxQueueSize) {
            handleOverflow(ctx, msg, promise, packet);
        } else {
            enqueuePacket(packet, promise);
            ReferenceCountUtil.release(msg);
        }
    }
    
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isWritable()) {
            drainQueue(ctx);
        }
        ctx.fireChannelWritabilityChanged();
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Packet packet;
        while ((packet = pendingQueue.poll()) != null) {
            packet.release();
        }
        ctx.fireChannelInactive();
    }
    
    private boolean configuredForOutbound(Object msg) {
        return msg instanceof BinaryWebSocketFrame;
    }
    
    private Packet extractPacket(Object msg) {
        if (msg instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame frame = (BinaryWebSocketFrame) msg;
            ByteBuf content = frame.content();
            
            if (content.readableBytes() < 27) {
                return null;
            }
            
            return Packet.decodeFromByteBuf(content);
        }
        return null;
    }
    
    private void drainQueue(ChannelHandlerContext ctx) {
        Packet packet;
        int drained = 0;
        
        while (ctx.channel().isWritable() && (packet = pendingQueue.poll()) != null) {
            ByteBuf frame = PacketWriter.writeToPooledDirectBuffer(packet, ctx.alloc());
            ctx.write(new BinaryWebSocketFrame(frame));
            packet.release();
            drained++;
        }
        
        if (drained > 0) {
            ctx.flush();
            drainCount.add(drained);
            LOGGER.debug("Drained {} packets from queue, remaining={}", drained, pendingQueue.size());
        }
    }
    
    private void handleOverflow(ChannelHandlerContext ctx, Object msg, ChannelPromise promise, 
                                Packet packet) {
        dropCount.increment();
        
        switch (overflowStrategy) {
            case DROP_OLDEST:
                dropOldestAndEnqueue(packet);
                ReferenceCountUtil.release(msg);
                promise.setSuccess();
                break;
                
            case DROP_NEWEST:
                ReferenceCountUtil.release(msg);
                packet.release();
                promise.setSuccess();
                break;
                
            case DISCONNECT:
                ReferenceCountUtil.release(msg);
                packet.release();
                disconnectCount.increment();
                ctx.close();
                promise.setSuccess();
                break;
                
            case DROP_OLDEST_THEN_DISCONNECT:
                if (pendingQueue.size() >= disconnectThreshold) {
                    ReferenceCountUtil.release(msg);
                    packet.release();
                    disconnectCount.increment();
                    ctx.close();
                } else {
                    dropOldestAndEnqueue(packet);
                }
                promise.setSuccess();
                break;
        }
        
        LOGGER.warn("Outbound overflow: strategy={}, queueSize={}, dropCount={}, disconnectCount={}",
            overflowStrategy, pendingQueue.size(), dropCount.sum(), disconnectCount.sum());
    }
    
    private void dropOldestAndEnqueue(Packet newPacket) {
        Packet oldest = pendingQueue.poll();
        if (oldest != null) {
            oldest.release();
        }
        pendingQueue.offer(newPacket);
    }
    
    private void enqueuePacket(Packet packet, ChannelPromise promise) {
        pendingQueue.offer(packet);
        enqueueCount.increment();
        promise.setSuccess();
        
        LOGGER.debug("Enqueued packet: queueSize={}", pendingQueue.size());
    }
    
    public int getQueueSize() {
        return pendingQueue.size();
    }
    
    public long getDropCount() {
        return dropCount.sum();
    }
    
    public long getEnqueueCount() {
        return enqueueCount.sum();
    }
    
    public long getDrainCount() {
        return drainCount.sum();
    }
    
    public long getDisconnectCount() {
        return disconnectCount.sum();
    }
}
```

- [ ] **Step 2: 在Packet中添加decodeFromByteBuf方法**

如果Packet类没有此方法，需要添加。先检查Packet类结构。

- [ ] **Step 3: 编译验证**

Run: `mvnd compile -pl jwsch-srv -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/flowcontrol/OutboundBufferHandler.java
git commit -m "feat(flowcontrol): add OutboundBufferHandler for L3 overflow control"
```

---

### Task 2.2: 集成OutboundBufferHandler到WebSocketServer

**Files:**
- Modify: `jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/server/websocket/WebSocketServer.java`

- [ ] **Step 1: 在WebSocketServer的pipeline中添加OutboundBufferHandler**

在WebSocketServer.java的initChannel方法中，在`flushConsolidation`后、`webSocketProtocol`前添加：

```java
import cn.itcraft.jwsch.srv.flowcontrol.OutboundBufferHandler;
import cn.itcraft.jwsch.common.flowcontrol.FlowControlConfig;

// 在类中添加字段
private final FlowControlConfig flowControlConfig;

// 修改构造函数
public WebSocketServer(WebSocketConfig config, PacketRouter packetRouter, 
                      ServerMetrics serverMetrics, int slowQueryThresholdMs) {
    this.config = config;
    this.packetRouter = packetRouter;
    this.serverMetrics = serverMetrics;
    this.slowQueryThresholdMs = slowQueryThresholdMs;
    this.sslContext = initSslContext(config.getSslConfig());
    this.ownsEventLoop = true;
    this.flowControlConfig = FlowControlConfig.defaultConfig();
}

// 在initChannel的pipeline中添加（在flushConsolidation后）
if (flowControlConfig.isOutboundEnabled()) {
    pipeline.addLast("outboundBuffer", new OutboundBufferHandler(flowControlConfig));
}
```

- [ ] **Step 2: 编译验证**

Run: `mvnd compile -pl jwsch-srv -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/server/websocket/WebSocketServer.java
git commit -m "feat(flowcontrol): integrate OutboundBufferHandler into WebSocketServer pipeline"
```

---

## Phase 3: L2 背压管理

### Task 3.1: 修复BackpressureManager（磁滞回线）

**Files:**
- Modify: `jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/router/BackpressureManager.java`

- [ ] **Step 1: 修改阈值常量**

将：
```java
private static final double NON_WRITABLE_THRESHOLD = 0.5;
private static final long RELEASE_COOLDOWN_MS = 100;
```

改为：
```java
private static final double TRIGGER_THRESHOLD = 0.2;
private static final double RELEASE_THRESHOLD = 0.05;
private static final long RELEASE_COOLDOWN_MS = 500;
```

- [ ] **Step 2: 添加releaseThreshold字段和磁滞逻辑**

添加字段：
```java
private volatile long lastReleaseTimeMs = 0;
```

修改`checkAndDisableAutoRead`：
```java
private void checkAndDisableAutoRead() {
    int total = frontendChannels.size();
    if (total == 0) {
        return;
    }
    
    int nonWritable = nonWritableCount.get();
    double ratio = nonWritable / (double) total;
    
    if (ratio >= TRIGGER_THRESHOLD && !autoReadDisabled.get()) {
        disableAutoReadOnAllTcpChannels();
    }
}
```

修改`tryReleaseAutoRead`：
```java
private void tryReleaseAutoRead() {
    if (!autoReadDisabled.get()) {
        return;
    }
    
    int nonWritable = nonWritableCount.get();
    int total = frontendChannels.size();
    
    if (total == 0) {
        return;
    }
    
    double ratio = nonWritable / (double) total;
    
    if (ratio > RELEASE_THRESHOLD) {
        LOGGER.debug("Cannot release: ratio={} > threshold={}", ratio, RELEASE_THRESHOLD);
        scheduledEventLoop = null;
        return;
    }
    
    long elapsed = System.currentTimeMillis() - lastActivateTime;
    if (elapsed < RELEASE_COOLDOWN_MS) {
        LOGGER.debug("Cannot release: cooldown not elapsed ({}ms < {}ms)", 
            elapsed, RELEASE_COOLDOWN_MS);
        return;
    }
    
    if (autoReadDisabled.compareAndSet(true, false)) {
        lastReleaseTimeMs = System.currentTimeMillis();
        
        int enabledCount = 0;
        for (Channel tcpChannel : tcpChannels) {
            if (tcpChannel.isActive() && !tcpChannel.config().isAutoRead()) {
                tcpChannel.config().setAutoRead(true);
                enabledCount++;
            }
        }
        
        LOGGER.info("Backpressure released: enabled AUTO_READ on {} TCP channels, " +
            "nonWritable={}/{}, cooldown={}ms", enabledCount, nonWritable, total, elapsed);
    }
    
    scheduledEventLoop = null;
}
```

- [ ] **Step 3: 重新启用背压触发**

在`onFrontendWritabilityChanged`中取消注释：
```java
public void onFrontendWritabilityChanged(Channel channel) {
    boolean writable = channel.isWritable();
    
    if (writable) {
        int count = nonWritableCount.decrementAndGet();
        LOGGER.debug("Frontend writable: nonWritableCount={}", count);
        scheduleReleaseCheck(channel.eventLoop());
    } else {
        int count = nonWritableCount.incrementAndGet();
        LOGGER.debug("Frontend non-writable: nonWritableCount={}", count);
        checkAndDisableAutoRead();  // 重新启用
    }
}
```

- [ ] **Step 4: 编译验证**

Run: `mvnd compile -pl jwsch-srv -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/router/BackpressureManager.java
git commit -m "fix(backpressure): add hysteresis loop and re-enable backpressure trigger

- Change trigger threshold from 50% to 20%
- Add release threshold at 5% for hysteresis
- Increase cooldown from 100ms to 500ms
- Re-enable checkAndDisableAutoRead() call"
```

---

### Task 3.2: TopicBackpressureState

**Files:**
- Create: `jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/flowcontrol/TopicBackpressureState.java`

- [ ] **Step 1: 创建TopicBackpressureState**

```java
package cn.itcraft.jwsch.srv.flowcontrol;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Topic背压状态。
 *
 * <p>跟踪单个Topic的订阅者可写状态，用于per-topic背压隔离。
 */
final class TopicBackpressureState {
    
    private final long topicHash;
    private final Set<String> nonWritableSubscribers;
    private final AtomicInteger totalSubscribers;
    
    private volatile boolean backpressured = false;
    private volatile long lastStateChangeTime = 0;
    
    private final double triggerThreshold;
    private final double releaseThreshold;
    
    TopicBackpressureState(long topicHash, double triggerThreshold, double releaseThreshold) {
        this.topicHash = topicHash;
        this.nonWritableSubscribers = ConcurrentHashMap.newKeySet();
        this.totalSubscribers = new AtomicInteger();
        this.triggerThreshold = triggerThreshold;
        this.releaseThreshold = releaseThreshold;
    }
    
    void incrementSubscribers() {
        totalSubscribers.incrementAndGet();
    }
    
    void decrementSubscribers() {
        totalSubscribers.decrementAndGet();
    }
    
    void updateSubscriberState(String connectionId, boolean writable) {
        if (writable) {
            nonWritableSubscribers.remove(connectionId);
            checkAndReleaseBackpressure();
        } else {
            nonWritableSubscribers.add(connectionId);
            checkAndActivateBackpressure();
        }
    }
    
    void removeSubscriber(String connectionId) {
        nonWritableSubscribers.remove(connectionId);
    }
    
    boolean isBackpressured() {
        return backpressured;
    }
    
    int getNonWritableCount() {
        return nonWritableSubscribers.size();
    }
    
    int getTotalSubscribers() {
        return totalSubscribers.get();
    }
    
    long getTopicHash() {
        return topicHash;
    }
    
    private void checkAndActivateBackpressure() {
        int total = totalSubscribers.get();
        if (total == 0) {
            return;
        }
        
        double ratio = (double) nonWritableSubscribers.size() / total;
        
        if (ratio >= triggerThreshold && !backpressured) {
            backpressured = true;
            lastStateChangeTime = System.currentTimeMillis();
        }
    }
    
    private void checkAndReleaseBackpressure() {
        int total = totalSubscribers.get();
        if (total == 0) {
            backpressured = false;
            return;
        }
        
        double ratio = (double) nonWritableSubscribers.size() / total;
        
        if (ratio <= releaseThreshold && backpressured) {
            backpressured = false;
            lastStateChangeTime = System.currentTimeMillis();
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvnd compile -pl jwsch-srv -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/flowcontrol/TopicBackpressureState.java
git commit -m "feat(flowcontrol): add TopicBackpressureState for per-topic backpressure tracking"
```

---

### Task 3.3: TopicBackpressureManager

**Files:**
- Create: `jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/flowcontrol/TopicBackpressureManager.java`

- [ ] **Step 1: 创建TopicBackpressureManager**

```java
package cn.itcraft.jwsch.srv.flowcontrol;

import cn.itcraft.jwsch.common.flowcontrol.FlowControlConfig;
import cn.itcraft.jwsch.srv.router.TopicSubscription;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LongAdder;

/**
 * Topic级别背压管理器。
 *
 * <p>实现per-topic背压隔离，一个慢Topic不拖垮其他Topic。
 * 
 * <p>工作原理：
 * <pre>
 * 当某Topic的订阅者不可写比例超过阈值时：
 * 1. 标记该Topic为背压状态
 * 2. PacketRouter.broadcastToTopic() 检查背压状态
 * 3. 背压Topic的消息被丢弃，其他Topic正常投递
 * </pre>
 */
public final class TopicBackpressureManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicBackpressureManager.class);
    
    private final ConcurrentHashMap<Long, TopicBackpressureState> topicStates;
    private final TopicSubscription topicSubscription;
    private final double triggerThreshold;
    private final double releaseThreshold;
    private final LongAdder backpressureDropCount;
    
    public TopicBackpressureManager(TopicSubscription topicSubscription, FlowControlConfig config) {
        this.topicStates = new ConcurrentHashMap<>();
        this.topicSubscription = topicSubscription;
        this.triggerThreshold = config.getTopicTriggerThreshold();
        this.releaseThreshold = config.getTopicReleaseThreshold();
        this.backpressureDropCount = new LongAdder();
    }
    
    public boolean isTopicBackpressured(long topicHash) {
        TopicBackpressureState state = topicStates.get(topicHash);
        return state != null && state.isBackpressured();
    }
    
    public boolean isTopicBackpressured(String topic) {
        return isTopicBackpressured(hashTopic(topic));
    }
    
    public void onSubscriberWritabilityChanged(long topicHash, String connectionId, boolean writable) {
        topicStates.compute(topicHash, (k, state) -> {
            if (state == null) {
                state = new TopicBackpressureState(topicHash, triggerThreshold, releaseThreshold);
            }
            state.updateSubscriberState(connectionId, writable);
            
            if (state.isBackpressured()) {
                LOGGER.info("Topic backpressure activated: topicHash={}, nonWritable={}/{}", 
                    topicHash, state.getNonWritableCount(), state.getTotalSubscribers());
            }
            
            return state;
        });
    }
    
    public void onSubscriberWritabilityChanged(String topic, String connectionId, boolean writable) {
        onSubscriberWritabilityChanged(hashTopic(topic), connectionId, writable);
    }
    
    public void incrementTopicDrop(long topicHash) {
        backpressureDropCount.increment();
        LOGGER.debug("Topic backpressure drop: topicHash={}", topicHash);
    }
    
    public long getBackpressureDropCount() {
        return backpressureDropCount.sum();
    }
    
    public int getTopicStateCount() {
        return topicStates.size();
    }
    
    public void clear() {
        topicStates.clear();
        backpressureDropCount.reset();
    }
    
    private long hashTopic(String topic) {
        if (topic == null) {
            return 0;
        }
        return topic.hashCode();
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvnd compile -pl jwsch-srv -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/flowcontrol/TopicBackpressureManager.java
git commit -m "feat(flowcontrol): add TopicBackpressureManager for per-topic isolation"
```

---

### Task 3.4: 集成TopicBackpressureManager到PacketRouter

**Files:**
- Modify: `jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/router/PacketRouter.java`

- [ ] **Step 1: 添加TopicBackpressureManager字段和初始化**

添加import和字段：
```java
import cn.itcraft.jwsch.common.flowcontrol.FlowControlConfig;
import cn.itcraft.jwsch.srv.flowcontrol.TopicBackpressureManager;

// 添加字段
private volatile TopicBackpressureManager topicBackpressureManager;
```

添加setter：
```java
public void setTopicBackpressureManager(TopicBackpressureManager topicBackpressureManager) {
    this.topicBackpressureManager = topicBackpressureManager;
}

public TopicBackpressureManager getTopicBackpressureManager() {
    return topicBackpressureManager;
}
```

- [ ] **Step 2: 在broadcastToTopic中添加背压检查**

在`broadcastToTopic`方法开头添加：
```java
public void broadcastToTopic(String topic, Packet packet) {
    // L2 Topic背压检查
    if (topicBackpressureManager != null && topicBackpressureManager.isTopicBackpressured(topic)) {
        topicBackpressureManager.incrementTopicDrop(hashTopic(topic));
        LOGGER.debug("Topic backpressure drop: topic={}", topic);
        return;
    }
    
    // 原有逻辑...
}

private long hashTopic(String topic) {
    return topic != null ? topic.hashCode() : 0;
}
```

- [ ] **Step 3: 编译验证**

Run: `mvnd compile -pl jwsch-srv -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/router/PacketRouter.java
git commit -m "feat(flowcontrol): integrate TopicBackpressureManager into PacketRouter"
```

---

### Task 3.5: WebSocketHandler上报Topic背压

**Files:**
- Modify: `jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/server/websocket/WebSocketHandler.java`

- [ ] **Step 1: 在channelWritabilityChanged中上报Topic背压**

修改`channelWritabilityChanged`：
```java
@Override
public void channelWritabilityChanged(ChannelHandlerContext ctx) {
    boolean writable = ctx.channel().isWritable();
    packetRouter.getBackpressureManager().onFrontendWritabilityChanged(ctx.channel());
    
    // 上报Topic背压状态
    TopicBackpressureManager topicBackpressureManager = packetRouter.getTopicBackpressureManager();
    if (topicBackpressureManager != null && connectionId != null) {
        TopicSubscription topicSubscription = packetRouter.getTopicSubscription();
        Set<String> topics = topicSubscription.getTopicsByConnectionId(connectionId);
        if (topics != null) {
            String connectionIdStr = String.valueOf(connectionId);
            for (String topic : topics) {
                topicBackpressureManager.onSubscriberWritabilityChanged(topic, connectionIdStr, writable);
            }
        }
    }
    
    if (!writable) {
        LOGGER.debug("Frontend channel non-writable (backpressure): connectionId={}", connectionId);
    } else {
        LOGGER.debug("Frontend channel writable again: connectionId={}", connectionId);
    }
}
```

需要添加import：
```java
import cn.itcraft.jwsch.srv.flowcontrol.TopicBackpressureManager;
import java.util.Set;
```

- [ ] **Step 2: 在TopicSubscription中添加getTopicsByConnectionId方法**

如果TopicSubscription没有此方法，需要添加。

- [ ] **Step 3: 编译验证**

Run: `mvnd compile -pl jwsch-srv -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/server/websocket/WebSocketHandler.java
git commit -m "feat(flowcontrol): report topic backpressure state from WebSocketHandler"
```

---

## Phase 4: L1 入口限速

### Task 4.1: InboundRateLimiterHandler

**Files:**
- Create: `jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/flowcontrol/InboundRateLimiterHandler.java`

- [ ] **Step 1: 创建InboundRateLimiterHandler**

```java
package cn.itcraft.jwsch.srv.flowcontrol;

import cn.itcraft.jwsch.common.exception.ErrorCode;
import cn.itcraft.jwsch.common.flowcontrol.FlowControlConfig;
import cn.itcraft.jwsch.common.flowcontrol.OverflowStrategy;
import cn.itcraft.jwsch.common.flowcontrol.RateLimiter;
import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketHeader;
import cn.itcraft.jwsch.common.protocol.ProtocolConsts;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LongAdder;

/**
 * 入站限速Handler。
 *
 * <p>实现L1入口限速，防止pub打爆server：
 * <ul>
 *   <li>使用令牌桶算法限制每个连接的消息速率</li>
 *   <li>超速时根据策略丢弃或回复错误码</li>
 * </ul>
 */
public class InboundRateLimiterHandler extends ChannelInboundHandlerAdapter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundRateLimiterHandler.class);
    
    private final RateLimiter rateLimiter;
    private final OverflowStrategy overflowStrategy;
    private final LongAdder acceptedCount;
    private final LongAdder rejectedCount;
    
    public InboundRateLimiterHandler(FlowControlConfig config) {
        this.rateLimiter = RateLimiter.create(
            config.getMaxTokensPerSecond(),
            config.getBurstSize()
        );
        this.overflowStrategy = config.getInboundOverflowStrategy();
        this.acceptedCount = new LongAdder();
        this.rejectedCount = new LongAdder();
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof Packet)) {
            ctx.fireChannelRead(msg);
            return;
        }
        
        Packet packet = (Packet) msg;
        
        if (!rateLimiter.tryAcquire()) {
            rejectedCount.increment();
            
            if (overflowStrategy == OverflowStrategy.DROP_NEWEST || 
                overflowStrategy == OverflowStrategy.DROP_OLDEST) {
                LOGGER.warn("Inbound rate limited: dropping packet, rejectedCount={}", rejectedCount.sum());
                ReferenceCountUtil.release(msg);
                return;
            }
            
            sendRateLimitResponse(ctx, packet);
            ReferenceCountUtil.release(msg);
            return;
        }
        
        acceptedCount.increment();
        ctx.fireChannelRead(msg);
    }
    
    private void sendRateLimitResponse(ChannelHandlerContext ctx, Packet packet) {
        ByteBuf buf = ctx.alloc().buffer(ProtocolConsts.FIXED_HEADER_LENGTH);
        buf.writeByte(ProtocolConsts.MAGIC[0]);
        buf.writeByte(ProtocolConsts.MAGIC[1]);
        buf.writeShort(ProtocolConsts.FIXED_HEADER_LENGTH);
        buf.writeInt(0);
        buf.writeByte(Command.RESPONSE);
        buf.writeShort(ErrorCode.RATE_LIMITED.getCode());
        buf.writeLong(packet.getHeader().getTargetId());
        buf.writeLong(packet.getHeader().getSourceId());
        
        ctx.writeAndFlush(buf);
        
        LOGGER.warn("Sent rate limit response: connectionId={}", packet.getHeader().getSourceId());
    }
    
    public long getAcceptedCount() {
        return acceptedCount.sum();
    }
    
    public long getRejectedCount() {
        return rejectedCount.sum();
    }
    
    public long getAvailableTokens() {
        return rateLimiter.getAvailableTokens();
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvnd compile -pl jwsch-srv -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/flowcontrol/InboundRateLimiterHandler.java
git commit -m "feat(flowcontrol): add InboundRateLimiterHandler for L1 rate limiting"
```

---

### Task 4.2: 集成InboundRateLimiterHandler到TcpServerInitializer

**Files:**
- Modify: `jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/server/tcp/TcpServerInitializer.java`

- [ ] **Step 1: 添加FlowControlConfig和InboundRateLimiterHandler**

添加import和字段：
```java
import cn.itcraft.jwsch.common.flowcontrol.FlowControlConfig;
import cn.itcraft.jwsch.srv.flowcontrol.InboundRateLimiterHandler;

// 添加字段
private final FlowControlConfig flowControlConfig;
```

修改构造函数：
```java
public TcpServerInitializer(PacketRouter packetRouter) {
    this(packetRouter, null, FlowControlConfig.defaultConfig());
}

public TcpServerInitializer(PacketRouter packetRouter, ServerMetrics serverMetrics) {
    this(packetRouter, serverMetrics, FlowControlConfig.defaultConfig());
}

public TcpServerInitializer(PacketRouter packetRouter, ServerMetrics serverMetrics, 
                           FlowControlConfig flowControlConfig) {
    this.packetRouter = packetRouter;
    this.serverMetrics = serverMetrics;
    this.flowControlConfig = flowControlConfig != null ? flowControlConfig : FlowControlConfig.defaultConfig();
}
```

修改initChannel：
```java
@Override
protected void initChannel(SocketChannel ch) {
    ChannelPipeline pipeline = ch.pipeline();
    
    pipeline.addLast("idleState", new IdleStateHandler(
        READER_IDLE_TIME_SECONDS, 
        WRITER_IDLE_TIME_SECONDS, 
        ALL_IDLE_TIME_SECONDS, 
        TimeUnit.SECONDS));
    pipeline.addLast("decoder", new PacketDecoder());
    pipeline.addLast("encoder", new PacketEncoder());
    
    if (flowControlConfig.isInboundEnabled()) {
        pipeline.addLast("rateLimiter", new InboundRateLimiterHandler(flowControlConfig));
    }
    
    pipeline.addLast("handler", new TcpServerHandler(packetRouter, serverMetrics));
}
```

- [ ] **Step 2: 编译验证**

Run: `mvnd compile -pl jwsch-srv -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/server/tcp/TcpServerInitializer.java
git commit -m "feat(flowcontrol): integrate InboundRateLimiterHandler into TcpServerInitializer"
```

---

## Phase 5: FlowControlMetrics监控

### Task 5.1: FlowControlMetrics

**Files:**
- Create: `jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/flowcontrol/FlowControlMetrics.java`

- [ ] **Step 1: 创建FlowControlMetrics**

```java
package cn.itcraft.jwsch.srv.flowcontrol;

import cn.itcraft.jwsch.srv.router.BackpressureManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 流控指标统一注册。
 *
 * <p>注册三层流控指标到Micrometer：
 * <ul>
 *   <li>L1: 入口限速指标</li>
 *   <li>L2: 背压传导指标</li>
 *   <li>L3: 堆积溢出指标</li>
 * </ul>
 */
public final class FlowControlMetrics {
    
    private final MeterRegistry registry;
    private final BackpressureManager backpressureManager;
    private final TopicBackpressureManager topicBackpressureManager;
    
    private final Map<String, InboundMetrics> inboundMetricsMap = new ConcurrentHashMap<>();
    private final Map<String, OutboundMetrics> outboundMetricsMap = new ConcurrentHashMap<>();
    
    private Counter backpressureActivateCounter;
    private Counter backpressureDeactivateCounter;
    private AtomicLong backpressureActivateCount = new AtomicLong();
    private AtomicLong backpressureDeactivateCount = new AtomicLong();
    
    public FlowControlMetrics(MeterRegistry registry, 
                              BackpressureManager backpressureManager,
                              TopicBackpressureManager topicBackpressureManager) {
        this.registry = registry;
        this.backpressureManager = backpressureManager;
        this.topicBackpressureManager = topicBackpressureManager;
        
        initL2Metrics();
    }
    
    private void initL2Metrics() {
        if (registry == null) {
            return;
        }
        
        backpressureActivateCounter = Counter.builder("jwsch.backpressure.global.activate")
            .description("Global backpressure activation count")
            .register(registry);
        
        backpressureDeactivateCounter = Counter.builder("jwsch.backpressure.global.deactivate")
            .description("Global backpressure deactivation count")
            .register(registry);
        
        if (backpressureManager != null) {
            Gauge.builder("jwsch.backpressure.global.active", backpressureManager, 
                bm -> bm.isAutoReadDisabled() ? 1 : 0)
                .description("Global backpressure active status")
                .register(registry);
            
            Gauge.builder("jwsch.backpressure.global.nonWritable.ratio", backpressureManager, bm -> {
                    int total = bm.getFrontendChannelCount();
                    if (total == 0) return 0.0;
                    return (double) bm.getNonWritableCount() / total;
                })
                .description("Global non-writable frontend ratio")
                .register(registry);
            
            Gauge.builder("jwsch.connection.backend.active", backpressureManager, 
                BackpressureManager::getTcpChannelCount)
                .description("Active TCP backend connections")
                .register(registry);
            
            Gauge.builder("jwsch.connection.frontend.active", backpressureManager,
                BackpressureManager::getFrontendChannelCount)
                .description("Active WebSocket frontend connections")
                .register(registry);
            
            Gauge.builder("jwsch.connection.frontend.nonWritable", backpressureManager,
                BackpressureManager::getNonWritableCount)
                .description("Non-writable frontend connections")
                .register(registry);
        }
        
        if (topicBackpressureManager != null) {
            Gauge.builder("jwsch.backpressure.topic.drop.total", topicBackpressureManager,
                TopicBackpressureManager::getBackpressureDropCount)
                .description("Total topic backpressure drops")
                .register(registry);
        }
    }
    
    public void registerInboundMetrics(String connectionId, InboundRateLimiterHandler handler) {
        if (registry == null || connectionId == null || handler == null) {
            return;
        }
        
        InboundMetrics metrics = new InboundMetrics(connectionId, handler, registry);
        inboundMetricsMap.put(connectionId, metrics);
    }
    
    public void registerOutboundMetrics(String connectionId, OutboundBufferHandler handler) {
        if (registry == null || connectionId == null || handler == null) {
            return;
        }
        
        OutboundMetrics metrics = new OutboundMetrics(connectionId, handler, registry);
        outboundMetricsMap.put(connectionId, metrics);
    }
    
    public void recordBackpressureActivate() {
        backpressureActivateCount.incrementAndGet();
        if (backpressureActivateCounter != null) {
            backpressureActivateCounter.increment();
        }
    }
    
    public void recordBackpressureDeactivate() {
        backpressureDeactivateCount.incrementAndGet();
        if (backpressureDeactivateCounter != null) {
            backpressureDeactivateCounter.increment();
        }
    }
    
    public long getBackpressureActivateCount() {
        return backpressureActivateCount.get();
    }
    
    public long getBackpressureDeactivateCount() {
        return backpressureDeactivateCount.get();
    }
    
    private static final class InboundMetrics {
        final Counter acceptedCounter;
        final Counter rejectedCounter;
        
        InboundMetrics(String connectionId, InboundRateLimiterHandler handler, MeterRegistry registry) {
            Tags tags = Tags.of("connectionId", connectionId);
            
            acceptedCounter = Counter.builder("jwsch.inbound.accepted")
                .tags(tags)
                .register(registry);
            
            rejectedCounter = Counter.builder("jwsch.inbound.rejected")
                .tags(tags)
                .register(registry);
            
            Gauge.builder("jwsch.inbound.available.tokens", handler, 
                InboundRateLimiterHandler::getAvailableTokens)
                .tags(tags)
                .register(registry);
        }
    }
    
    private static final class OutboundMetrics {
        OutboundMetrics(String connectionId, OutboundBufferHandler handler, MeterRegistry registry) {
            Tags tags = Tags.of("connectionId", connectionId);
            
            Gauge.builder("jwsch.outbound.queue.size", handler, OutboundBufferHandler::getQueueSize)
                .tags(tags)
                .register(registry);
            
            Gauge.builder("jwsch.outbound.drop.total", handler, OutboundBufferHandler::getDropCount)
                .tags(tags)
                .register(registry);
            
            Gauge.builder("jwsch.outbound.enqueue.total", handler, OutboundBufferHandler::getEnqueueCount)
                .tags(tags)
                .register(registry);
            
            Gauge.builder("jwsch.outbound.drain.total", handler, OutboundBufferHandler::getDrainCount)
                .tags(tags)
                .register(registry);
            
            Gauge.builder("jwsch.outbound.disconnect.total", handler, OutboundBufferHandler::getDisconnectCount)
                .tags(tags)
                .register(registry);
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvnd compile -pl jwsch-srv -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/flowcontrol/FlowControlMetrics.java
git commit -m "feat(flowcontrol): add FlowControlMetrics for Micrometer integration"
```

---

### Task 5.2: 在BackpressureManager中触发Metrics

**Files:**
- Modify: `jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/router/BackpressureManager.java`

- [ ] **Step 1: 添加FlowControlMetrics字段**

添加：
```java
import cn.itcraft.jwsch.srv.flowcontrol.FlowControlMetrics;

private volatile FlowControlMetrics flowControlMetrics;

public void setFlowControlMetrics(FlowControlMetrics flowControlMetrics) {
    this.flowControlMetrics = flowControlMetrics;
}
```

- [ ] **Step 2: 在activate和deactivate时记录Metrics**

在`disableAutoReadOnAllTcpChannels`末尾添加：
```java
if (flowControlMetrics != null) {
    flowControlMetrics.recordBackpressureActivate();
}
```

在`tryReleaseAutoRead`的`compareAndSet`块末尾添加：
```java
if (flowControlMetrics != null) {
    flowControlMetrics.recordBackpressureDeactivate();
}
```

- [ ] **Step 3: 编译验证**

Run: `mvnd compile -pl jwsch-srv -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add jwsch-srv/src/main/java/cn/itcraft/jwsch/srv/router/BackpressureManager.java
git commit -m "feat(flowcontrol): integrate FlowControlMetrics into BackpressureManager"
```

---

## Phase 6: 集成测试验证

### Task 6.1: 编写集成测试

**Files:**
- Create: `jwsch-test/src/test/java/cn/itcraft/jwsch/srv/flowcontrol/FlowControlIntegrationTest.java`

- [ ] **Step 1: 创建集成测试**

```java
package cn.itcraft.jwsch.srv.flowcontrol;

import cn.itcraft.jwsch.common.flowcontrol.FlowControlConfig;
import cn.itcraft.jwsch.common.flowcontrol.OverflowStrategy;
import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketHeader;
import cn.itcraft.jwsch.srv.JwschServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 流控集成测试。
 *
 * <p>验证三层流控机制：
 * <ul>
 *   <li>L1: 入口限速触发</li>
 *   <li>L2: 背压传导触发</li>
 *   <li>L3: 堆积溢出触发</li>
 * </ul>
 */
class FlowControlIntegrationTest {
    
    private static final int TEST_PORT = 39090;
    private static final int TEST_TCP_PORT = 39091;
    
    private JwschServer server;
    private SimpleMeterRegistry meterRegistry;
    private FlowControlMetrics flowControlMetrics;
    
    @BeforeEach
    void setUp() throws Exception {
        meterRegistry = new SimpleMeterRegistry();
        
        FlowControlConfig config = FlowControlConfig.builder()
            .inboundEnabled(true)
            .maxTokensPerSecond(100)
            .burstSize(120)
            .inboundOverflowStrategy(OverflowStrategy.DROP_NEWEST)
            .outboundEnabled(true)
            .maxQueueSize(10)
            .outboundOverflowStrategy(OverflowStrategy.DROP_OLDEST)
            .build();
        
        server = new JwschServer();
        // TODO: 配置server使用测试端口和流控配置
    }
    
    @AfterEach
    void tearDown() {
        if (server != null) {
            server.shutdown();
        }
    }
    
    @Test
    @Timeout(30)
    void testL1_rateLimiting_shouldDropPacketsWhenExceedingRate() throws Exception {
        // TODO: 发送超过限速的消息，验证rejected计数
    }
    
    @Test
    @Timeout(30)
    void testL2_backpressure_shouldTriggerWhenSubscribersSlow() throws Exception {
        // TODO: 创建慢消费者，验证背压触发
    }
    
    @Test
    @Timeout(30)
    void testL3_overflow_shouldDropWhenQueueFull() throws Exception {
        // TODO: 创建慢消费者导致队列满，验证丢弃
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvnd compile -pl jwsch-test -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add jwsch-test/src/test/java/cn/itcraft/jwsch/srv/flowcontrol/FlowControlIntegrationTest.java
git commit -m "test(flowcontrol): add integration test skeleton for flow control"
```

---

## Phase 7: 最终打包验证

### Task 7.1: 全量编译和测试

- [ ] **Step 1: 清理并编译**

Run: `mvnd clean compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 运行测试**

Run: `mvnd test -pl jwsch-test -Dtest=AllTests -q`
Expected: Tests pass

- [ ] **Step 3: 打包**

Run: `mvnd package -Dmaven.test.skip=true -q`
Expected: BUILD SUCCESS

---

### Task 7.2: 提交全部代码

```bash
git add .
git commit -m "feat(flowcontrol): complete three-layer flow control implementation

- L1: InboundRateLimiterHandler with token bucket rate limiting
- L2: BackpressureManager with hysteresis loop + TopicBackpressureManager for per-topic isolation
- L3: OutboundBufferHandler with configurable overflow strategies
- FlowControlMetrics for Micrometer integration
- FlowControlConfig for centralized configuration"
```

---

## Summary

| Phase | Tasks | Status |
|-------|-------|--------|
| Phase 1 | 基础设施 | Pending |
| Phase 2 | L3 OutboundBufferHandler | Pending |
| Phase 3 | L2 背压管理 | Pending |
| Phase 4 | L1 入口限速 | Pending |
| Phase 5 | FlowControlMetrics监控 | Pending |
| Phase 6 | 集成测试验证 | Pending |
| Phase 7 | 最终打包验证 | Pending |