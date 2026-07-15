# jwsch 编码计划

## 一、Phase 1: 核心基础（第1-2周）

**目标**：单机版本，完成核心协议和基础功能

### 1.1 jwsch-common 模块

#### 1.1.1 协议模块

**核心类**：
- `ProtocolConsts`：协议常量（MAGIC, FIXED_HEADER_LENGTH=27）
- `Command`：命令枚举（预留集群命令0x10-0x12）
- `ErrorCode`：错误码枚举（code+desc）
- `PacketHeader`：包头（27字节固定头）
- `Packet`：消息包（持有ByteBuf引用）
- `PacketEncoder`：编码器（零拷贝）
- `PacketDecoder`：解码器（零拷贝）

**单元测试**：
- `ProtocolConstsTest`
- `CommandTest`
- `ErrorCodeTest`
- `PacketHeaderTest`
- `PacketTest`
- `PacketEncoderTest`
- `PacketDecoderTest`

**JMH测试**：
- `PacketEncoderBenchmark`
- `PacketDecoderBenchmark`

#### 1.1.2 ID生成模块

**核心类**：
- `IdGenerator`：ID生成器（MurmurHash3）

**单元测试**：
- `IdGeneratorTest`

**JMH测试**：
- `IdGeneratorBenchmark`

#### 1.1.3 异常模块

**核心类**：
- `JwschException`：基类异常（持有ErrorCode枚举）
- `ProtocolException`：协议异常
- `ConnectionException`：连接异常
- `RouteException`：路由异常
- `RegistryException`：注册中心异常

#### 1.1.4 缓存模块

**核心类**：
- `Cache<K, V>`：缓存接口
- `LoadingCache<K, V>`：自动加载缓存接口
- `CacheBuilder<K, V>`：缓存构建器
- `CacheConfig`：缓存配置
- `ConcurrentHashMapCache<K, V>`：实现类
- `GuavaCache<K, V>`：实现类

#### 1.1.5 配置模块

**核心类**：
- `TcpConfig`：TCP配置基类
- `WriteBufferWaterMark`：写缓冲区水位
- `ConfigValidator`：配置校验器

#### 1.1.6 工具模块

**核心类**：
- `StringUtils`：字符串工具
- `ValidateUtils`：校验工具

#### 1.1.7 ByteBuf模块

**核心类**：
- `ByteBufConfig`：ByteBuf配置
- `ByteBufAllocatorFactory`：分配器工厂

---

### 1.2 jwsch-cli 模块

#### 1.2.1 配置模块

**核心类**：
- `ClientConfig`：客户端总配置
- `TcpClientConfig`：TCP客户端配置
- `EventLoopConfig`：EventLoop配置

#### 1.2.2 EventLoop模块

**核心类**：
- `EventLoopHolder`：EventLoop持有者
- `SharedEventLoopManager`：共享EventLoop管理器

#### 1.2.3 TCP客户端模块

**核心类**：
- `TcpClient`：TCP客户端
- `TcpClientInitializer`：Pipeline初始化
- `TcpHandler`：消息处理器
- `ReconnectHandler`：重连处理器

#### 1.2.4 连接池模块

**核心类**：
- `ConnectionPool`：连接池
- `ConnectionPoolManager`：连接池管理器

---

### 1.3 jwsch-srv 模块

#### 1.3.1 配置模块

**核心类**：
- `ServerConfig`：服务端总配置
- `WebSocketServerConfig`：WebSocket配置
- `HttpServerConfig`：HTTP配置
- `TcpServerConfig`：TCP服务端配置

#### 1.3.2 WebSocket服务模块

**核心类**：
- `WebSocketServer`：WebSocket服务端
- `WebSocketInitializer`：Pipeline初始化
- `WebSocketHandler`：消息处理器
- `WebSocketConnectionManager`：连接管理器

#### 1.3.3 连接管理模块

**核心类**：
- `ConnectionRegistry`：连接注册表
- `ConnectionInfo`：连接信息模型
- `ConnectionType`：连接类型枚举
- `ConnectionStatus`：连接状态枚举

#### 1.3.4 注册中心模块

**核心类**：
- `ServiceRegistry`：注册中心接口
- `ServiceInstance`：服务实例模型
- `InMemoryServiceRegistry`：内存实现

#### 1.3.5 负载均衡模块

**核心类**：
- `LoadBalance`：负载均衡接口
- `RandomLoadBalance`：随机实现
- `RoundRobinLoadBalance`：轮询实现
- `ConsistentHashLoadBalance`：一致性哈希实现

#### 1.3.6 消息路由模块

**核心类**：
- `PacketRouter`：消息路由器
- `ResponseMapping`：请求-响应映射

#### 1.3.7 主入口

**核心类**：
- `JwschServer`：服务端主入口

---

### 1.4 连通性测试

**测试类**：
- `WebSocketMockClient`：WebSocket模拟客户端
- `TcpMockServer`：TCP模拟服务端
- `WebSocketConnectionTest`：WebSocket连接测试
- `MessageForwardTest`：消息转发测试
- `EndToEndTest`：端到端测试

---

## 二、Phase 2: 集群支持（第3周）

**目标**：支持多节点集群部署

### 2.1 集群核心

**核心类**：
- `ClusterConfig`：集群配置
- `ClusterServer`：集群服务端
- `ClusterClient`：集群客户端
- `NodeInfo`：节点信息模型
- `NodeStatus`：节点状态枚举

### 2.2 连接同步

**核心类**：
- `ClusterConnectionRegistry`：集群连接映射表
- `ConnectionSyncTask`：连接同步任务
- `ConnectionSyncMessage`：同步消息

### 2.3 集群消息处理

**核心类**：
- `ClusterSyncHandler`：同步处理
- `ClusterForwardHandler`：转发处理
- `ClusterBroadcastHandler`：广播处理

### 2.4 节点注册

**核心类**：
- `ClusterNodeRegistry`：节点注册中心

---

## 三、Phase 3: 可观测性（第4周）

**目标**：完善监控和诊断能力

### 3.1 Metrics模块

**核心类**：
- `MetricsCollector`：指标收集器
- `TopicStatsManager`：Topic统计管理器

### 3.2 HTTP服务模块

**核心类**：
- `HttpServer`：HTTP服务
- `HealthCheckHandler`：健康检查
- `StatsHandler`：统计接口

---

## 四、Phase 4: 高级特性（第5-6周）

**目标**：增强功能和扩展性

### 4.1 消息模式增强

**核心类**：
- `TopicSubscriptionManager`：订阅管理
- `PushManager`：推送管理
- `BroadcastManager`：广播管理

### 4.2 注册中心扩展

**核心类**：
- `NacosServiceRegistry`：Nacos实现
- `ZookeeperServiceRegistry`：ZooKeeper实现

---

## 五、编码顺序

基于依赖关系和测试驱动开发：

1. jwsch-common协议模块 → 协议是基础
2. jwsch-common异常模块 → 异常是基础
3. jwsch-common ID生成 → ID是协议一部分
4. jwsch-common缓存模块 → 被多个模块依赖
5. jwsch-common配置模块 → 被cli和srv依赖
6. jwsch-cli模块 → 依赖common
7. jwsch-srv模块 → 依赖common和cli
8. 连通性测试 → 验证整体功能

---

## 六、测试策略

- **测试驱动开发**：每个类完成后立即编写单元测试
- **覆盖率要求**：
  - jwsch-common：行覆盖率≥80%，分支覆盖率≥70%
  - jwsch-cli：行覆盖率≥75%，分支覆盖率≥65%
  - jwsch-srv：行覆盖率≥70%，分支覆盖率≥60%