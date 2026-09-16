# jwsch

A Netty-based middleware platform for frontend-backend message forwarding and communication.

[中文文档](README_cn.md) | [Changelog](CHANGELOG.md) | [Manual](MANUAL.md)

## Features

- **Dual Protocol Support**: WebSocket (frontend) + TCP (backend)
- **Cluster Mesh**: Service node interconnection, cross-node message forwarding
- **Topic Subscription**: High-performance topic subscription based on xxHash64
- **Zero Copy**: Netty ByteBuf slice forwarding reduces memory copying
- **High Availability**: Automatic node discovery, heartbeat detection, failover

## Security Notice

**Recommended for internal network use only.** Authentication/authorization is not enforced on the WebSocket and TCP endpoints yet (the security module is reserved but not wired): any client that can reach the port can subscribe to arbitrary topics or push messages. Deploy behind a trusted network boundary or your own proxy; external exposure is not supported.

## Packet Size Limits

Both client and server enforce packet total length (Header + Body); oversize packets are dropped without closing the connection:

- **Soft limit**: default 200KB, configurable (`tcp.max-packet-length` / `websocket.max-packet-length`)
- **Hard limit**: 500KB (hardcoded); any configuration above it is force-clamped to 500KB
- Drops log WARN (length / limit / packet hash); optional `log-oversize-content` dumps the first 200 bytes from a dedicated daemon thread that starts lazily (off by default, no thread at all when off)

See MANUAL.md "5.2 Packet Size Limits".

## Quick Start

```bash
# Build
mvnd clean package -Dmaven.test.skip=true

# Start single-node example service
java -jar jwschd/target/jwschd-1.0.0-SNAPSHOT.jar

# Start 3-node cluster
java -Djwsch.advertise.host=192.168.1.10 -jar jwschd/target/jwschd-1.0.0-SNAPSHOT.jar --config cluster-node1.yaml
java -Djwsch.advertise.host=192.168.1.11 -jar jwschd/target/jwschd-1.0.0-SNAPSHOT.jar --config cluster-node2.yaml
java -Djwsch.advertise.host=192.168.1.12 -jar jwschd/target/jwschd-1.0.0-SNAPSHOT.jar --config cluster-node3.yaml
```

## Build & Test

```bash
# Compile
mvnd compile

# Package (skip tests)
mvnd package -Dmaven.test.skip=true

# Run unit tests
mvnd test -pl jwsch-test -Dtest=AllTests
```

## Latency Testing Tool

The jwsch-bench module provides latency testing tools for measuring end-to-end message latency.

### Critical JVM Arguments

**Must disable Netty leak detection:**

```bash
-Dio.netty.leakDetection.level=disabled
```

Netty enables `simple` leak detection by default, which samples and records call stacks on every ByteBuf allocation. In high-throughput scenarios, this severely impacts performance (creates a `Throwable` object per allocation).

Both production and benchmark environments should disable leak detection.

### Quick Test (Single Process)

Start 1 pub + 1 sub, auto-collect latency stats:

```bash
java -Dio.netty.leakDetection.level=disabled \
  -Djava.net.preferIPv4Stack=true \
  -cp "jwsch-bench/target/jwsch-bench-1.0.0-SNAPSHOT.jar:jwsch-common/target/jwsch-common-1.0.0-SNAPSHOT.jar:jwsch-cli/target/jwsch-cli-1.0.0-SNAPSHOT.jar:jwsch-srv/target/jwsch-srv-1.0.0-SNAPSHOT.jar" \
  cn.itcraft.jwsch.bench.latency.LatencyTestMain \
  --host localhost --tcpPort 9090 --wsUrl ws://localhost:8080/ws \
  --topic /topic/latency --interval 100 --payloadSize 64 --duration 1
```

### Separate Process Test

**Start subscriber:**

```bash
java -Dio.netty.leakDetection.level=disabled \
  -Djava.net.preferIPv4Stack=true \
  -cp "jwsch-bench/target/jwsch-bench-1.0.0-SNAPSHOT.jar:jwsch-common/target/jwsch-common-1.0.0-SNAPSHOT.jar:jwsch-cli/target/jwsch-cli-1.0.0-SNAPSHOT.jar:jwsch-srv/target/jwsch-srv-1.0.0-SNAPSHOT.jar" \
  cn.itcraft.jwsch.bench.latency.LatencySubscriberMain \
  --wsUrl ws://localhost:8080/ws --topic /topic/latency --duration 1
```

**Start publisher:**

```bash
java -Dio.netty.leakDetection.level=disabled \
  -Djava.net.preferIPv4Stack=true \
  -cp "jwsch-bench/target/jwsch-bench-1.0.0-SNAPSHOT.jar:jwsch-common/target/jwsch-common-1.0.0-SNAPSHOT.jar:jwsch-cli/target/jwsch-cli-1.0.0-SNAPSHOT.jar:jwsch-srv/target/jwsch-srv-1.0.0-SNAPSHOT.jar" \
  cn.itcraft.jwsch.bench.latency.LatencyPublisherMain \
  --host localhost --tcpPort 9090 --topic /topic/latency --interval 100 --payloadSize 64 --duration 1
```

### Parameter Reference

| Parameter | Description | Default |
|-----------|-------------|---------|
| `--host` | Server address | localhost |
| `--tcpPort` | TCP port | 9090 |
| `--wsUrl` | WebSocket URL | ws://localhost:8080/ws |
| `--topic` | Subscription topic | /topic/latency |
| `--interval` | Send interval (microseconds) | 100 |
| `--payloadSize` | Payload size (bytes) | 64 |
| `--duration` | Duration (minutes), 0=infinite | 1 |

### Latency Baseline

At 10K msg/s (100μs interval, 64B payload):

| Metric | Value |
|--------|-------|
| P50 | ~250μs |
| P90 | ~870μs |
| P99 | ~3.2ms |
| Max | ~12ms |

## Throughput Ladder (2026-09-16, single node)

Server 16 workers (epoll), rate limit raised, topic backpressure on; 1 tight-loop TCP publisher; 2 WebSocket receivers; payload = 8B seq + N bytes. 100KB is already a maximum practical packet; 1MB exceeds the WS frame cap (512KB) and is out of range.

Throughput (msg/s), delivery in parentheses:

| Msg size | Subscribe (PUSH) | Broadcast (BROADCAST) | Point-to-point (REQUEST) |
|---|---|---|---|
| 10 B | 103K (63K) | 360K (233K) | 316K (316K) |
| 100 B | 306K (65K) | 341K (323K) | 381K (381K) |
| 1 KB | 100K (153K) | 283K (65K) | 293K (156K) |
| 10 KB | 53K (106K, lossless) | 41K (82K, lossless) | 44K (44K) |
| 50 KB | 11.6K (23K, lossless) | 9.6K (19K, lossless) | 11.1K, ~1.14 GB/s |
| 100 KB | 5.7K (11.5K, lossless) | 5.6K (11.2K, lossless) | 5.3K |

Fanout bandwidth converges at ~1.1 GB/s for 50-100KB packets. Small-packet (<1KB) sub-100% delivery is backpressure protection, not a leak. Single WebSocket connection discharge ceiling is ~195-200K msg/s regardless of command type: scale by adding nodes, not per-connection.

## Module Structure

```
jwsch/
├── jwsch-common/    # Shared: protocol, ID generation, cache, exceptions
├── jwsch-cli/       # Client: TCP connection, connection pool, node selector
├── jwsch-srv/       # Server: WebSocket service, routing, cluster
├── jwschd/          # Deployment: YAML config, launcher
├── jwsch-bench/     # Benchmark: performance testing tools
└── jwsch-sample/    # Example: server/webapp/pusher
```

## Cluster Mesh

jwsch supports service node interconnection forming a Mesh topology, enabling cross-node message forwarding.

### Architecture

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

### Core Features

| Feature | Description |
|---------|-------------|
| **Node Discovery** | Auto-discovery via base-port, non-base nodes auto-connect to base node |
| **Message Routing** | REQUEST forwarded by targetId, PUSH routed by topic, BROADCAST to all nodes |
| **Connection Sync** | Periodic full sync + event-driven incremental sync |
| **Topic Filtering** | BloomFilter pre-filtering reduces unnecessary cross-node messages |
| **NodeSelector** | Random, RoundRobin, Priority, Single selection strategies |

### Node ID Format

```
{node-prefix}-{advertise-host}-{cluster-port}
```

Example: `jwsch-192.168.1.10-9090`

### advertise-host Configuration

Node's external communication address, priority: JVM arg > Env var > Auto-detect

```bash
# Method 1: JVM argument (recommended)
java -Djwsch.advertise.host=192.168.1.10 -jar jwschd.jar

# Method 2: Environment variable
export JWSCH_ADVERTISE_HOST=192.168.1.10
java -jar jwschd.jar

# Method 3: Auto-detect (first non-loopback address)
java -jar jwschd.jar
```

Full documentation at [MANUAL.md](MANUAL.md).

## Development Guide

See [AGENTS.md](AGENTS.md).