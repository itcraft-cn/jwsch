# 集群架构设计

## 一、设计目标

- 支持多节点集群部署
- 节点间消息转发
- 连接信息同步
- 广播扩散机制
- 负载均衡和会话粘性

---

## 二、集群拓扑

### 2.1 架构图

```
                    ┌─────────────────┐
                    │  注册中心        │
                    │ (Nacos/ZK)      │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
        ┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐
        │  jwsch-s1 │  │  jwsch-s2 │  │  jwsch-s3 │
        │  node-01  │  │  node-02  │  │  node-03  │
        └─────┬─────┘  └─────┬─────┘  └─────┬─────┘
              │              │              │
         ┌────┴────┐    ┌────┴────┐    ┌────┴────┐
         │ c1, c3  │    │ c2, c4  │    │ c5, c6  │
         └─────────┘    └─────────┘    └─────────┘
```

### 2.2 节点角色

- **jwsch-s1/s2/s3**：jwsch服务节点
- **c1/c2/...**：前端客户端连接
- **注册中心**：服务发现、配置管理

---

## 三、集群通信

### 3.1 节点间通信协议

**方案**：TCP直连，使用相同协议

**原因**：
- 复用现有协议设计
- 高性能
- 无需额外依赖

### 3.2 集群消息类型

扩展Command类型：

| 命令 | 值 | 说明 |
|------|-----|------|
| CLUSTER_SYNC | 0x10 | 集群连接同步 |
| CLUSTER_FORWARD | 0x11 | 集群消息转发 |
| CLUSTER_BROADCAST | 0x12 | 集群广播扩散 |

---

## 四、连接信息同步

### 4.1 同步策略

**定时同步**：
- 每个节点定时（默认30秒）同步本地连接信息到其他节点
- 使用增量同步，减少网络开销

### 4.2 连接信息结构

```
ClusterConnectionInfo:
- nodeId: String                    // 节点ID
- nodeAddress: String               // 节点地址（IP:port）
- connections: Map<Long, ConnectionMeta>  // 连接元数据

ConnectionMeta:
- connectionId: long                // 连接ID
- connectionType: ConnectionType    // 连接类型
- remoteAddress: String             // 远程地址
- createTime: long                  // 创建时间
- lastActiveTime: long              // 最后活跃时间
```

### 4.3 同步流程

```
定时同步（每30秒）:
1. 收集本地连接元数据
2. 构造CLUSTER_SYNC消息
3. 发送到所有其他节点
4. 其他节点更新本地连接映射表

连接建立时:
1. 立即发送增量同步给其他节点

连接断开时:
1. 立即发送增量同步给其他节点
```

### 4.4 节点连接映射表

```
ClusterConnectionRegistry:
- localConnections: Map<Long, ConnectionInfo>      // 本地连接
- remoteConnections: Map<Long, RemoteConnection>   // 远程连接

RemoteConnection:
- connectionId: long
- nodeId: String                    // 所在节点ID
- nodeAddress: String               // 节点地址
- connectionMeta: ConnectionMeta    // 连接元数据

方法:
- isLocal(connectionId): boolean
- getNodeAddress(connectionId): String
- getLocalConnectionIds(): Set<Long>
- getRemoteConnectionIds(): Set<Long>
```

---

## 五、消息路由策略

### 5.1 点对点消息（REQUEST/RESPONSE/PUSH）

**路由原则**：本地优先，远程其次

```
routeMessage(connectionId, packet):
  if (isLocal(connectionId)):
    // 本地连接
    channel = localConnections.get(connectionId)
    channel.writeAndFlush(packet)
  else:
    // 远程连接
    nodeAddress = clusterRegistry.getNodeAddress(connectionId)
    if (nodeAddress != null):
      // 构造CLUSTER_FORWARD消息
      forwardPacket = new Packet.Builder()
        .command(Command.CLUSTER_FORWARD)
        .sourceId(localNodeId)
        .targetId(connectionId)
        .body(packet.toByteArray())
        .build()
      
      // 转发到目标节点
      clusterClient.send(nodeAddress, forwardPacket)
    else:
      // 找不到连接，返回错误
      sendErrorResponse(packet, ErrorCode.CONNECTION_NOT_FOUND)
```

### 5.2 广播消息（BROADCAST）

**广播原则**：先广播本地连接，再扩散到其他节点

```
broadcast(packet):
  // 1. 广播本地连接
  localConnections = getLocalConnections()
  for (connectionId in localConnections):
    channel = localConnections.get(connectionId)
    channel.writeAndFlush(packet.retain())
  
  // 2. 扩散到其他节点
  otherNodes = clusterRegistry.getOtherNodes()
  for (node in otherNodes):
    // 构造CLUSTER_BROADCAST消息
    broadcastPacket = new Packet.Builder()
      .command(Command.CLUSTER_BROADCAST)
      .sourceId(localNodeId)
      .targetId(0)
      .body(packet.toByteArray())
      .build()
    
    clusterClient.send(node.address, broadcastPacket)
  
  // 释放原始packet
  packet.release()
```

### 5.3 集群消息处理

#### CLUSTER_FORWARD处理

```
handleClusterForward(packet):
  // 解析内部消息
  innerPacket = Packet.fromBytes(packet.getBody())
  
  // 获取目标连接
  targetId = innerPacket.getTargetId()
  
  if (isLocal(targetId)):
    // 本地连接，直接转发
    channel = localConnections.get(targetId)
    channel.writeAndFlush(innerPacket)
  else:
    // 非本地连接，返回错误
    sendClusterError(packet, ErrorCode.CONNECTION_NOT_FOUND)
```

#### CLUSTER_BROADCAST处理

```
handleClusterBroadcast(packet):
  // 解析内部消息
  innerPacket = Packet.fromBytes(packet.getBody())
  
  // 仅广播本地连接（不再扩散，避免循环）
  localConnections = getLocalConnections()
  for (connectionId in localConnections):
    channel = localConnections.get(connectionId)
    channel.writeAndFlush(innerPacket.retain())
  
  innerPacket.release()
```

---

## 六、节点发现与注册

### 6.1 节点注册

```
NodeInfo:
- nodeId: String                    // 节点ID
- nodeAddress: String               // 节点地址（IP:clusterPort）
- websocketPort: int                // WebSocket端口
- httpPort: int                     // HTTP端口
- status: NodeStatus                // 节点状态
- metadata: Map<String, String>     // 元数据

NodeStatus (enum):
- UP: 在线
- DOWN: 下线
```

### 6.2 注册中心集成

```
ClusterNodeRegistry:
- register(NodeInfo node): void
- deregister(String nodeId): void
- getNodes(): List<NodeInfo>
- subscribe(NodeChangeListener listener): void

NodeChangeListener:
- onNodeJoin(NodeInfo node): void
- onNodeLeave(String nodeId): void
```

### 6.3 节点连接管理

```
ClusterClient:
- connections: Map<String, Channel>  // nodeId -> Channel

方法:
- connect(NodeInfo node): void       // 连接到其他节点
- disconnect(String nodeId): void    // 断开连接
- send(String nodeId, Packet packet): void
- broadcast(Packet packet): void
```

---

## 七、会话粘性

### 7.1 设计原则

- 同一客户端的请求尽量路由到同一后端
- 一致性哈希实现会话粘性
- 节点变化时最小化影响

### 7.2 实现

```
ConsistentHashLoadBalance:
- virtualNodes: TreeMap<Long, ServiceInstance>
- virtualNodeCount: int = 160

select(instances, key):
  // key使用connectionId或sessionId
  hash = hash(key)
  entry = virtualNodes.ceilingEntry(hash)
  if (entry == null):
    entry = virtualNodes.firstEntry()
  return entry.getValue()
```

### 7.3 连接ID路由

**场景**：根据connectionId路由到对应节点

```
routeByConnectionId(connectionId):
  if (isLocal(connectionId)):
    return thisNode
  else:
    nodeAddress = clusterRegistry.getNodeAddress(connectionId)
    return getNodeByAddress(nodeAddress)
```

---

## 八、集群配置

### 8.1 配置项

```properties
# 集群配置
jwsch.cluster.enabled=true
jwsch.cluster.node.id=node-01
jwsch.cluster.node.prefix=10
jwsch.cluster.port=9090                    # 集群通信端口
jwsch.cluster.sync.interval=30            # 同步间隔（秒）
jwsch.cluster.connection.timeout=30000    # 连接超时（毫秒）
jwsch.cluster.heartbeat.interval=60       # 心跳间隔（秒）

# 注册中心配置
jwsch.registry.type=NACOS
jwsch.registry.nacos.server-addr=127.0.0.1:8848
jwsch.registry.nacos.namespace=jwsch-cluster
```

### 8.2 配置类

```
ClusterConfig:
- enabled: boolean
- nodeId: String
- nodePrefix: String
- port: int
- syncInterval: int
- connectionTimeout: int
- heartbeatInterval: int
```

---

## 九、集群启动流程

```
启动:
1. 加载配置
2. 生成节点ID
3. 初始化本地连接管理器
4. 初始化集群连接映射表
5. 注册到注册中心
6. 订阅节点变更事件
7. 连接到其他节点
8. 启动集群同步定时任务
9. 启动WebSocket服务
10. 启动HTTP服务

运行:
1. 接收客户端连接
2. 处理消息路由
3. 定时同步连接信息
4. 监听节点变更

关闭:
1. 从注册中心注销
2. 断开与其他节点连接
3. 关闭WebSocket服务
4. 关闭HTTP服务
```

---

## 十、故障处理

### 10.1 节点故障

**检测**：
- 注册中心心跳超时
- 集群连接断开

**处理**：
1. 从节点列表移除故障节点
2. 清理该节点的远程连接映射
3. 通知其他组件

### 10.2 连接故障

**场景**：远程连接断开

**处理**：
1. 尝试重连
2. 重连失败则标记节点为DOWN
3. 清理该节点的连接映射

### 10.3 消息丢失

**场景**：转发消息时目标节点故障

**处理**：
1. 返回错误响应
2. 记录日志
3. 通知业务方

---

## 十一、性能优化

### 11.1 连接信息同步优化

**增量同步**：
- 仅同步变化的连接
- 使用时间戳标识版本

**压缩传输**：
- 连接信息使用紧凑格式
- 批量同步减少网络开销

### 11.2 路由优化

**本地优先**：
- 先查本地连接映射
- 本地无则查远程映射

**缓存优化**：
- 热点连接缓存
- LRU淘汰策略

### 11.3 广播优化

**树形扩散**：
- 避免广播风暴
- 节点组成树状结构

**示例**：
```
s1 -> s2, s3
s2 -> s4, s5
s3 -> s6, s7
```

---

## 十二、监控与可观测性

### 12.1 集群指标

| 指标 | 说明 |
|------|------|
| jwsch.cluster.nodes.total | 集群节点总数 |
| jwsch.cluster.connections.local | 本地连接数 |
| jwsch.cluster.connections.remote | 远程连接数 |
| jwsch.cluster.forward.count | 转发消息数 |
| jwsch.cluster.broadcast.count | 广播扩散数 |
| jwsch.cluster.sync.latency | 同步延迟 |

### 12.2 HTTP端点

| 端点 | 说明 |
|------|------|
| /cluster/nodes | 集群节点列表 |
| /cluster/connections | 集群连接分布 |
| /cluster/stats | 集群统计信息 |

---

## 十三、使用示例

### 13.1 点对点消息

**场景**：c1（连接s1）发送消息给c2（连接s2）

```
流程:
1. c1发送REQUEST到s1
2. s1查找c2的connectionId
3. s1发现c2在s2节点
4. s1构造CLUSTER_FORWARD消息发送到s2
5. s2收到CLUSTER_FORWARD，解析内部消息
6. s2找到本地连接c2，转发消息
7. c2收到消息
```

### 13.2 广播消息

**场景**：c2（连接s2）广播消息到所有客户端

```
流程:
1. c2发送BROADCAST到s2
2. s2广播到本地连接c2, c4
3. s2构造CLUSTER_BROADCAST扩散到s1, s3
4. s1收到CLUSTER_BROADCAST，广播到本地连接c1, c3
5. s3收到CLUSTER_BROADCAST，广播到本地连接c5, c6
6. 所有客户端收到消息
```

---

## 十四、包结构

```
cn.itcraft.jwsch.srv.cluster
├── ClusterServer.java              # 集群服务端
├── ClusterClient.java              # 集群客户端
├── ClusterConfig.java              # 集群配置
├── ClusterConnectionRegistry.java  # 集群连接映射
├── NodeInfo.java                   # 节点信息
├── NodeStatus.java                 # 节点状态
├── ClusterNodeRegistry.java        # 节点注册中心
├── handler/
│   ├── ClusterSyncHandler.java     # 同步处理
│   ├── ClusterForwardHandler.java  # 转发处理
│   └── ClusterBroadcastHandler.java# 广播处理
└── sync/
    ├── ConnectionSyncTask.java     # 连接同步任务
    └── ConnectionSyncMessage.java  # 同步消息
```

---