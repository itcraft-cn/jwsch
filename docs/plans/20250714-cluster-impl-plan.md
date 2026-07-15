# jwsch 集群 Mesh 实现计划

> 日期: 2025-07-14  
> 基于设计文档: `doc/design/2025-07-14-cluster-mesh-design.md`

## 退出条件

1. 全功能按 plan 开发完毕
2. 集成测试通过：
   - 1 pub + 2 server(A/B) + 3 sub(s1/s2/s3)
   - s1 订阅 a.b.c，s2 订阅 a.b.d，s3 订阅 a.b.e
   - Pub 按 targetId 发 s1 ✓
   - Pub 按 topic 发 s2 ✓
   - Pub 广播发 s1/s2/s3 ✓

---

## Phase 1: jwsch-common

### 1.1 Command.java 新增集群命令

| 命令 | Cmd | 位置 |
|------|-----|------|
| CLUSTER_JOIN | 0x10 | 其他节点加入 |
| CLUSTER_MEMBERSHIP | 0x11 | 返回节点列表 |
| CLUSTER_SYNC | 0x12 | 连接信息同步 |
| CLUSTER_FORWARD | 0x13 | 转发 REQUEST |
| CLUSTER_BROADCAST | 0x14 | 扩散 PUSH/BROADCAST |
| CLUSTER_HEARTBEAT | 0x15 | 节点心跳 |

### 1.2 TopicHash.java 新增

- xxHash64 计算 topic hash
- 依赖：`net.openhft:zero-allocation-hashing:0.16` (已有)

**输出物**: `TopicHash.java`

---

## Phase 2: jwsch-srv 基础组件

### 2.1 TopicSubscription.java 改造

**改动**:
- `ConcurrentMap<String, Set<Long>>` → `ConcurrentMap<Long, Set<Long>>` (topicHash → connectionIds)
- 新增 `ConcurrentMap<Long, Set<Long>> connectionTopics` (connectionId → topicHashes)
- 方法改为 `subscribe(String topic, long connectionId)` 内部计算 hash

### 2.2 NodeBloomFilter.java 新增

**功能**:
- Guava BloomFilter<Long>
- `addTopic(long topicHash)`
- `mightHaveTopic(long topicHash)`
- `rebuild(Set<Long> topicHashes)`

**输出物**: `NodeBloomFilter.java`

---

## Phase 3: jwsch-srv 配置与注册表

### 3.1 ClusterConfig.java 改造

**新增配置项**:
```yaml
enabled: boolean
node-prefix: String
base-port: int
port-range: int
startup-wait-seconds: int
sync-interval-seconds: int
heartbeat-interval-seconds: int
heartbeat-timeout-seconds: int
nodes: List<NodeInfo>
```

### 3.2 ClusterConnectionRegistry.java 改造

**功能**:
- `localConnections`: Set<Long> 本地连接
- `remoteConnections`: Map<String, Set<Long>> nodeId → connectionIds
- `connectionToNode`: Map<Long, String> connectionId → nodeId
- 方法: `addLocalConnection()`, `removeLocalConnection()`, `addRemoteConnection()`, `findNodeForConnection()`

---

## Phase 4: jwsch-srv 协议编解码

### 4.1 ClusterMessage.java 新增

**基类**，所有集群消息继承:
- `getCmd(): Command`
- `encode(ByteBuf): void`
- `decode(ByteBuf): void`

### 4.2 ClusterJoin.java 新增

**字段**:
- nodeId: String
- host: String
- port: int

### 4.3 ClusterMembership.java 新增

**字段**:
- nodes: List<NodeInfo>

### 4.4 ClusterSync.java 新增

**字段**:
- syncType: SyncType (FULL/INCREMENTAL)
- operations: List<SyncOp>

**SyncOp**:
- opType: OpType (ADD_CONNECTION/REMOVE_CONNECTION/ADD_SUBSCRIPTION/REMOVE_SUBSCRIPTION)
- connectionId: long
- topicHashes: Set<Long>

### 4.5 ClusterForward.java 新增

**字段**:
- targetNodeId: String (可选，用于定向转发)
- targetId: long
- originalPacket: Packet (REQUEST)

### 4.6 ClusterBroadcast.java 新增

**字段**:
- sourceNodeId: String
- topicHash: long (可选，PUSH 时有)
- originalCmd: Command (PUSH/BROADCAST)
- body: ByteBuf

### 4.7 ClusterHeartbeat.java 新增

**字段**: 无 (空消息)

### 4.8 ClusterEncoder/ClusterDecoder

**ClusterEncoder**: ClusterMessage → ByteBuf  
**ClusterDecoder**: ByteBuf → ClusterMessage

---

## Phase 5: jwsch-srv 节点管理

### 5.1 ClusterMeshManager.java 新增

**职责**: 集群生命周期管理

**方法**:
- `start()`: 启动集群 (绑定端口，连接其他节点)
- `stop()`: 停止集群
- `getClusterClient()`: 获取 ClusterClient 引用
- `getKnownNodes()`: 获取已知节点列表

**启动流程**:
1. 解析 advertise-host (JVM 参数 > 环境变量 > 自动检测)
2. 绑定 base-port，失败则尝试 base-port+1...
3. 非base-port节点: 等待 startup-wait-seconds，连接 base-port 节点
4. 发送 CLUSTER_JOIN，接收 CLUSTER_MEMBERSHIP
5. 连接其他节点

### 5.2 ClusterServerHandler.java 改造

**处理消息**:
- CLUSTER_JOIN: 记录节点信息，返回 CLUSTER_MEMBERSHIP
- CLUSTER_MEMBERSHIP: 连接新节点
- CLUSTER_SYNC: 更新 ClusterConnectionRegistry
- CLUSTER_FORWARD: 调用 ClusterForwarder.forwardToLocal()
- CLUSTER_BROADCAST: 调用 ClusterForwarder.broadcastLocally()
- CLUSTER_HEARTBEAT: 更新心跳时间

---

## Phase 6: jwsch-srv 转发与同步

### 6.1 ClusterForwarder.java 新增

**职责**: 消息转发逻辑

**方法**:
- `forwardRequest(Packet request)`: REQUEST 转发 (查 ClusterConnectionRegistry 找目标节点)
- `broadcastPush(Packet push)`: PUSH 广播 (查 NodeBloomFilter 过滤)
- `broadcastAll(Packet broadcast)`: BROADCAST 广播 (发给所有节点)
- `forwardToLocal(long connectionId, Packet packet)`: 转发到本地连接

### 6.2 ClusterSyncService.java 新增

**职责**: 连接信息同步

**方法**:
- `onLocalConnectionAdd(long connectionId)`: 增量同步 ADD_CONNECTION
- `onLocalConnectionRemove(long connectionId)`: 增量同步 REMOVE_CONNECTION
- `onLocalSubscribe(long connectionId, long topicHash)`: 增量同步 ADD_SUBSCRIPTION
- `onLocalUnsubscribe(long connectionId, long topicHash)`: 增量同步 REMOVE_SUBSCRIPTION
- `scheduleFullSync()`: 定时全量同步

---

## Phase 7: jwsch-srv 路由集成

### 7.1 PacketRouter.java 改造

**注入**: ClusterForwarder

**改动**:
- `route(Packet)`: 
  - REQUEST: 调用 `clusterForwarder.forwardRequest(packet)`
  - PUSH: 调用 `clusterForwarder.broadcastPush(packet)`
  - BROADCAST: 调用 `clusterForwarder.broadcastAll(packet)`

### 7.2 JwschServer.java 改造

**改动**:
- 启动时创建 ClusterMeshManager
- 注入 ClusterForwarder 到 PacketRouter

---

## Phase 8: jwsch-cli 客户端改造

### 8.1 NodeSelector 接口新增

```java
public interface NodeSelector {
    InetSocketAddress select(List<InetSocketAddress> candidates);
    void onConnectSuccess(InetSocketAddress address);
    void onConnectFailed(InetSocketAddress address);
}
```

### 8.2 NodeSelector 实现

| 实现 | 策略 |
|------|------|
| RandomSelector | 随机选择 |
| RoundRobinSelector | 轮询选择 |
| PrioritySelector | 优先 base-port |
| SingleSelector | 单地址 |

### 8.3 TcpClient.java 改造

**新增**:
- `addresses: List<InetSocketAddress>` (展开后的所有候选地址)
- `nodeSelector: NodeSelector`
- `reconnectDelay: int`

**重连逻辑**:
- 连接断开 → `nodeSelector.select(addresses)` → 连接
- 失败 → 休眠 reconnectDelay → 重试

### 8.4 TcpClientConfig.java 改造

**新增配置**:
```yaml
nodes: List<String> (host)
base-port: int
port-range: int
selector: String (random/round-robin/priority/single)
reconnect-delay-seconds: int
```

---

## Phase 9: 集成测试

### 9.1 测试场景

```
┌─────────────┐
│   Publisher │
└──────┬──────┘
       │ TCP
       ▼
┌─────────────┐      Cluster TCP      ┌─────────────┐
│  Server-A   │◄─────────────────────►│  Server-B   │
│   (9090)    │                       │   (9091)    │
└──────┬──────┘                       └──────┬──────┘
       │ WS                                  │ WS
       ▼                                     ▼
┌─────────────┐                       ┌─────────────┐
│  Sub-s1     │                       │  Sub-s2     │
│  订阅a.b.c  │                       │  订阅a.b.d  │
└─────────────┘                       └─────────────┘
                                             
┌─────────────┐
│  Sub-s3     │ (连接到A或B)
│  订阅a.b.e  │
└─────────────┘
```

### 9.2 测试用例

| 用例 | 操作 | 预期结果 |
|------|------|----------|
| REQUEST by targetId | Pub 发 REQUEST(targetId=s1) | s1 收到消息 |
| PUSH by topic | Pub 发 PUSH(topic=a.b.d) | s2 收到消息 |
| BROADCAST | Pub 发 BROADCAST | s1/s2/s3 都收到 |
| 节点故障 | 停止 Server-A | Pub 重连到 Server-B，消息正常转发 |

### 9.3 测试代码位置

`jwsch-test/src/test/java/cn/itcraft/jwsch/srv/cluster/ClusterIntegrationTest.java`

---

## 验证策略

### 单元验证

- TopicHash 唯一性
- NodeBloomFilter 误判率
- ClusterSync 序列化/反序列化
- NodeSelector 策略

### 集成验证

- 单机多实例启动 (9090/9091/9092)
- 节点发现与连接
- 消息转发正确性
- 故障恢复

---

## 回滚策略

1. Git tag: `pre-cluster-impl`
2. 如失败，回退到 tag
3. 配置 `cluster.enabled: false` 可禁用集群功能