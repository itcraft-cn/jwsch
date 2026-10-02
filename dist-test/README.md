# 三机分布式压测 (dist-test)

验证 jwsch 在「Java 发布端 → Java 服务端 → Node.js 订阅端」跨语言链路下的吞吐与端到端延时。

## 拓扑

```
 vboxdeb001 (172.22.133.251)        vboxdeb002 (172.22.133.252)        vboxdeb003 (172.22.133.253)
 ┌───────────────────────┐          ┌───────────────────────┐          ┌────────────────────────┐
 │ LatencyPublisherMain  │  TCP     │  jwsch-bench server   │  WS      │  bench-subscriber.js   │
 │  (PUSH, --clock wall) ├─────────►│  (WebSocket + TCP)    ├─────────►│  (Node.js 订阅/统计)   │
 └───────────────────────┘  9090    └───────────────────────┘  8080    └────────────────────────┘
        发布端                              服务端                              订阅端
```

## 度量口径

- **吞吐**：Node 订阅端接收到的合法 PUSH/BROADCAST 消息数 ÷ 墙钟测量时长，单位 msg/s。
- **延时**：单向端到端延时 = `Date.now()@收到` − `sendTimeMillis@发送`。
  - 发送端在消息体前 8 字节写入 `System.currentTimeMillis()`（大端），依赖三机 NTP 时钟同步
    （Debian 默认 `systemd-timesyncd`）。
  - 消息体格式：`| SendTimeMillis(8B) | Sequence(8B) | Payload(NB) |`
  - 结果包含固定时钟偏差，`lat_min_ms` 可作为偏差下界参考；`lat_negative` 为负延时样本数
    （时钟未同步或抖动导致），正常应接近 0。
- 统计前 2s 为预热（`--warmup 2`），不计入延时样本。

## 前置条件

1. 三台主机可免密 ssh（用户名 `vboxuser`）。
2. 101/102 安装 JDK8（`~/lang/dragonwell-8.29.28`），103 安装 Node.js。
3. 制品已上传：
   - 101/102：`~/jwsch/bench.jar`
   - 103：`~/jwsch/jwsch-js`（含 `lib/`、`bench/`、`node_modules/ws`）

## 使用

```bash
# 执行全部场景（low/mid/high/max）
./run-distributed-test.sh

# 仅执行指定场景
./run-distributed-test.sh low high
```

可通过环境变量覆盖主机与端口：`PUB_HOST` / `SRV_HOST` / `SUB_HOST` / `SRV_IP` /
`WS_PORT` / `TCP_PORT` / `WORKERS` / `TOPIC`。

结果写入 `results/<时间戳>/`，含发布端、订阅端、服务端日志。

## 场景

| 名称 | 发送间隔 | 负载 | 时长 | 订阅连接 |
|------|---------|------|------|---------|
| low  | 1000 μs | 64 B | 30 s | 1 |
| mid  | 100 μs  | 64 B | 30 s | 1 |
| h50  | 20 μs   | 64 B | 30 s | 1 |
| h100 | 10 μs   | 64 B | 30 s | 1 |
| h200 | 5 μs    | 64 B | 30 s | 1 |
| over | 1 μs    | 100 B| 30 s | 1 |

## 时钟同步

跨机单向延时依赖两端时钟一致，需先建立局域网 NTP 参考（默认以服务端 102 为参考，
客户端 chrony `minpoll 2 maxpoll 4`）。测试脚本启动前会执行一次 `chronyc makestep`
消除累积漂移；校时后相对偏差可达 ~20-40 µs。

## 流控

服务端默认入站限流为 10000 tokens/s、burst 12000，会成为吞吐硬上限。脚本默认以
`--inboundRateLimit 0` 关闭入站限流，并放大出站队列；可用环境变量
`INBOUND_RATE_LIMIT` / `OUTBOUND_QUEUE` / `OUTBOUND_DISCONNECT` 覆盖。

## 实测结果

见 [`RESULTS.md`](RESULTS.md)。

