# 可观测性设计

## 一、设计目标

- Metrics：指标监控
- Tracing：链路追踪
- Logging：结构化日志
- Health：健康检查

---

## 二、Metrics

### 2.1 基础指标

| 指标类别 | 指标名称 | 说明 |
|----------|----------|------|
| 连接数 | jwsch.connections.total | 总连接数 |
| 连接数 | jwsch.connections.active | 活跃连接数 |
| 消息吞吐 | jwsch.messages.received | 接收消息数 |
| 消息吞吐 | jwsch.messages.sent | 发送消息数 |
| 响应时间 | jwsch.response.time | 响应时间分布 |
| 错误率 | jwsch.errors.total | 错误总数 |

### 2.2 Topic统计指标

#### 2.2.1 Top10订阅Topic

**指标**：`jwsch.topic.subscriptions`

**说明**：统计每个Topic的订阅数量，展示Top10

**数据结构**：
```
TopicSubscriptionStats:
- topic: String              // Topic名称
- subscriptionCount: int     // 订阅数量
- updateTime: long           // 更新时间
```

**实现**：
```
TopicSubscriptionTracker:
- subscriptionCounts: ConcurrentMap<String, AtomicInteger>

subscribe(topic):
  subscriptionCounts.computeIfAbsent(topic, k -> new AtomicInteger(0))
    .incrementAndGet()

unsubscribe(topic):
  counter = subscriptionCounts.get(topic)
  if (counter != null):
    counter.decrementAndGet()

getTop10Subscriptions():
  return subscriptionCounts.entrySet().stream()
    .sorted((e1, e2) -> e2.getValue().get() - e1.getValue().get())
    .limit(10)
    .collect(toList())
```

#### 2.2.2 Top10消息交互次数Topic

**指标**：`jwsch.topic.message.count`

**说明**：统计每个Topic的消息交互次数，展示Top10

**数据结构**：
```
TopicMessageStats:
- topic: String              // Topic名称
- messageCount: long         // 消息次数
- requestCount: long         // 请求次数
- responseCount: long        // 响应次数
- pushCount: long            // 推送次数
- broadcastCount: long       // 广播次数
- updateTime: long           // 更新时间
```

**实现**：
```
TopicMessageTracker:
- messageCounts: ConcurrentMap<String, LongAdder>
- requestCounts: ConcurrentMap<String, LongAdder>
- responseCounts: ConcurrentMap<String, LongAdder>
- pushCounts: ConcurrentMap<String, LongAdder>
- broadcastCounts: ConcurrentMap<String, LongAdder>

recordMessage(topic, command):
  messageCounts.computeIfAbsent(topic, k -> new LongAdder()).increment()
  switch (command):
    case REQUEST: requestCounts.computeIfAbsent(topic, k -> new LongAdder()).increment()
    case RESPONSE: responseCounts.computeIfAbsent(topic, k -> new LongAdder()).increment()
    case PUSH: pushCounts.computeIfAbsent(topic, k -> new LongAdder()).increment()
    case BROADCAST: broadcastCounts.computeIfAbsent(topic, k -> new LongAdder()).increment()

getTop10MessageCount():
  return messageCounts.entrySet().stream()
    .sorted((e1, e2) -> Long.compare(e2.getValue().sum(), e1.getValue().sum()))
    .limit(10)
    .collect(toList())
```

#### 2.2.3 Top10流量消耗Topic

**指标**：`jwsch.topic.traffic.bytes`

**说明**：统计每个Topic的流量消耗（字节数），展示Top10

**数据结构**：
```
TopicTrafficStats:
- topic: String              // Topic名称
- totalBytes: long           // 总流量（字节）
- requestBytes: long         // 请求流量
- responseBytes: long        // 响应流量
- pushBytes: long            // 推送流量
- broadcastBytes: long       // 广播流量
- updateTime: long           // 更新时间
```

**实现**：
```
TopicTrafficTracker:
- totalBytes: ConcurrentMap<String, LongAdder>
- requestBytes: ConcurrentMap<String, LongAdder>
- responseBytes: ConcurrentMap<String, LongAdder>
- pushBytes: ConcurrentMap<String, LongAdder>
- broadcastBytes: ConcurrentMap<String, LongAdder>

recordTraffic(topic, command, bytes):
  totalBytes.computeIfAbsent(topic, k -> new LongAdder()).add(bytes)
  switch (command):
    case REQUEST: requestBytes.computeIfAbsent(topic, k -> new LongAdder()).add(bytes)
    case RESPONSE: responseBytes.computeIfAbsent(topic, k -> new LongAdder()).add(bytes)
    case PUSH: pushBytes.computeIfAbsent(topic, k -> new LongAdder()).add(bytes)
    case BROADCAST: broadcastBytes.computeIfAbsent(topic, k -> new LongAdder()).add(bytes)

getTop10Traffic():
  return totalBytes.entrySet().stream()
    .sorted((e1, e2) -> Long.compare(e2.getValue().sum(), e1.getValue().sum()))
    .limit(10)
    .collect(toList())
```

### 2.3 Topic统计查询接口

```
TopicStatsService:
- getTop10Subscriptions(): List<TopicSubscriptionStats>
- getTop10MessageCount(): List<TopicMessageStats>
- getTop10Traffic(): List<TopicTrafficStats>
- getAllTopicStats(): Map<String, TopicStats>
- resetStats(): void
```

### 2.4 Topic统计HTTP端点

| 端点 | 说明 |
|------|------|
| /stats/topics/subscriptions | Top10订阅Topic |
| /stats/topics/messages | Top10消息交互Topic |
| /stats/topics/traffic | Top10流量消耗Topic |
| /stats/topics | 所有Topic统计 |

**响应示例**：
```json
{
  "timestamp": 1689072000000,
  "top10Subscriptions": [
    {"topic": "/topic/news", "count": 150},
    {"topic": "/topic/alerts", "count": 120},
    {"topic": "/topic/chat", "count": 100}
  ],
  "top10MessageCount": [
    {"topic": "/api/user/get", "total": 50000, "request": 25000, "response": 25000},
    {"topic": "/api/order/list", "total": 30000, "request": 15000, "response": 15000}
  ],
  "top10Traffic": [
    {"topic": "/api/file/download", "totalBytes": 1073741824, "request": 1024, "response": 1073740800},
    {"topic": "/api/user/avatar", "totalBytes": 536870912, "request": 512, "response": 536870400}
  ]
}
```

### 2.5 性能优化

#### 2.5.1 统计性能

- 使用LongAdder替代AtomicLong，高并发写入性能更好
- 使用ConcurrentHashMap保证线程安全
- Top10计算延迟执行，避免每次查询都排序

#### 2.5.2 内存优化

- Topic名称使用String.intern()减少内存占用
- 定期清理不活跃的Topic统计
- 设置Topic数量上限

#### 2.5.3 采样策略

对于高频Topic，可采用采样策略：
```properties
jwsch.stats.topic.sample.enabled=true
jwsch.stats.topic.sample.rate=0.1  # 采样率10%
```

### 2.6 实现依赖

**依赖**：Micrometer

**配置**：
```properties
jwsch.metrics.enabled=true
```

---

## 三、Topic统计管理

### 3.1 TopicStatsManager

统一管理所有Topic统计：

```
TopicStatsManager:
- subscriptionTracker: TopicSubscriptionTracker
- messageTracker: TopicMessageTracker
- trafficTracker: TopicTrafficTracker
- scheduler: ScheduledExecutorService

方法:
- recordSubscribe(String topic): void
- recordUnsubscribe(String topic): void
- recordMessage(String topic, byte command, int bytes): void
- getTop10Subscriptions(): List<TopicSubscriptionStats>
- getTop10MessageCount(): List<TopicMessageStats>
- getTop10Traffic(): List<TopicTrafficStats>
- startScheduledCleanup(): void  // 定时清理
```

### 3.2 统计生命周期

```
启动:
1. 初始化统计追踪器
2. 启动定时清理任务

运行:
1. 记录订阅/取消订阅
2. 记录消息交互
3. 记录流量消耗

定时清理:
1. 每小时清理不活跃Topic（无操作超过24小时）
2. 每天重置统计计数（可选）
```

### 3.3 配置项

```properties
# Topic统计配置
jwsch.stats.topic.enabled=true
jwsch.stats.topic.cleanup.interval=3600        # 清理间隔（秒）
jwsch.stats.topic.cleanup.inactive.hours=24    # 不活跃阈值（小时）
jwsch.stats.topic.max.count=10000              # Topic数量上限
```

---

## 四、Tracing

### 4.1 埋点位置

- 消息接收
- 消息转发
- 消息响应

### 4.2 TraceContext传播

通过包头扩展字段传递TraceContext

### 4.3 实现

**依赖**：OpenTelemetry

**配置**：
```properties
jwsch.tracing.enabled=true
jwsch.tracing.sampler=0.1
```

---

## 五、Logging

### 5.1 结构化日志

**格式**：JSON

**字段**：
```json
{
  "timestamp": "2026-03-12T10:00:00.000Z",
  "level": "INFO",
  "logger": "cn.itcraft.jwsch.srv.WebSocketServer",
  "message": "Connection established",
  "connectionId": 12345,
  "traceId": "abc123",
  "spanId": "def456"
}
```

### 5.2 日志级别

| 级别 | 场景 |
|------|------|
| ERROR | 系统错误、严重异常 |
| WARN | 可恢复异常、业务告警 |
| INFO | 重要操作、连接建立/关闭 |
| DEBUG | 详细调试信息 |

---

## 六、健康检查

### 6.1 HTTP端点

| 端点 | 说明 |
|------|------|
| /health/live | 存活探针 |
| /health/ready | 就绪探针 |

### 6.2 健康状态

```json
{
  "status": "UP",
  "components": {
    "websocket": "UP",
    "tcp": "UP",
    "registry": "UP"
  }
}
```

### 6.3 实现

独立HTTP服务器（Netty实现）

**配置**：
```properties
jwsch.srv.http.enabled=true
jwsch.srv.http.port=8081
```

---

## 七、配置项汇总

```properties
# Metrics
jwsch.metrics.enabled=true

# Topic统计
jwsch.stats.topic.enabled=true
jwsch.stats.topic.cleanup.interval=3600
jwsch.stats.topic.cleanup.inactive.hours=24
jwsch.stats.topic.max.count=10000

# Tracing
jwsch.tracing.enabled=true
jwsch.tracing.sampler=0.1

# Logging
jwsch.logging.format=json
```