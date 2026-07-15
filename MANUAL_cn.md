# jwsch 集群手册

[English](MANUAL.md) | [README](README_cn.md) | [变更日志](CHANGELOG_cn.md)

## 目录

1. [集群架构](#1-集群架构)
2. [配置指南](#2-配置指南)
3. [节点部署](#3-节点部署)
4. [消息路由](#4-消息路由)
5. [运维监控](#5-运维监控)
6. [故障排查](#6-故障排查)

---

## 1. 集群架构

### 1.1 拓扑结构

jwsch 集群采用 Mesh 拓扑，所有节点之间两两互联：

```
                    ┌─────────────┐
                    │  Node-B     │
                    │  9091       │
                    └──────┬──────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
   │  Node-A     │  │  Node-C     │  │  Node-D     │
   │  9090       │  │  9092       │  │  9093       │
   └─────────────┘  └─────────────┘  └─────────────┘
          ▲                ▲                ▲
          │                │                │
          └────────────────┼────────────────┘
                           │
                    ┌──────┴──────┐
                    │  Node-E     │
                    │  9094       │
                    └─────────────┘
```

### 1.2 节点角色

| 角色 | 说明 |
|------|------|
| **Base Node** | `cluster-port == base-port` 的节点，新节点启动时首先连接此节点 |
| **Non-Base Node** | `cluster-port != base-port` 的节点，启动后自动连接 Base Node |

### 1.3 端口分配

```
base-port = 9090
port-range = 3

→ 可用端口: 9090, 9091, 9092
→ Node-A: bind-port=9090 (Base)
→ Node-B: bind-port=9091 (Non-Base)
→ Node-C: bind-port=9092 (Non-Base)
```

---

## 2. 配置指南

### 2.1 YAML 配置

**单节点配置 (node1.yaml)**:

```yaml
cluster:
  enabled: true
  node-prefix: jwsch-prod
  base-port: 9090
  port-range: 3
  bind-port: 9090
  startup-wait-seconds: 5
  sync-interval-seconds: 30
  heartbeat-interval-seconds: 10
  heartbeat-timeout-seconds: 30
  nodes:
    - host: 192.168.1.10
    - host: 192.168.1.11
    - host: 192.168.1.12

websocket:
  port: 8080

tcp:
  port: 9090
```

**节点 B 配置 (node2.yaml)**:

```yaml
cluster:
  enabled: true
  node-prefix: jwsch-prod
  base-port: 9090
  port-range: 3
  bind-port: 9091
  nodes:
    - host: 192.168.1.10
    - host: 192.168.1.11
    - host: 192.168.1.12

websocket:
  port: 8081

tcp:
  port: 9091
```

### 2.2 配置项说明

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enabled` | boolean | false | 是否启用集群 |
| `node-prefix` | String | jwsch | 节点前缀，用于生成 nodeId |
| `base-port` | int | 9090 | 集群基准端口 |
| `port-range` | int | 3 | 端口范围 |
| `bind-port` | int | -1 | 绑定端口，-1 表示使用 base-port |
| `startup-wait-seconds` | int | 5 | 非基端口节点启动等待时间 |
| `sync-interval-seconds` | int | 30 | 全量同步间隔 |
| `heartbeat-interval-seconds` | int | 10 | 心跳发送间隔 |
| `heartbeat-timeout-seconds` | int | 30 | 心跳超时时间 |
| `nodes` | List | [] | 集群节点列表 |

### 2.3 advertise-host 配置

节点对外通信地址，三种配置方式：

**方式1: JVM 参数（推荐）**

```bash
java -Djwsch.advertise.host=192.168.1.10 -jar jwschd.jar
```

**方式2: 环境变量**

```bash
export JWSCH_ADVERTISE_HOST=192.168.1.10
java -jar jwschd.jar
```

**方式3: 自动检测**

```bash
java -jar jwschd.jar
# 自动取第一个非回环网卡地址
```

**优先级**: JVM 参数 > 环境变量 > 自动检测

---

## 3. 节点部署

### 3.1 启动顺序

```
1. 启动 Node-A (Base Node, bind-port=9090)
2. 等待 5 秒
3. 启动 Node-B (bind-port=9091) → 自动连接 Node-A
4. 启动 Node-C (bind-port=9092) → 自动连接 Node-A
```

### 3.2 启动脚本示例

**start-cluster.sh**:

```bash
#!/bin/bash

NODES=("192.168.1.10" "192.168.1.11" "192.168.1.12")
BASE_PORT=9090
PORT_RANGE=3

for i in "${!NODES[@]}"; do
  HOST=${NODES[$i]}
  BIND_PORT=$((BASE_PORT + i))
  
  ssh $HOST "java -Djwsch.advertise.host=$HOST \
    -jar /opt/jwsch/jwschd.jar \
    --config /opt/jwsch/config/node$((i+1)).yaml &"
  
  if [ $i -eq 0 ]; then
    echo "Waiting for base node to start..."
    sleep 5
  fi
done
```

### 3.3 Docker 部署

**docker-compose.yml**:

```yaml
version: '3'
services:
  jwsch-node1:
    image: jwsch:latest
    environment:
      - JWSCH_ADVERTISE_HOST=192.168.1.10
    command: ["--config", "/config/node1.yaml"]
    ports:
      - "8080:8080"
      - "9090:9090"
  
  jwsch-node2:
    image: jwsch:latest
    environment:
      - JWSCH_ADVERTISE_HOST=192.168.1.11
    command: ["--config", "/config/node2.yaml"]
    ports:
      - "8081:8081"
      - "9091:9091"
    depends_on:
      - jwsch-node1
```

---

## 4. 消息路由

### 4.1 REQUEST (按 targetId 路由)

```
Publisher → Server-A → 查找 targetId 所在节点
                       ↓
            如果在本地 → 直接发送
            如果在远程 → CLUSTER_FORWARD 到目标节点
```

### 4.2 PUSH (按 topic 路由)

```
Publisher → Server-A → 查找订阅 topic 的连接
                       ↓
            本地有订阅 → 直接发送
            远程有订阅 → CLUSTER_BROADCAST 扩散
                       ↓
            Server-B 收到 → 检查本地订阅 → 发送给订阅者
```

### 4.3 BROADCAST (全网扩散)

```
Publisher → Server-A → CLUSTER_BROADCAST 到所有节点
                       ↓
            所有节点收到 → 发送给本地所有连接
```

### 4.4 BloomFilter 优化

每个节点维护一个 BloomFilter 记录本节点订阅的 topic hash：

```java
// 发送 PUSH 前
if (!remoteNode.getBloomFilter().mightHaveTopic(topicHash)) {
    // 该节点肯定没有订阅此 topic，跳过
    return;
}
// 可能订阅（3% 误判率），发送 CLUSTER_BROADCAST
```

---

## 5. 运维监控

### 5.1 日志关键字

| 关键字 | 说明 |
|--------|------|
| `Cluster started` | 节点启动完成 |
| `Node joined` | 新节点加入 |
| `Node left` | 节点离开 |
| `Cluster sync` | 同步事件 |
| `Cluster forward` | 消息转发 |

### 5.2 关键指标

| 指标 | 说明 |
|------|------|
| `cluster.nodes.count` | 集群节点数 |
| `cluster.connections.local` | 本地连接数 |
| `cluster.connections.remote` | 远程连接数 |
| `cluster.forward.count` | 转发消息数 |
| `cluster.sync.duration` | 同步耗时 |

### 5.3 健康检查

```bash
# 检查节点状态
curl http://localhost:8081/health

# 检查集群成员
curl http://localhost:8081/cluster/members
```

---

## 6. 故障排查

### 6.1 节点无法加入集群

**症状**: 日志显示 `Connection refused` 或 `Timeout`

**排查**:

1. 检查网络连通性：`telnet <base-node> <base-port>`
2. 检查防火墙：确保集群端口开放
3. 检查 `advertise-host`：确保配置正确的外网 IP
4. 检查 `bind-port`：确保端口未被占用

### 6.2 消息未送达

**症状**: 发送消息后订阅者未收到

**排查**:

1. 检查订阅是否成功：`TopicSubscription.getTopicHashesForConnection()`
2. 检查 BloomFilter：`NodeBloomFilter.mightHaveTopic()`
3. 检查转发日志：搜索 `Cluster forward`
4. 检查连接状态：`ClusterConnectionRegistry.findNodeForConnection()`

### 6.3 节点频繁断连

**症状**: 日志频繁出现 `Node disconnected` / `Node reconnected`

**排查**:

1. 检查网络稳定性
2. 检查心跳配置：`heartbeat-interval-seconds` / `heartbeat-timeout-seconds`
3. 检查 GC 停顿：长时间 GC 会导致心跳超时
4. 检查系统负载：CPU/内存/网络带宽

### 6.4 性能调优

**建议**:

1. 禁用 Netty 泄漏检测：`-Dio.netty.leakDetection.level=disabled`
2. 调整 EventLoop 线程数：根据 CPU 核心数
3. 调整 BloomFilter 容量：`expectedInsertions` 根据实际 topic 数量
4. 调整同步间隔：`sync-interval-seconds` 根据业务需求

---

## 附录

### A. 集群命令列表

| 命令 | Code | 说明 |
|------|------|------|
| CLUSTER_JOIN | 0x10 | 节点加入 |
| CLUSTER_MEMBERSHIP | 0x11 | 成员更新 |
| CLUSTER_SYNC | 0x12 | 连接同步 |
| CLUSTER_FORWARD | 0x13 | 消息转发 |
| CLUSTER_BROADCAST | 0x14 | 消息扩散 |
| CLUSTER_HEARTBEAT | 0x15 | 心跳 |

### B. NodeSelector 策略

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| Random | 随机选择 | 负载均衡 |
| RoundRobin | 轮询选择 | 均匀分布 |
| Priority | 按优先级选择 | 主备切换 |
| Single | 固定节点 | 测试/单节点 |
