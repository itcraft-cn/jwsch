# 消息路由设计

## 一、设计目标

- 请求-响应映射
- 消息转发（点对点、广播）
- 超时管理
- 异步处理

---

## 二、请求-响应映射

### 2.1 设计思路

每个请求分配唯一的requestId，用于匹配响应。

### 2.2 ResponseMapping

```
ResponseMapping:
- pendingRequests: ConcurrentMap<Integer, CompletableFuture<Packet>>
- requestIdGenerator: AtomicInteger

方法:
- generateRequestId(): int
- createFuture(int requestId): CompletableFuture<Packet>
- completeResponse(int requestId, Packet response): void
- removeFuture(int requestId): void
```

### 2.3 请求流程

```
1. 生成requestId
2. 创建CompletableFuture，存入pendingRequests
3. 发送请求包
4. 等待响应（带超时）
5. 收到响应后，根据requestId找到Future
6. 完成Future，移除映射
```

### 2.4 超时处理

```
CompletableFuture<Packet> future = new CompletableFuture<>();
future.orTimeout(30, TimeUnit.SECONDS)
    .whenComplete((packet, ex) -> {
        pendingRequests.remove(requestId);
        if (ex != null) {
            logger.warn("Request timeout: requestId={}", requestId);
        }
    });
```

---

## 三、消息路由器

### 3.1 PacketRouter

```
PacketRouter:
- connectionManager: WebSocketConnectionManager
- tcpConnectionPool: TcpConnectionPool
- responseMapping: ResponseMapping
- loadBalance: LoadBalance

方法:
- route(Packet packet): CompletableFuture<Packet>
- routeToBackend(Packet packet): void
- routeToFrontend(Packet packet): void
- broadcast(Packet packet): void
```

### 3.2 路由逻辑

#### 前端 -> 后端

```
routeToBackend(packet):
  1. 从packet获取目标服务名（从topic解析）
  2. 从注册中心发现服务实例
  3. 负载均衡选择实例
  4. 从连接池获取连接
  5. 转发packet到后端
```

#### 后端 -> 前端

```
routeToFrontend(packet):
  1. 从packet获取targetId
  2. 从connectionManager查找Channel
  3. 转发packet到前端
```

#### 广播

```
broadcast(packet):
  1. 获取所有前端连接
  2. 遍历连接
  3. 使用零拷贝方式转发（retain + writeAndFlush）
```

---

## 四、消息类型处理

### 4.1 REQUEST

**流程**：
```
前端 -> WebSocketHandler -> PacketRouter -> 后端

1. 前端发送REQUEST
2. WebSocketHandler接收并解码
3. PacketRouter路由到后端
4. 后端处理并返回RESPONSE
5. PacketRouter路由回前端
```

### 4.2 RESPONSE

**流程**：
```
后端 -> TcpHandler -> PacketRouter -> 前端

1. 后端返回RESPONSE
2. TcpHandler接收并解码
3. PacketRouter根据targetId路由到前端
4. 前端收到响应
```

### 4.3 PUSH

**流程**：
```
后端 -> TcpHandler -> PacketRouter -> 前端

1. 后端主动推送PUSH
2. PacketRouter根据targetId转发到指定前端
```

### 4.4 BROADCAST

**流程**：
```
后端 -> TcpHandler -> PacketRouter -> 所有前端

1. 后端发送BROADCAST（targetId=0）
2. PacketRouter获取所有前端连接
3. 零拷贝广播到所有连接
```

### 4.5 SUBSCRIBE

**流程**：
```
前端 -> WebSocketHandler -> PacketRouter

1. 前端发送SUBSCRIBE（topic）
2. PacketRouter记录订阅关系
3. 后续PUSH/BROADCAST到该topic的消息会被转发
```

---

## 五、订阅管理

### 5.1 Topic订阅关系

```
TopicSubscription:
- topicSubscribers: ConcurrentMap<String, Set<Long>>

方法:
- subscribe(String topic, long connectionId): void
- unsubscribe(String topic, long connectionId): void
- getSubscribers(String topic): Set<Long>
```

### 5.2 订阅流程

```
subscribe(topic, connectionId):
  topicSubscribers.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet())
    .add(connectionId)
  logger.info("Subscribed: topic={}, connectionId={}", topic, connectionId)
```

### 5.3 取消订阅

```
unsubscribe(topic, connectionId):
  Set<Long> subscribers = topicSubscribers.get(topic)
  if (subscribers != null):
    subscribers.remove(connectionId)
    if (subscribers.isEmpty()):
      topicSubscribers.remove(topic)
```

---

## 六、配置项

```properties
# 路由配置
jwsch.router.request.timeout=30000  # 请求超时（毫秒）
```

---

## 七、性能优化

### 7.1 零拷贝转发

- 点对点：直接转发Packet
- 广播：retain + resetReaderIndex

### 7.2 异步处理

- 使用CompletableFuture异步处理
- 不阻塞IO线程

### 7.3 连接复用

- 使用连接池
- 避免频繁创建连接