# jwsch

基于 Netty 实现的中间件平台，用于前后端消息转发和通信。

[English](README.md) | [变更日志](CHANGELOG_cn.md) | [集群手册](MANUAL_cn.md)

## 特性

- **双协议支持**：WebSocket（前端）+ TCP（后端）
- **集群 Mesh**：服务节点互联，消息跨节点转发
- **Topic 订阅**：基于 xxHash64 的高性能主题订阅
- **零拷贝**：Netty ByteBuf 切片转发，减少内存复制
- **高可用**：节点自动发现、心跳检测、故障转移

## 快速开始

```bash
# 编译
mvnd clean package -Dmaven.test.skip=true

# 启动示例服务（单节点）
java -jar jwschd/target/jwschd-1.0.0-SNAPSHOT.jar

# 启动集群（3节点）
java -Djwsch.advertise.host=192.168.1.10 -jar jwschd/target/jwschd-1.0.0-SNAPSHOT.jar --config cluster-node1.yaml
java -Djwsch.advertise.host=192.168.1.11 -jar jwschd/target/jwschd-1.0.0-SNAPSHOT.jar --config cluster-node2.yaml
java -Djwsch.advertise.host=192.168.1.12 -jar jwschd/target/jwschd-1.0.0-SNAPSHOT.jar --config cluster-node3.yaml
```

## 构建与测试

```bash
# 编译项目
mvnd compile

# 打包（跳过测试）
mvnd package -Dmaven.test.skip=true

# 运行单元测试
mvnd test -pl jwsch-test -Dtest=AllTests
```

## 延迟测试工具

jwsch-bench 模块提供延迟测试工具，用于测量端到端消息延迟。

### 关键 JVM 参数

**必须添加以下参数禁用 Netty 泄漏检测：**

```bash
-Dio.netty.leakDetection.level=disabled
```

Netty 默认开启 `simple` 级别的泄漏检测，每次 ByteBuf 分配都会采样并记录调用栈。高吞吐场景下会严重影响性能（每次分配创建 `Throwable` 对象）。

生产环境和压测环境都应禁用泄漏检测。

### 快速测试（单进程）

启动 1 pub + 1 sub，自动统计延迟：

```bash
java -Dio.netty.leakDetection.level=disabled \
  -Djava.net.preferIPv4Stack=true \
  -cp "jwsch-bench/target/jwsch-bench-1.0.0-SNAPSHOT.jar:jwsch-common/target/jwsch-common-1.0.0-SNAPSHOT.jar:jwsch-cli/target/jwsch-cli-1.0.0-SNAPSHOT.jar:jwsch-srv/target/jwsch-srv-1.0.0-SNAPSHOT.jar" \
  cn.itcraft.jwsch.bench.latency.LatencyTestMain \
  --host localhost --tcpPort 9090 --wsUrl ws://localhost:8080/ws \
  --topic /topic/latency --interval 100 --payloadSize 64 --duration 1
```

### 分离进程测试

**启动订阅者：**

```bash
java -Dio.netty.leakDetection.level=disabled \
  -Djava.net.preferIPv4Stack=true \
  -cp "jwsch-bench/target/jwsch-bench-1.0.0-SNAPSHOT.jar:jwsch-common/target/jwsch-common-1.0.0-SNAPSHOT.jar:jwsch-cli/target/jwsch-cli-1.0.0-SNAPSHOT.jar:jwsch-srv/target/jwsch-srv-1.0.0-SNAPSHOT.jar" \
  cn.itcraft.jwsch.bench.latency.LatencySubscriberMain \
  --wsUrl ws://localhost:8080/ws --topic /topic/latency --duration 1
```

**启动发布者：**

```bash
java -Dio.netty.leakDetection.level=disabled \
  -Djava.net.preferIPv4Stack=true \
  -cp "jwsch-bench/target/jwsch-bench-1.0.0-SNAPSHOT.jar:jwsch-common/target/jwsch-common-1.0.0-SNAPSHOT.jar:jwsch-cli/target/jwsch-cli-1.0.0-SNAPSHOT.jar:jwsch-srv/target/jwsch-srv-1.0.0-SNAPSHOT.jar" \
  cn.itcraft.jwsch.bench.latency.LatencyPublisherMain \
  --host localhost --tcpPort 9090 --topic /topic/latency --interval 100 --payloadSize 64 --duration 1
```

### 参数说明

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `--host` | 服务器地址 | localhost |
| `--tcpPort` | TCP 端口 | 9090 |
| `--wsUrl` | WebSocket URL | ws://localhost:8080/ws |
| `--topic` | 订阅主题 | /topic/latency |
| `--interval` | 发送间隔（微秒） | 100 |
| `--payloadSize` | 负载大小（字节） | 64 |
| `--duration` | 持续时间（分钟），0=无限 | 1 |

### 延迟基准

10K msg/s (100μs 间隔, 64B 负载)：

| 指标 | 值 |
|------|------|
| P50 | ~250μs |
| P90 | ~870μs |
| P99 | ~3.2ms |
| Max | ~12ms |

## 模块结构

```
jwsch/
├── jwsch-common/    # 共享模块：协议、ID生成、缓存、异常
├── jwsch-cli/       # 客户端模块：TCP连接、连接池、节点选择器
├── jwsch-srv/       # 服务端模块：WebSocket服务、路由、集群
├── jwschd/          # 独立部署模块：YAML配置、启动器
├── jwsch-bench/     # 压测模块：Benchmark工具
└── jwsch-sample/    # 示例模块：server/webapp/pusher
```

## 集群 Mesh

jwsch 支持服务节点互联形成 Mesh 拓扑，实现消息跨节点转发。

### 架构

```
┌─────────────┐
│   Publisher │ (TCP Client)
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
│  topic: a   │                       │  topic: b   │
└─────────────┘                       └─────────────┘
```

### 核心特性

| 特性 | 说明 |
|------|------|
| **节点发现** | 基于 base-port 自动发现，非基端口节点自动连接基端口节点 |
| **消息转发** | REQUEST 按 targetId 转发，PUSH 按 topic 路由，BROADCAST 全网扩散 |
| **连接同步** | 定期全量同步 + 事件驱动增量同步 |
| **Topic 过滤** | BloomFilter 预过滤，减少不必要的跨节点消息 |
| **NodeSelector** | 支持 Random、RoundRobin、Priority、Single 四种选择策略 |

### 节点 ID 格式

```
{node-prefix}-{advertise-host}-{cluster-port}
```

示例：`jwsch-192.168.1.10-9090`

### advertise-host 配置

节点对外通信地址，优先级：JVM 参数 > 环境变量 > 自动检测

```bash
# 方式1: JVM 参数（推荐）
java -Djwsch.advertise.host=192.168.1.10 -jar jwschd.jar

# 方式2: 环境变量
export JWSCH_ADVERTISE_HOST=192.168.1.10
java -jar jwschd.jar

# 方式3: 自动检测（取首个非回环地址）
java -jar jwschd.jar
```

详细文档见 [MANUAL_cn.md](MANUAL_cn.md)。

## 开发指南

详见 [AGENTS.md](AGENTS.md)。
