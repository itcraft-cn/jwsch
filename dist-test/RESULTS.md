# jwsch 三机分布式压测结果

- **日期**：2026-10-02
- **结果目录**：`dist-test/results/20261002-163018/`（发布端 / 订阅端 / 服务端原始日志）

## 1. 拓扑与环境

| 角色 | 主机 | 地址 | 软件 |
|------|------|------|------|
| 发布端 | vboxdeb001 | 172.22.133.251 | Dragonwell JDK 8.29.28 (1.8.0_492)，`LatencyPublisherMain --clock wall`（TCP PUSH） |
| 服务端 | vboxdeb002 | 172.22.133.252 | 同 JDK，`jwsch-bench server`（WebSocket 8080 + TCP 9090，8 worker） |
| 订阅端 | vboxdeb003 | 172.22.133.253 | Node.js 20.19.2 + `ws` 8.19.0，`jwsch-js` CJS 客户端（WebSocket 订阅） |

- 每台虚拟机：Debian 13 (trixie)，2 vCPU，8 GB 内存，VirtualBox host-only 网络（`172.22.133.0/24`，RTT < 1 ms）。
- 服务端流控：**关闭入站限流**（`--inboundRateLimit 0`），出站队列上限 65536 / 断连阈值 262144；主题级背压保持默认开启（trigger=0.3）。
- 消息体：`| SendTimeMillis(8B) | Sequence(8B) | Payload(NB) |`。

### 时钟同步（延时测量前提）

跨机单向延时要求两端时钟一致。初始时三台主机各自同步到不同公网 NTP，偏差达 8–16 ms，导致延时不可信。改造为：

- **以 vboxdeb002 作为局域网 NTP 参考**（chrony server，`allow 172.22.133.0/24`）；
- vboxdeb001 / vboxdeb003 作为客户端指向 102，`minpoll 2 maxpoll 4`（4–16 s 轮询），测试前强制 `chronyc makestep`。

校时后两端相对偏差约 **20–40 µs**，本轮全部场景 `lat_negative = 0`（无负延时样本）。

## 2. 结果

测量时长 30 s，预热 2 s，单 Node 订阅连接。吞吐取「活跃窗口」（首末消息到达区间）以排除发布端启动空档。

| 场景 | 发送间隔 | 负载 | 接收总数 | 活跃吞吐 (msg/s) | 时长吞吐 (msg/s) | avg | p50 | p90 | p95 | p99 | max |
|------|---------|------|---------|-----------------|-----------------|-----|-----|-----|-----|-----|-----|
| low  | 1000 µs | 64 B | 28,463    | 1,001   | 949    | 1.34 ms | 1 ms | 2 ms | 6 ms | 18 ms | 30 ms |
| mid  | 100 µs  | 64 B | 283,949   | 10,004  | 9,465  | 0.74 ms | 1 ms | 1 ms | 2 ms | 6 ms  | 29 ms |
| h50  | 20 µs   | 64 B | 1,383,006 | 48,924  | 45,989 | 6.24 ms | 1 ms | 18 ms | 31 ms | 56 ms | 90 ms |
| h100 | 10 µs   | 64 B | 2,721,301 | 96,079  | 90,703 | 30.11 ms | 13 ms | 98 ms | 125 ms | 150 ms | 167 ms |
| h200 | 5 µs    | 64 B | 256,961   | 25,377  | 8,564  | 601.47 ms | 98 ms | 1424 ms | 5331 ms | 7312 ms | 8308 ms |
| over | 1 µs    | 100 B | 185,149  | 14,875  | 6,171  | 1080.0 ms | 72 ms | 4714 ms | 8009 ms | 11007 ms | 11015 ms |

> h200 / over 为**过载**场景：投递在约 10 s 后停滞（队列积压 + 系统饱和），表中的低「时长吞吐」是失稳产物，不代表容量。

## 3. 结论

1. **低延迟区间（≤ 10K msg/s）**：延时稳定在亚毫秒~1 ms 量级，p99 6–18 ms。1000 µs 与 100 µs 档基本呈线性。
2. **可持续吞吐拐点 ≈ 96K msg/s**（64 B，单 Node 订阅连接，对应 100K msg/s 投放）：投递 96.1K msg/s，p50 13 ms、p99 150 ms，服务端与订阅端进入轻度排队。
3. **过载坍塌**：投放 200K+ msg/s 时，约 10 s 后系统饱和、端到端延时升至数秒并最终停止投递。说明单订阅连接的稳定上限在 100K msg/s 附近。
4. **低延迟 vs 高吞吐的取舍**：约 50K msg/s 时 p99 仍可控制在 ~56 ms；100K msg/s 时 p99 升至 ~150 ms。
5. **默认配置的重要约束**：未放开流控时，服务端入站限流恒为 **10000 tokens/s、burst 12000**，表现为「突发 12000 条后丢包并断开 TCP 生产连接」。压测/高吞吐部署需显式放开（`--inboundRateLimit`）。
6. 与单机基准（README 单 WebSocket 连接出口上限约 195–200K msg/s）相比，本测试的瓶颈更可能来自 Node 订阅端单进程（单线程解码）与 2 vCPU 服务端，而非协议本身。

## 4. 说明与局限

- 延时来自墙钟相减，时间戳分辨率为 **1 ms**（`currentTimeMillis` / `Date.now`），百分位存在 1 ms 量化；同步精度约几十 µs。
- 103 仅运行**单个 Node 进程**，其事件循环单线程处理解码；横向扩展需增加进程/节点。
- 结果受 VirtualBox、2 vCPU、JVM/Node 默认堆与 GC 影响，绝对值仅供横向对比参考。

## 5. 复现

```bash
# 前置：三机已装 JDK8 与 Node，101/102 有 ~/jwsch/bench.jar，103 有 ~/jwsch/jwsch-js
./dist-test/run-distributed-test.sh          # 全部场景
./dist-test/run-distributed-test.sh low h100 # 指定场景
```

可用环境变量覆盖：`PUB_HOST/SRV_HOST/SUB_HOST/SRV_IP/WORKERS/INBOUND_RATE_LIMIT/OUTBOUND_QUEUE/OUTBOUND_DISCONNECT`。
