# 流量控制与堆积控制设计

> 日期：2026-07-14
> 状态：已确认，待实施

## 1. 背景与问题

jwsch 作为前后端消息转发中间件，当前缺乏系统性的流量控制机制：

| 问题 | 严重度 | 说明 |
|------|--------|------|
| 无pub侧限速 | HIGH | 单pub可打爆server，无入口防护 |
| BackpressureManager已禁用 | HIGH | 核心背压机制因丢包bug被注释，sub堆积无法传导到pub |
| 无per-topic背压隔离 | MEDIUM | 一个慢topic拖垮全局 |
| 无sub侧堆积溢出策略 | HIGH | 慢sub导致ChannelOutboundBuffer无限增长，OOM风险 |
| 无流控可观测性 | MEDIUM | 堆积/限速/丢弃无指标无日志，问题不可见 |

扇出放大场景：1个pub × 100个sub × 10K msg/s × 1KB = 1GB/s出站，8MB/连接 × 100 = 800MB潜在缓冲。

## 2. 设计目标

- **L1 防洪**：入口限速，防止单pub打爆server
- **L2 传导**：sub堆积信号沿链路反向传导到pub，端到端背压
- **L3 兜底**：慢sub最终保护，避免OOM
- **可观测**：全链路指标、日志、告警

## 3. 整体架构

```
Pub(TCP) ──► [L1 入口限速] ──► PacketRouter ──► [L2 背压传导] ──► [L3 堆积溢出] ──► Sub(WS)
              │                                  │                    │
              │ Per-connection                   │ 全局 + Per-topic   │ Per-connection
              │ 令牌桶                           │ BackpressureManager│ 有界队列+溢出策略
              │                                  │                    │
              │ 超速→丢弃/响应错误码              │ 堆积→关闭autoRead  │ 超限→按策略处理
              │                                  │ pub端降速           │ 丢弃旧/丢弃新/断开
```

### 新增类

| 类 | 所属模块 | 职责 |
|---|---|---|
| `RateLimiter` | jwsch-common | 令牌桶限速器 |
| `InboundRateLimiterHandler` | jwsch-srv | L1 入口限速ChannelHandler |
| `TopicBackpressureManager` | jwsch-srv | Per-topic背压管理 |
| `TopicBackpressureState` | jwsch-srv | 单topic背压状态 |
| `OutboundBufferHandler` | jwsch-srv | L3 有界队列+溢出策略 |
| `OverflowStrategy` | jwsch-common | 溢出策略枚举 |
| `FlowControlConfig` | jwsch-common | 流控配置(Builder模式) |
| `FlowControlMetrics` | jwsch-srv | 流控指标统一注册 |

### 修改类

| 类 | 修改内容 |
|---|---|
| `BackpressureManager` | 修复丢包bug，磁滞回线，重新启用 |
| `TcpServerHandler` | 集成L1限速 |
| `WebSocketHandler` | 集成L3堆积控制，上报writability事件 |
| `PacketRouter` | 集成L2背压检查 |
| `TcpClient` / `TcpHandler` | 客户端自适应降速 |
| `ProtocolConsts` | 新增ErrCode RATE_LIMITED(0x0A) |

## 4. L1 入口限速

### 4.1 RateLimiter

```java
public final class RateLimiter {
    private final long maxTokens;            // 桶容量
    private final long refillTokens;         // 每次补充令牌数
    private final long refillIntervalNanos;  // 补充间隔
    private long availableTokens;            // 当前令牌数
    private long lastRefillNanos;            // 上次补充时间

    public boolean tryAcquire(int permits);  // 非阻塞获取
    public boolean tryAcquire();             // 获取1个令牌
}
```

- 基于`System.nanoTime()`，无锁设计（单Channel单EventLoop，无竞争）
- 令牌数对应消息数（非字节数），语义清晰
- 配置项：`maxTokens`、`refillTokens`、`refillIntervalNanos`

### 4.2 InboundRateLimiterHandler

```java
public class InboundRateLimiterHandler extends ChannelInboundHandlerAdapter {
    private final RateLimiter rateLimiter;
    private final OverflowStrategy overflowStrategy; // DROP | ERROR_RESPONSE

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Packet) {
            if (!rateLimiter.tryAcquire()) {
                if (overflowStrategy == OverflowStrategy.ERROR_RESPONSE) {
                    sendRateLimitResponse(ctx, (Packet) msg);
                }
                ReferenceCountUtil.release(msg);
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }
}
```

- Pipeline位置：`PacketDecoder`之后、`TcpServerHandler`之前
- 超速时两种策略：静默丢弃（DROP）或回复错误码（ERROR_RESPONSE）
- 错误码复用协议`ErrCode`字段，新增`RATE_LIMITED(0x0A)`

### 4.3 Pipeline位置

```
TcpServer Pipeline:
  LengthFieldBasedFrameDecoder → PacketDecoder → InboundRateLimiterHandler → TcpServerHandler
```

### 4.4 配置

```yaml
flowControl:
  inbound:
    enabled: true
    maxTokensPerSecond: 10000
    burstSize: 12000
    overflowStrategy: DROP       # DROP | ERROR_RESPONSE
```

## 5. L2 背压传导

### 5.1 修复现有 BackpressureManager

**丢包根因**：关闭autoRead后TCP接收缓冲区满，内核丢包。问题在于：
1. 阈值过高（50%），触发时已大量堆积
2. 释放冷却期过短（100ms），频繁开关autoRead导致抖动
3. 无磁滞区间，触发/释放阈值相同导致震荡

**修复方案**：
- 触发阈值降低至20%，释放阈值5%，形成磁滞回线
- 冷却期延长至500ms
- 重新启用`checkAndDisableAutoRead()`

```java
public final class BackpressureManager {
    private static final double TRIGGER_THRESHOLD = 0.2;
    private static final double RELEASE_THRESHOLD = 0.05;
    private static final long RELEASE_COOLDOWN_MS = 500;

    private final AtomicInteger nonWritableCount = new AtomicInteger();
    private final AtomicInteger totalFrontendCount = new AtomicInteger();
    private volatile boolean backpressureActive = false;
    private volatile long lastReleaseTimeMs = 0;

    public void onFrontendWritabilityChanged(Channel channel, boolean writable) {
        if (writable) {
            nonWritableCount.decrementAndGet();
        } else {
            nonWritableCount.incrementAndGet();
        }
        checkAndAdjustBackpressure();
    }

    private void checkAndAdjustBackpressure() {
        double ratio = (double) nonWritableCount.get() / totalFrontendCount.get();
        if (!backpressureActive && ratio >= TRIGGER_THRESHOLD) {
            activateBackpressure();
        } else if (backpressureActive && ratio <= RELEASE_THRESHOLD) {
            if (System.currentTimeMillis() - lastReleaseTimeMs >= RELEASE_COOLDOWN_MS) {
                deactivateBackpressure();
            }
        }
    }

    private void activateBackpressure() {
        backpressureActive = true;
        for (Channel ch : backendChannels) {
            ch.config().setAutoRead(false);
        }
    }

    private void deactivateBackpressure() {
        backpressureActive = false;
        lastReleaseTimeMs = System.currentTimeMillis();
        for (Channel ch : backendChannels) {
            ch.config().setAutoRead(true);
        }
    }
}
```

### 5.2 新增 TopicBackpressureManager

per-topic背压隔离，一个慢topic不拖垮其他topic。

```java
public final class TopicBackpressureManager {
    private final ConcurrentHashMap<Long, TopicBackpressureState> topicStates;
    private final TopicSubscription topicSubscription;

    public boolean isTopicBackpressured(long topicHash) {
        TopicBackpressureState state = topicStates.get(topicHash);
        return state != null && state.isBackpressured();
    }

    public void onSubscriberWritabilityChanged(long topicHash, String connectionId, boolean writable) {
        topicStates.compute(topicHash, (k, state) -> {
            if (state == null) {
                state = new TopicBackpressureState(topicHash);
            }
            state.updateSubscriberState(connectionId, writable);
            return state;
        });
    }
}
```

```java
final class TopicBackpressureState {
    private final long topicHash;
    private final Set<String> nonWritableSubscribers = ConcurrentHashMap.newKeySet();
    private final AtomicInteger totalSubscribers = new AtomicInteger();

    private static final double TRIGGER_THRESHOLD = 0.3;
    private static final double RELEASE_THRESHOLD = 0.1;

    boolean isBackpressured() {
        if (totalSubscribers.get() == 0) return false;
        double ratio = (double) nonWritableSubscribers.size() / totalSubscribers.get();
        return ratio >= TRIGGER_THRESHOLD;
    }

    void updateSubscriberState(String connectionId, boolean writable) {
        if (writable) {
            nonWritableSubscribers.remove(connectionId);
        } else {
            nonWritableSubscribers.add(connectionId);
        }
    }
}
```

### 5.3 PacketRouter集成L2

```java
public void broadcastToTopic(long topicHash, Packet packet) {
    if (topicBackpressureManager.isTopicBackpressured(topicHash)) {
        metrics.incrementBackpressureDrop(topicHash);
        return;
    }
    // 正常广播逻辑...
}
```

### 5.4 客户端自适应降速

```java
public class TcpHandler extends SimpleChannelInboundHandler<Packet> {
    private static final long BACKOFF_INTERVAL_MS = 50;
    private static final long RECOVERY_INTERVAL_MS = 10;

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        if (!ctx.channel().isWritable()) {
            adjustSendInterval(BACKOFF_INTERVAL_MS);
        } else {
            adjustSendInterval(RECOVERY_INTERVAL_MS);
        }
    }
}
```

### 5.5 两级背压协作

```
全局BackpressureManager（粗粒度）     TopicBackpressureManager（细粒度）
         │                                      │
         │ 20% sub不可写 → 关闭autoRead          │ 30% topic sub不可写 → 丢弃该topic消息
         │ 5% sub不可写 → 恢复autoRead           │ 10% topic sub不可写 → 恢复该topic投递
         │                                      │
         └──────── 协作逻辑 ─────────────────────┘
         Topic级先触发（30% < 50%全局），丢弃特定topic消息
         全局后触发，关闭autoRead从源头限流
         恢复时全局先恢复，topic后恢复
```

## 6. L3 堆积溢出控制

### 6.1 OverflowStrategy

```java
public enum OverflowStrategy {
    DROP_OLDEST,                       // 丢弃最旧消息
    DROP_NEWEST,                       // 丢弃新消息
    DISCONNECT,                        // 断开慢消费者
    DROP_OLDEST_THEN_DISCONNECT        // 先丢弃旧消息，超极限阈值后断开
}
```

### 6.2 OutboundBufferHandler

```java
public class OutboundBufferHandler extends ChannelDuplexHandler {
    private final int maxQueueSize;
    private final int disconnectThreshold;
    private final OverflowStrategy overflowStrategy;
    private final Queue<Packet> pendingQueue;
    private final LongAdder dropCount;
    private final LongAdder enqueueCount;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (!(msg instanceof BinaryWebSocketFrame)) {
            ctx.write(msg, promise);
            return;
        }

        if (ctx.channel().isWritable()) {
            drainQueue(ctx);
            ctx.write(msg, promise);
        } else {
            if (pendingQueue.size() >= maxQueueSize) {
                handleOverflow(ctx, msg, promise);
            } else {
                pendingQueue.offer(extractPacket(msg));
                ReferenceCountUtil.release(msg);
                promise.setSuccess();
            }
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        if (ctx.channel().isWritable()) {
            drainQueue(ctx);
        }
        ctx.fireChannelWritabilityChanged();
    }

    private void drainQueue(ChannelHandlerContext ctx) {
        Packet packet;
        while (ctx.channel().isWritable() && (packet = pendingQueue.poll()) != null) {
            ByteBuf frame = PacketWriter.writeToPooledDirectBuffer(packet, ctx.alloc());
            ctx.write(frame);
            ReferenceCountUtil.release(packet);
        }
        ctx.flush();
    }

    private void handleOverflow(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        switch (overflowStrategy) {
            case DROP_OLDEST:
                Packet oldest = pendingQueue.poll();
                ReferenceCountUtil.release(oldest);
                pendingQueue.offer(extractPacket(msg));
                dropCount.increment();
                break;
            case DROP_NEWEST:
                ReferenceCountUtil.release(msg);
                dropCount.increment();
                break;
            case DISCONNECT:
                ReferenceCountUtil.release(msg);
                ctx.close();
                break;
            case DROP_OLDEST_THEN_DISCONNECT:
                if (pendingQueue.size() >= disconnectThreshold) {
                    ReferenceCountUtil.release(msg);
                    ctx.close();
                } else {
                    Packet old = pendingQueue.poll();
                    ReferenceCountUtil.release(old);
                    pendingQueue.offer(extractPacket(msg));
                    dropCount.increment();
                }
                break;
        }
        promise.setSuccess();
    }
}
```

### 6.3 Pipeline位置

```
WebSocketServer Pipeline:
  HttpServerCodec → HttpObjectAggregator → WebSocketServerProtocolHandler
  → FlushConsolidationHandler → OutboundBufferHandler → WebSocketHandler
```

### 6.4 配置

```yaml
flowControl:
  outbound:
    enabled: true
    maxQueueSize: 1024
    disconnectThreshold: 4096
    overflowStrategy: DROP_OLDEST_THEN_DISCONNECT
```

## 7. 监控与可观测性

### 7.1 FlowControlMetrics

统一注册所有流控指标到Micrometer。

### 7.2 L1 入口限速指标

| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `jwsch.inbound.accepted` | Counter | `connectionId` | 限速通过的消息数 |
| `jwsch.inbound.rejected` | Counter | `connectionId`, `strategy` | 限速拒绝的消息数 |
| `jwsch.inbound.available.tokens` | Gauge | `connectionId` | 当前可用令牌数 |
| `jwsch.inbound.rate` | Gauge | `connectionId` | 当前实际入站速率(msg/s) |

### 7.3 L2 背压传导指标

| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `jwsch.backpressure.global.activate` | Counter | - | 全局背压激活次数 |
| `jwsch.backpressure.global.deactivate` | Counter | - | 全局背压释放次数 |
| `jwsch.backpressure.global.active` | Gauge | - | 当前全局背压是否激活(0/1) |
| `jwsch.backpressure.global.nonWritable.ratio` | Gauge | - | 全局不可写sub比例 |
| `jwsch.backpressure.topic.drop` | Counter | `topicHash` | Topic级背压丢弃数 |
| `jwsch.backpressure.topic.active` | Gauge | `topicHash` | Topic背压是否激活(0/1) |
| `jwsch.backpressure.topic.nonWritable.ratio` | Gauge | `topicHash` | Topic不可写sub比例 |
| `jwsch.backpressure.autoread.disabled` | Gauge | - | autoRead关闭的backend连接数 |

### 7.4 L3 堆积溢出指标

| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `jwsch.outbound.queue.size` | Gauge | `connectionId` | 出站队列消息数 |
| `jwsch.outbound.queue.bytes` | Gauge | `connectionId` | 出站队列字节数 |
| `jwsch.outbound.drop` | Counter | `connectionId`, `strategy` | 溢出丢弃数 |
| `jwsch.outbound.enqueue` | Counter | `connectionId` | 入队数 |
| `jwsch.outbound.drain` | Counter | `connectionId` | 队列排空数 |
| `jwsch.outbound.disconnect` | Counter | `connectionId` | 慢消费者断开数 |

### 7.5 连接级指标

| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `jwsch.connection.backend.active` | Gauge | - | 活跃TCP backend连接数 |
| `jwsch.connection.frontend.active` | Gauge | - | 活跃WebSocket frontend连接数 |
| `jwsch.connection.frontend.nonWritable` | Gauge | - | 不可写frontend连接数 |

### 7.6 日志规范

```java
// L1 限速日志
LOGGER.warn("Inbound rate limited: connectionId={}, rejected={}, availableTokens={}",
    connectionId, rejectedCount, availableTokens);

// L2 背压日志
LOGGER.info("Global backpressure activated: nonWritableRatio={}, backendChannels={}",
    ratio, backendChannels.size());
LOGGER.info("Global backpressure deactivated: nonWritableRatio={}, durationMs={}",
    ratio, durationMs);
LOGGER.warn("Topic backpressure drop: topicHash={}, nonWritableRatio={}, dropCount={}",
    topicHash, ratio, dropCount);

// L3 堆积日志
LOGGER.warn("Outbound overflow: connectionId={}, queueSize={}, strategy={}, dropCount={}",
    connectionId, queueSize, strategy, dropCount);
LOGGER.warn("Slow consumer disconnected: connectionId={}, queueSize={}, totalDropped={}",
    connectionId, queueSize, totalDropped);
```

### 7.7 告警规则（Prometheus示例）

```yaml
groups:
  - name: jwsch_flow_control
    rules:
      - alert: InboundRateLimitHigh
        expr: rate(jwsch_inbound_rejected_total[1m]) > 100
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "入站限速拒绝率过高"

      - alert: GlobalBackpressureActive
        expr: jwsch_backpressure_global_active == 1
        for: 30s
        labels:
          severity: critical
        annotations:
          summary: "全局背压已激活，pub端被限流"

      - alert: OutboundQueueDepthHigh
        expr: jwsch_outbound_queue_size > 512
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "出站队列堆积深度过高"

      - alert: SlowConsumerDisconnected
        expr: rate(jwsch_outbound_disconnect_total[5m]) > 0
        labels:
          severity: warning
        annotations:
          summary: "慢消费者被断开"
```

## 8. 完整配置

```yaml
flowControl:
  # L1 入口限速
  inbound:
    enabled: true
    maxTokensPerSecond: 10000
    burstSize: 12000
    overflowStrategy: DROP          # DROP | ERROR_RESPONSE

  # L2 背压传导
  backpressure:
    global:
      enabled: true
      triggerThreshold: 0.2         # 20% sub不可写触发
      releaseThreshold: 0.05        # 5% sub不可写释放
      releaseCooldownMs: 500
    topic:
      enabled: true
      triggerThreshold: 0.3
      releaseThreshold: 0.1

  # L3 堆积溢出
  outbound:
    enabled: true
    maxQueueSize: 1024
    disconnectThreshold: 4096
    overflowStrategy: DROP_OLDEST_THEN_DISCONNECT

  # 监控
  metrics:
    enabled: true
    logIntervalSeconds: 60
    topicMetricsEnabled: true
```

## 9. 实施优先级

1. **P0**：修复BackpressureManager + 重新启用（已有代码基础，风险最低）
2. **P0**：L3 OutboundBufferHandler（防OOM，最紧急）
3. **P1**：L1 InboundRateLimiterHandler（防pub打爆）
4. **P1**：TopicBackpressureManager（per-topic隔离）
5. **P2**：FlowControlMetrics（可观测性）
6. **P2**：客户端自适应降速
