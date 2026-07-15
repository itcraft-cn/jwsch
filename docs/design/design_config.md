# 配置管理设计

## 一、配置文件

### 1.1 文件名

`jwsch.properties`

### 1.2 配置文件位置

- 默认位置：classpath根目录
- 可通过JVM参数指定：`-Djwsch.config.file=/path/to/config.properties`

---

## 二、配置项分类

### 2.1 节点配置

```properties
jwsch.node.prefix=10  # 必填
```

### 2.2 服务端配置

```properties
# WebSocket服务端
jwsch.srv.enabled=true
jwsch.srv.websocket.port=8080
jwsch.srv.websocket.boss.threads=1
jwsch.srv.websocket.worker.threads=8

# HTTP服务端
jwsch.srv.http.enabled=true
jwsch.srv.http.port=8081
```

### 2.3 客户端配置

```properties
jwsch.cli.enabled=true
jwsch.cli.eventloop.shared=true
jwsch.cli.eventloop.worker.threads=8
```

### 2.4 协议配置

```properties
jwsch.protocol.body.length.default=99999
jwsch.protocol.body.length.max=9999999
```

### 2.5 ID生成配置

```properties
jwsch.id.hash.seed=0x1234ABCD
```

### 2.6 ByteBuf配置

```properties
jwsch.bytebuf.pool.enabled=true
jwsch.bytebuf.pool.direct=true
jwsch.bytebuf.pool.leak.detection=SIMPLE
```

### 2.7 连接管理配置

```properties
jwsch.connection.max=10000
jwsch.connection.heartbeat.interval=60
jwsch.connection.heartbeat.timeout=180
```

### 2.8 负载均衡配置

```properties
jwsch.loadbalance.strategy=ROUND_ROBIN
jwsch.loadbalance.retry.times=3
jwsch.loadbalance.failure.threshold=5
```

### 2.9 注册中心配置

```properties
jwsch.registry.type=MEMORY
```

---

## 三、配置类设计

### 3.1 JwschConfig（总配置）

```
JwschConfig:
- nodePrefix: String
- serverConfig: ServerConfig
- clientConfig: ClientConfig
- protocolConfig: ProtocolConfig
- idConfig: IdConfig
- bytebufConfig: ByteBufConfig
- connectionConfig: ConnectionConfig
- loadbalanceConfig: LoadBalanceConfig
- registryConfig: ServiceRegistryConfig
```

### 3.2 ServerConfig

```
ServerConfig:
- enabled: boolean
- websocketConfig: WebSocketServerConfig
- httpConfig: HttpServerConfig
```

### 3.3 ClientConfig

```
ClientConfig:
- enabled: boolean
- eventLoopConfig: EventLoopConfig
- tcpConfig: TcpClientConfig
```

---

## 四、配置加载器

### 4.1 ConfigLoader

```
ConfigLoader:
- load(): JwschConfig
- load(String filePath): JwschConfig
- load(InputStream inputStream): JwschConfig
```

### 4.2 加载流程

```
1. 查找配置文件
2. 读取配置文件
3. 解析配置项
4. 构建配置对象
5. 校验配置
6. 返回配置对象
```

---

## 五、配置校验

### 5.1 ConfigValidator

```
ConfigValidator:
- validate(JwschConfig config): void
```

### 5.2 校验规则

1. 必填项校验
2. 参数范围校验
3. 依赖关系校验
4. 逻辑一致性校验

---

## 六、完整配置文件示例

```properties
# ==================== jwsch配置文件 ====================

# 节点配置
jwsch.node.prefix=10

# ==================== 服务端配置 ====================
jwsch.srv.enabled=true

jwsch.srv.websocket.port=8080
jwsch.srv.websocket.boss.threads=1
jwsch.srv.websocket.worker.threads=8
jwsch.srv.websocket.tcp.nodelay=true
jwsch.srv.websocket.tcp.backlog=2048
jwsch.srv.websocket.tcp.sndbuf=1048576
jwsch.srv.websocket.tcp.rcvbuf=1048576
jwsch.srv.websocket.tcp.write.buffer.low=32768
jwsch.srv.websocket.tcp.write.buffer.high=65536
jwsch.srv.websocket.timeout.connect=30000
jwsch.srv.websocket.timeout.read=0
jwsch.srv.websocket.timeout.write=0

jwsch.srv.http.enabled=true
jwsch.srv.http.port=8081

# ==================== 客户端配置 ====================
jwsch.cli.enabled=true
jwsch.cli.eventloop.shared=true
jwsch.cli.eventloop.worker.threads=8

jwsch.cli.tcp.nodelay=true
jwsch.cli.tcp.keepalive=true
jwsch.cli.tcp.sndbuf=1048576
jwsch.cli.tcp.rcvbuf=1048576
jwsch.cli.tcp.write.buffer.low=32768
jwsch.cli.tcp.write.buffer.high=65536
jwsch.cli.tcp.timeout.connect=30000
jwsch.cli.tcp.timeout.read=0
jwsch.cli.tcp.timeout.write=0

# ==================== 协议配置 ====================
jwsch.protocol.body.length.default=99999
jwsch.protocol.body.length.max=9999999

# ==================== ID生成配置 ====================
jwsch.id.hash.seed=0x1234ABCD

# ==================== ByteBuf配置 ====================
jwsch.bytebuf.pool.enabled=true
jwsch.bytebuf.pool.direct=true
jwsch.bytebuf.pool.leak.detection=SIMPLE

# ==================== 连接管理配置 ====================
jwsch.connection.max=10000
jwsch.connection.heartbeat.interval=60
jwsch.connection.heartbeat.timeout=180

# ==================== 负载均衡配置 ====================
jwsch.loadbalance.strategy=ROUND_ROBIN
jwsch.loadbalance.retry.times=3
jwsch.loadbalance.failure.threshold=5

# ==================== 注册中心配置 ====================
jwsch.registry.type=MEMORY
```

---