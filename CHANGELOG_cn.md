# 变更日志

[English](CHANGELOG.md) | [README](README_cn.md) | [集群手册](MANUAL_cn.md)

本文件记录项目的所有重要变更。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

## [1.0.0-SNAPSHOT] - 2025-07-14

### 新增 - 集群 Mesh 支持

#### jwsch-common
- **Command**: 新增集群命令 (0x10-0x15): CLUSTER_JOIN、CLUSTER_MEMBERSHIP、CLUSTER_SYNC、CLUSTER_FORWARD、CLUSTER_BROADCAST、CLUSTER_HEARTBEAT
- **TopicHash**: 新增基于 xxHash64 的主题哈希，用于高性能主题比较

#### jwsch-cli
- **NodeSelector**: 新增节点选择策略接口，包含 4 种实现:
  - `RandomSelector`: 从可用节点随机选择
  - `RoundRobinSelector`: 轮询选择
  - `PrioritySelector`: 按优先级顺序选择
  - `SingleSelector`: 固定单节点选择
- **TcpClientConfig**: 新增集群配置字段: `nodes`、`basePort`、`portRange`、`selectorType`、`reconnectDelaySeconds`
- **TcpClient**: 新增集群连接方法 `connectCluster()`，支持自动重连

#### jwsch-srv
- **ClusterConfig**: 重构配置类，`nodeId` 自动计算，`advertise-host` 从 JVM/环境变量读取，支持 `base-port` 和 `port-range` 端口分配
- **ClusterConnectionRegistry**: 连接注册表，跟踪本地/远程节点连接
- **InMemoryClusterNodeRegistry**: 内存节点注册表，支持心跳追踪
- **NodeBloomFilter**: Guava BloomFilter 主题预过滤器（~3% 误判率）
- **ClusterMessage**: 集群消息基类，包含 6 种消息类型:
  - `ClusterJoin`: 节点加入通知
  - `ClusterMembership`: 节点成员更新
  - `ClusterSync`: 连接/主题同步
  - `ClusterForward`: 转发 REQUEST 到目标节点
  - `ClusterBroadcast`: 扩散 PUSH/BROADCAST 到所有节点
  - `ClusterHeartbeat`: 节点心跳
- **ClusterEncoder/ClusterDecoder**: 集群消息编解码器
- **ClusterServer**: 集群 TCP 服务端，用于节点间通信
- **ClusterClient**: 集群 TCP 客户端，用于连接其他节点
- **ClusterMeshManager**: 集群 Mesh 生命周期管理中心
- **ClusterServerHandler/ClusterClientHandler**: Netty 集群协议处理器
- **ClusterForwarder**: 消息转发逻辑，基于 targetId/topic/broadcast
- **ClusterSyncService**: 定期全量同步 + 事件驱动增量同步
- **ConnectionManager**: 连接生命周期管理接口
- **PacketRouter**: 集成 ClusterForwarder 支持跨节点路由
- **JwschServer**: 组装所有集群组件

#### jwsch-test
- **ClusterIntegrationTest**: 4 个集群集成测试:
  - 2 节点 Mesh 拓扑验证
  - REQUEST 按 targetId 路由
  - PUSH 按 topic 路由
  - BROADCAST 全网广播
- **ClusterConfigTest**: 更新为新 API
- **ClusterServerTest**: 更新为新 API
- **TopicSubscriptionTest**: 更新为 TopicHash

### 技术细节

- **节点 ID 格式**: `{node-prefix}-{advertise-host}-{cluster-port}`
- **启动规则**: 非基端口节点等待 `startup-wait-seconds` 后连接基端口节点
- **同步策略**: 30 秒定期全量同步 + 连接/断开/订阅/取消订阅事件驱动增量同步
- **主题哈希**: xxHash64 快速比较，存储为 `long` 而非 `String`
- **BloomFilter**: 每节点 ~9-91KB（1 万预期插入，3% 误判率）
- **通配符**: 不支持（明确设计决策）

### 构建与测试

```bash
# 构建
mvnd clean package -Dmaven.test.skip=true

# 运行集群测试
mvnd test -pl jwsch-test -Dtest="Cluster*Test"

# 运行所有测试
mvnd test -pl jwsch-test -Dtest=SrvTestSuite
```