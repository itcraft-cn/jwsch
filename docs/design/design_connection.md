# 连接管理设计

## 一、连接类型

### 1.1 前端连接

- **协议**：WebSocket
- **来源**：前端JS客户端
- **管理组件**：WebSocketConnectionManager

### 1.2 后端连接

- **协议**：TCP
- **来源**：jwsch主动连接后端Java服务
- **管理组件**：TcpConnectionPool

---

## 二、连接ID分配

### 2.1 分配时机

连接建立时立即分配唯一的 `long` 类型ID

### 2.2 ID生成规则

- 前端连接：`MurmurHash3(ip:port)`
- 后端连接：`MurmurHash3(ip:port)`

### 2.3 ID唯一性

- 通过MurmurHash3保证低冲突率
- 在jwsch集群内部保持唯一

---

## 三、WebSocket连接管理

### 3.1 WebSocketConnectionManager

**职责**：
- 连接注册：记录 `connectionId <-> Channel` 映射
- 连接注销：连接关闭时移除映射
- 连接查询：根据ID查找Channel
- 连接统计：统计连接数、状态等
- 广播：向所有连接发送消息

**接口设计**：
```
WebSocketConnectionManager:
- addConnection(Channel channel): long
- removeConnection(long connectionId): void
- getConnection(long connectionId): Channel
- getAllConnections(): List<Channel>
- getConnectionCount(): int
- broadcast(Packet packet): void
```

### 3.2 连接生命周期

```
建立连接:
1. WebSocket握手成功
2. 生成connectionId
3. 存入connections映射
4. 触发连接事件

运行:
1. 接收消息
2. 处理业务
3. 发送响应

关闭连接:
1. 检测到连接关闭
2. 从connections映射移除
3. 触发断开事件
4. 清理资源
```

### 3.3 连接存储

```
ConcurrentMap<Long, Channel> connections

Key: connectionId (long)
Value: Channel (Netty通道)
```

---

## 四、TCP连接池

### 4.1 TcpConnectionPool

**职责**：
- 连接池管理：维护后端服务连接
- 连接复用：避免频繁创建/销毁连接
- 健康检查：检测连接可用性
- 负载均衡：选择合适的连接

**接口设计**：
```
TcpConnectionPool:
- getChannel(String serviceName): Channel
- returnChannel(String serviceName, Channel channel): void
- removeChannel(String serviceName, Channel channel): void
- getActiveChannels(String serviceName): List<Channel>
```

### 4.2 连接池策略

**每个服务一个连接池**：
```
Map<String, List<Channel>> servicePools

Key: serviceName
Value: List<Channel>
```

**连接获取策略**：
1. 从池中获取空闲连接
2. 如果池为空，创建新连接
3. 如果连接不可用，创建新连接

### 4.3 连接保活

**心跳机制**：
- 定时发送HEARTBEAT消息
- 检测连接是否存活
- 超时未响应则关闭连接

**自动重连**：
- 连接断开时自动重连
- 重连间隔：指数退避
- 最大重连次数：可配置

---

## 五、ID反向查找

### 5.1 设计目标

提供从connectionId到连接真实信息的反向查找能力，方便在日志中体现真实连接情况。

### 5.2 ConnectionInfo模型

```
ConnectionInfo:
- connectionId: long           // 连接ID
- remoteAddress: String        // 远程地址（IP:port）
- localAddress: String         // 本地地址（IP:port）
- connectionType: ConnectionType  // 连接类型（FRONTEND/BACKEND）
- serviceName: String          // 服务名（仅后端连接）
- createTime: long             // 创建时间
- lastActiveTime: long         // 最后活跃时间
- status: ConnectionStatus     // 状态（ACTIVE/IDLE/CLOSED）

ConnectionType (enum):
- FRONTEND: 前端连接
- BACKEND: 后端连接

ConnectionStatus (enum):
- ACTIVE: 活跃
- IDLE: 空闲
- CLOSED: 已关闭
```

### 5.3 反向查找接口

```
ConnectionRegistry:
- register(long connectionId, ConnectionInfo info): void
- unregister(long connectionId): void
- lookup(long connectionId): ConnectionInfo
- lookupAll(): List<ConnectionInfo>
- lookupByType(ConnectionType type): List<ConnectionInfo>
- lookupByRemoteAddress(String remoteAddress): List<ConnectionInfo>
```

### 5.4 实现方案

#### 方案一：ConcurrentHashMap（推荐）

**优点**：
- 线程安全
- 查询性能高（O(1)）
- 实现简单

**实现**：
```
ConnectionRegistryImpl:
- connectionMap: ConcurrentHashMap<Long, ConnectionInfo>
- addressIndex: ConcurrentHashMap<String, Set<Long>>  // 地址索引

register(connectionId, info):
  connectionMap.put(connectionId, info)
  addressIndex.computeIfAbsent(info.remoteAddress, k -> ConcurrentHashMap.newKeySet())
    .add(connectionId)

unregister(connectionId):
  info = connectionMap.remove(connectionId)
  if (info != null):
    Set<Long> ids = addressIndex.get(info.remoteAddress)
    if (ids != null):
      ids.remove(connectionId)

lookup(connectionId):
  return connectionMap.get(connectionId)
```

#### 方案二：Guava Cache

**优点**：
- 支持过期策略
- 支持容量限制
- 自动淘汰

**实现**：
```
ConnectionRegistryImpl:
- connectionCache: Cache<Long, ConnectionInfo>

// 配置
connectionCache = CacheBuilder.newBuilder()
  .maximumSize(10000)
  .expireAfterWrite(1, TimeUnit.HOURS)
  .build()
```

### 5.5 性能优化

#### 5.5.1 查询性能

| 操作 | 时间复杂度 | 说明 |
|------|------------|------|
| register | O(1) | ConcurrentHashMap.put |
| unregister | O(1) | ConcurrentHashMap.remove |
| lookup | O(1) | ConcurrentHashMap.get |
| lookupByRemoteAddress | O(1) + O(k) | 地址索引 + 遍历k个结果 |

#### 5.5.2 内存优化

- 使用基本类型而非包装类型
- ConnectionInfo使用final字段
- 合理设置初始容量，避免扩容

### 5.6 日志集成

#### 5.6.1 日志格式

```
INFO - Connection established: id=12345, remote=192.168.1.100:8080, type=FRONTEND
INFO - Connection closed: id=12345, remote=192.168.1.100:8080, duration=3600s
ERROR - Connection error: id=12345, remote=192.168.1.100:8080, error=Connection reset
```

#### 5.6.2 日志工具类

```
ConnectionLogger:
- logConnection(long connectionId, String action, ConnectionInfo info): void
- logError(long connectionId, String action, Throwable error): void

使用方式:
ConnectionInfo info = connectionRegistry.lookup(connectionId);
ConnectionLogger.logConnection(connectionId, "established", info);
// 输出: Connection established: id=12345, remote=192.168.1.100:8080, type=FRONTEND
```

### 5.7 使用示例

#### 5.7.1 注册连接

```
// WebSocket连接建立时
ConnectionInfo info = new ConnectionInfo.Builder()
    .connectionId(connectionId)
    .remoteAddress(remoteAddress)
    .localAddress(localAddress)
    .connectionType(ConnectionType.FRONTEND)
    .createTime(System.currentTimeMillis())
    .status(ConnectionStatus.ACTIVE)
    .build();

connectionRegistry.register(connectionId, info);
logger.info("Connection established: {}", info);
```

#### 5.7.2 查询连接信息

```
// 日志中输出连接信息
ConnectionInfo info = connectionRegistry.lookup(connectionId);
if (info != null) {
    logger.info("Processing request from: remote={}, id={}", 
        info.getRemoteAddress(), info.getConnectionId());
}
```

#### 5.7.3 注销连接

```
// 连接关闭时
ConnectionInfo info = connectionRegistry.unregister(connectionId);
if (info != null) {
    long duration = System.currentTimeMillis() - info.getCreateTime();
    logger.info("Connection closed: remote={}, duration={}s", 
        info.getRemoteAddress(), duration / 1000);
}
```

### 5.8 配置项

```properties
# 连接注册表配置
jwsch.connection.registry.initial.capacity=1024
jwsch.connection.registry.load.factor=0.75
jwsch.connection.registry.concurrency.level=16
```

---

## 六、心跳机制

### 6.1 配置项

```properties
# 心跳配置
jwsch.connection.heartbeat.interval=60   # 心跳间隔（秒）
jwsch.connection.heartbeat.timeout=180   # 心跳超时（秒）
```

### 6.2 实现方式

**Netty IdleStateHandler**：
```
ChannelPipeline pipeline = ch.pipeline();
pipeline.addLast("idleState", 
    new IdleStateHandler(heartbeatTimeout, 0, 0, TimeUnit.SECONDS));
pipeline.addLast("heartbeatHandler", new HeartbeatHandler());
```

### 6.3 心跳逻辑

**服务端**：
```
IdleStateEvent触发:
1. 发送HEARTBEAT请求
2. 等待客户端响应
3. 超时未响应则关闭连接

收到HEARTBEAT请求:
1. 立即回复HEARTBEAT响应
```

**客户端**：
```
收到HEARTBEAT请求:
1. 立即回复HEARTBEAT响应
```

---

## 七、连接事件

### 7.1 事件类型

| 事件 | 触发时机 |
|------|----------|
| CONNECTED | 连接建立成功 |
| DISCONNECTED | 连接断开 |
| HEARTBEAT_TIMEOUT | 心跳超时 |
| ERROR | 连接异常 |

### 7.2 事件监听器

```
ConnectionEventListener:
- onConnected(ConnectionEvent event): void
- onDisconnected(ConnectionEvent event): void
- onHeartbeatTimeout(ConnectionEvent event): void
- onError(ConnectionEvent event): void
```

### 7.3 事件处理

```
ConnectionEvent:
- connectionId: long
- channel: Channel
- type: EventType
- timestamp: long
- metadata: Map<String, Object>
```

---

## 八、连接统计

### 8.1 统计指标

| 指标 | 说明 |
|------|------|
| totalConnections | 总连接数 |
| activeConnections | 活跃连接数 |
| idleConnections | 空闲连接数 |
| connectionsPerSecond | 每秒新增连接数 |
| disconnectionsPerSecond | 每秒断开连接数 |

### 8.2 统计实现

```
ConnectionStats:
- totalConnections: AtomicInteger
- activeConnections: AtomicInteger
- connectionCount: LongAdder
- disconnectionCount: LongAdder

方法:
- incrementConnections(): void
- decrementConnections(): void
- getConnectionsPerSecond(): double
```

---

## 九、配置项汇总

```properties
# 连接管理配置
jwsch.connection.max=10000
jwsch.connection.heartbeat.interval=60
jwsch.connection.heartbeat.timeout=180

# WebSocket服务端
jwsch.srv.websocket.timeout.connect=30000
jwsch.srv.websocket.timeout.read=0
jwsch.srv.websocket.timeout.write=0

# TCP客户端
jwsch.cli.tcp.timeout.connect=30000
jwsch.cli.tcp.timeout.read=0
jwsch.cli.tcp.timeout.write=0
```

---

## 十、异常处理

### 10.1 连接异常

| 异常 | 处理方式 |
|------|----------|
| CONNECTION_REFUSED | 重试或选择其他实例 |
| CONNECTION_TIMEOUT | 重试或选择其他实例 |
| CONNECTION_RESET | 关闭连接，触发重连 |
| HEARTBEAT_TIMEOUT | 关闭连接，触发重连 |

### 10.2 异常处理器

```
ConnectionExceptionHandler:
- handleException(Channel channel, Throwable cause): void
```

**处理逻辑**：
1. 记录异常日志
2. 关闭连接
3. 触发重连（如果需要）
4. 发送错误响应（如果可能）