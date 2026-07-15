# jwsch 集群 Mesh 设计文档

> 版本: 1.0  
> 日期: 2025-07-14  
> 状态: 待审核

## 1. 概述

### 1.1 背景

jwsch 当前集群功能仅有骨架，核心转发逻辑未实现。单节点 Pub 连接导致消息只能广播给本节点订阅者，其他节点的订阅者无法收到。

### 1.2 目标

- 实现集群节点间互联转发，消息可达所有订阅者
- Pub 连接简化，支持多节点策略选择和自动重连
- 配置统一，运维友好

### 1.3 约束

- **不支持通配符订阅**：Topic 匹配精确，不支持 `topic/*` 等模式
- **静态节点配置**：节点列表静态配置，后续可扩展注册中心
- **集群端口复用 TCP 端口**：Pub 连接和集群连接共用同一端口

---

## 2. 整体架构

### 2.1 架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                            Publisher                                 │
│              (TcpClient + NodeSelector + 自动重连)                    │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ TCP (单连接)
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         jwsch-node-01                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │
│  │ TcpServer   │  │ ClusterMesh │  │ WebSocket   │                  │
│  │ (9090)      │  │ Manager     │  │ (8080)      │                  │
│  └─────────────┘  └──────┬──────┘  └─────────────┘                  │
│                          │                                           │
│         ┌────────────────┼────────────────┐                         │
│         ▼                ▼                ▼                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │
│  │TopicHashReg │  │NodeBloom    │  │ClusterConn  │                  │
│  │(topic→hash) │  │(节点级布隆) │  │Registry     │                  │
│  └─────────────┘  └─────────────┘  └─────────────┘                  │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ Cluster TCP (Mesh互联)
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
       jwsch-node-02    jwsch-node-03    ...
```

### 2.2 Mesh 拓扑

- 节点间两两互联，全连接拓扑
- 每对节点间只有一条 TCP 连接（双向通信）
- 任意节点可接收 Pub 消息并转发到其他节点

---

## 3. 核心组件

### 3.1 服务端组件

| 组件 | 职责 |
|------|------|
| **ClusterMeshManager** | 集群生命周期管理，协调节点发现、连接、心跳 |
| **ClusterForwarder** | 消息转发逻辑（PUSH/BROADCAST/REQUEST） |
| **ClusterSyncService** | 连接信息同步（定时 + 事件触发） |
| **ClusterServerHandler** | 处理其他节点的连接和消息 |
| **ClusterConnectionRegistry** | 本地/远程连接路由表 |
| **TopicHashRegistry** | topic → topicHash 映射（xxHash） |
| **NodeBloomFilter** | 节点级布隆过滤器，快速判断是否有订阅 |
| **TopicSubscription** | 改造为 topicHash → Set<Long> |

### 3.2 客户端组件

| 组件 | 职责 |
|------|------|
| **TcpClient** | 增加重连机制，支持多地址 |
| **NodeSelector** | 节点选择策略接口 |
| **RandomSelector** | 随机选择 |
| **RoundRobinSelector** | 轮询选择 |
| **PrioritySelector** | 优先 base-port |
| **SingleSelector** | 单地址（兼容负载均衡器） |

---

## 4. Topic Hash 设计

### 4.1 Hash 算法

使用 **xxHash64**，64-bit 输出，分布均匀，速度最快。

```java
public final class TopicHash {
    private static final xxHash64 HASHER = xxHashFactory.instance().hash64();
    
    public static long hash(String topic) {
        ByteBuf buf = Unpooled.copiedBuffer(topic, StandardCharsets.UTF_8);
        return HASHER.hash(buf);
    }
}
```

### 4.2 TopicSubscription 改造

```java
public class TopicSubscription {
    private final ConcurrentMap<Long, Set<Long>> hashSubscribers;
    private final ConcurrentMap<Long, Set<Long>> connectionTopics;
    
    public void subscribe(String topic, long connectionId) {
        long topicHash = TopicHash.hash(topic);
        hashSubscribers.computeIfAbsent(topicHash, k -> ConcurrentHashMap.newKeySet()).add(connectionId);
        connectionTopics.computeIfAbsent(connectionId, k -> ConcurrentHashMap.newKeySet()).add(topicHash);
    }
    
    public Set<Long> getSubscribers(String topic) {
        return hashSubscribers.getOrDefault(TopicHash.hash(topic), Collections.emptySet());
    }
}
```

---

## 5. 节点级布隆过滤器

### 5.1 用途

快速判断"本节点是否有订阅 topic X 的连接"，无则跳过转发。

### 5.2 实现

```java
public class NodeBloomFilter {
    private final BloomFilter<Long> bloom;
    private static final double FALSE_POSITIVE_RATE = 0.03;
    
    public NodeBloomFilter(int expectedTopics) {
        this.bloom = BloomFilter.create(Funnels.longFunnel(), expectedTopics, FALSE_POSITIVE_RATE);
    }
    
    public void addTopic(long topicHash) {
        bloom.put(topicHash);
    }
    
    public boolean mightHaveTopic(long topicHash) {
        return bloom.mightContain(topicHash);
    }
    
    public void rebuild(Set<Long> topicHashes) {
        // 定时重建，避免累积误差
    }
}
```

### 5.3 内存估算

| 本节点订阅 Topic 数 | 内存占用 |
|-------------------|---------|
| 1万 | ~9 KB |
| 10万 | ~91 KB |

---

## 6. 集群协议

### 6.1 命令定义

| 命令 | Cmd | 用途 |
|------|-----|------|
| CLUSTER_JOIN | 0x10 | 新节点加入，携带自身信息 |
| CLUSTER_MEMBERSHIP | 0x11 | 返回已知节点列表 |
| CLUSTER_SYNC | 0x12 | 全量/增量同步连接信息 |
| CLUSTER_FORWARD | 0x13 | 转发 REQUEST 消息 |
| CLUSTER_BROADCAST | 0x14 | 扩散 PUSH/BROADCAST 消息 |
| CLUSTER_HEARTBEAT | 0x15 | 节点间心跳 |

### 6.2 消息格式

**CLUSTER_JOIN:**
```
| Cmd(1B) | NodeIdLen(1B) | NodeId(NB) | HostLen(1B) | Host(NB) | Port(4B) |
```

**CLUSTER_MEMBERSHIP:**
```
| Cmd(1B) | NodeCount(2B) | NodeInfo1 | NodeInfo2 | ... |
```

**CLUSTER_SYNC:**
```
| Cmd(1B) | SyncType(1B) | DataLen(4B) | Data(NB) |

SyncType:
  0x01 = FULL (全量)
  0x02 = INCREMENTAL (增量)
  
Data (增量):
  | OpCount(2B) | Op1 | Op2 | ... |
  
Op:
  | OpType(1B) | ConnectionId(8B) | TopicHashCount(2B) | TopicHashes(NB) |
  
OpType:
  0x01 = ADD_CONNECTION
  0x02 = REMOVE_CONNECTION
  0x03 = ADD_SUBSCRIPTION
  0x04 = REMOVE_SUBSCRIPTION
```

---

## 7. 消息路由

### 7.1 PUSH 流程

```
1. Pub → Node-A: PUSH(topic=X, body)
2. Node-A:
   a. 本地 broadcastToTopic(topicHash)
   b. 遍历其他节点:
      - if (nodeBloom.mightHaveTopic(topicHash)):
          → CLUSTER_BROADCAST(targetNodeId, topicHash, body)
3. Node-B 收到 CLUSTER_BROADCAST:
   - 本地 broadcastToTopic(topicHash)
   - 不再扩散（避免循环）
```

### 7.2 BROADCAST 流程

```
1. Pub → Node-A: BROADCAST(body)
2. Node-A:
   a. 本地 broadcast() 给所有连接
   b. 遍历其他节点:
       → CLUSTER_BROADCAST(targetNodeId, body)
3. Node-B 收到 CLUSTER_BROADCAST:
   - 本地 broadcast()
   - 不再扩散
```

### 7.3 REQUEST 流程

```
1. Pub → Node-A: REQUEST(targetId=Y, body)
2. Node-A:
   a. if (localConnections.contains(Y)):
         → routeToFrontend(Y, body)
   b. else:
         → 查 ClusterConnectionRegistry 找到 Y 所在节点
         → CLUSTER_FORWARD(targetNodeId, targetId=Y, body)
3. 目标节点收到 CLUSTER_FORWARD:
   → routeToFrontend(Y, body)
```

---

## 8. 连接同步

### 8.1 同步内容

- connectionId (8 bytes)
- topicHashes (该连接订阅的所有 topic hash)

### 8.2 触发时机

| 事件 | 同步类型 |
|------|---------|
| WS 连接建立 | 增量：ADD_CONNECTION |
| WS 连接断开 | 增量：REMOVE_CONNECTION |
| 订阅 topic | 增量：ADD_SUBSCRIPTION |
| 取消订阅 | 增量：REMOVE_SUBSCRIPTION |
| 定时（30秒） | 全量：FULL |

### 8.3 内存估算

假设 1万连接，每连接订阅 100 topics：

| 项目 | 计算 | 内存 |
|------|------|------|
| 订阅关系 | 1万 × 100 × 80 bytes | ~80 MB |
| 节点级布隆 | 10万 topics × 7.3 bits | ~91 KB |
| **总计** | | **~80 MB** |

---

## 9. 节点发现与连接

### 9.1 节点 ID 生成

格式：`{node-prefix}-{advertise-host}-{bind-port}`

示例：`jwsch-192.168.1.10-9090`

其中 `bind-port` 为实际绑定成功的端口（base-port 或 base-port+offset）。

### 9.2 启动流程

**base-port 节点 (9090):**
```
1. 绑定 base-port 成功
2. 监听等待其他节点连接
3. 收到 CLUSTER_JOIN → 记录节点信息 → 返回 CLUSTER_MEMBERSHIP
```

**非 base-port 节点 (9091/9092):**
```
1. 绑定 base-port 失败 → 尝试 base-port+1 ...
2. 绑定成功后，等待 startup-wait-seconds
3. 连接所有 base-port 节点
4. 全失败 → 记录日志 → 休眠 startup-wait-seconds → 重试
5. 连接成功 → 发送 CLUSTER_JOIN(自身信息)
6. 收到 CLUSTER_MEMBERSHIP → 连接其他节点
```

### 9.3 心跳检测

- 心跳间隔：10 秒
- 超时时间：30 秒
- 超时处理：断开连接 → 清理该节点所有连接信息

---

## 10. 配置设计

### 10.1 服务端配置

```yaml
jwsch:
  cluster:
    enabled: true
    node-prefix: "jwsch"
    base-port: 9090
    port-range: 3
    startup-wait-seconds: 5
    sync-interval-seconds: 30
    heartbeat-interval-seconds: 10
    heartbeat-timeout-seconds: 30
    nodes:
      - host: "192.168.1.10"
      - host: "192.168.1.11"
      - host: "192.168.1.12"
```

**advertise-host**: 本节点对外可达地址，用于生成 nodeId 和 CLUSTER_JOIN 中告知其他节点如何回连。**不放在配置文件中**（配置文件需全集群一致），通过环境变量或 JVM 参数注入：

- 环境变量：`JWSCH_ADVERTISE_HOST=192.168.1.10`
- JVM 参数：`-Djwsch.advertise.host=192.168.1.10`

优先级：JVM 参数 > 环境变量 > 自动检测（取第一个非 loopback 地址）。单网卡场景可省略。

**单机多实例：**

```yaml
jwsch:
  cluster:
    enabled: true
    node-prefix: "jwsch"
    base-port: 9090
    port-range: 3
    nodes:
      - host: "127.0.0.1"
```

### 10.2 客户端配置

```yaml
jwsch:
  client:
    nodes:
      - host: "192.168.1.10"
      - host: "192.168.1.11"
    base-port: 9090
    port-range: 3
    selector: "round-robin"
    reconnect-delay-seconds: 5
```

**地址展开：**
- 192.168.1.10:9090, 9091, 9092
- 192.168.1.11:9090, 9091, 9092

共 6 个候选地址。

### 10.3 NodeSelector 策略

| 策略 | selector 值 | 说明 |
|------|------------|------|
| RandomSelector | `random` | 随机选择 |
| RoundRobinSelector | `round-robin` | 轮询选择 |
| PrioritySelector | `priority` | 优先 base-port |
| SingleSelector | `single` | 单地址，兼容负载均衡器 |

---

## 11. 故障处理

### 11.1 节点故障

| 场景 | 处理 |
|------|------|
| 心跳超时 | 断开连接，清理该节点所有连接信息 |
| TCP 断开 | 同上 |
| 重连成功 | 重新同步连接信息 |

### 11.2 Pub 故障

| 场景 | 处理 |
|------|------|
| 连接断开 | 通过 NodeSelector 选择新节点重连 |
| 重连失败 | 休眠 reconnect-delay-seconds 后重试 |

---

## 12. 文件改动清单

### 12.1 jwsch-common

| 文件 | 改动 |
|------|------|
| `Command.java` | + CLUSTER_JOIN/MEMBERSHIP/SYNC/FORWARD/BROADCAST/HEARTBEAT |
| `TopicHash.java` | 新增，xxHash 计算 |

### 12.2 jwsch-srv

| 文件 | 改动 |
|------|------|
| `ClusterConfig.java` | + nodes/base-port/port-range/startup-wait 等配置 |
| `ClusterMeshManager.java` | 新增，集群生命周期管理 |
| `ClusterForwarder.java` | 新增，消息转发 |
| `ClusterSyncService.java` | 新增，连接同步 |
| `ClusterServerHandler.java` | 实现消息处理 |
| `ClusterMessageEncoder.java` | 实现 |
| `ClusterMessageDecoder.java` | 实现 |
| `NodeBloomFilter.java` | 新增，节点级布隆 |
| `TopicSubscription.java` | 改造，topic → topicHash |
| `PacketRouter.java` | 集成 ClusterForwarder |
| `JwschServer.java` | 启动 ClusterMeshManager |

### 12.3 jwsch-cli

| 文件 | 改动 |
|------|------|
| `TcpClient.java` | + 重连 + 多地址 |
| `TcpClientConfig.java` | + base-port/port-range/selector 配置 |
| `NodeSelector.java` | 新增，策略接口 |
| `RandomSelector.java` | 新增 |
| `RoundRobinSelector.java` | 新增 |
| `PrioritySelector.java` | 新增 |
| `SingleSelector.java` | 新增 |

---

## 13. 测试计划

### 13.1 单元测试

- TopicHash 唯一性测试
- NodeBloomFilter 误判率测试
- ClusterSync 序列化/反序列化测试
- NodeSelector 策略测试

### 13.2 集成测试

- 单机多实例启动测试（9090/9091/9092）
- 节点发现与连接测试
- PUSH/BROADCAST/REQUEST 转发测试
- 节点故障恢复测试
- Pub 重连测试

### 13.3 压力测试

- 集群消息转发性能
- 连接同步性能
- 大量订阅场景内存验证

---

## 14. 后续扩展

### 14.1 注册中心集成

预留 `ClusterNodeRegistry` 接口，实现：
- `InMemoryClusterNodeRegistry`（当前）
- `NacosClusterNodeRegistry`（后续）
- `ZookeeperClusterNodeRegistry`（后续）

### 14.2 动态扩缩容

- 节点上线：广播 CLUSTER_JOIN
- 节点下线：心跳超时自动清理
