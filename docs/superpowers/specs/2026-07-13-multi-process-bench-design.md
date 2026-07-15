# Multi-Process Benchmark Design

## Background

jwsch-bench currently runs server, publishers, and subscribers in a single JVM. In scenario 3 (1 pub × 50 sub × 50μs × 20KB), 50 subscriber EventLoopGroups consume 50 threads, causing severe resource contention and subscriber death. This is not a real deployment scenario.

## Goal

Support multi-process benchmark where server, publishers, and subscribers each run in independent JVMs on the same machine. Each process has its own heap and thread pool, eliminating resource contention.

## Architecture

```
start-bench-dist.sh
    │
    ├─→ java BenchServerMain --wsPort 8080 --tcpPort 9090 --workers 16
    │       Prints "SERVER_READY" after ports bound
    │
    ├─→ java BenchSubscriberMain --wsUrl ws://localhost:8080/ws --subscribers 50 --topic /topic/bench
    │       Prints "SUBSCRIBER_READY" after all connected and subscribed
    │       Independent TPS output
    │
    └─→ java BenchPublisherMain --host localhost --tcpPort 9090 --publishers 1 --topic /topic/bench --interval 50 --payloadSize 20480
            Independent TPS output
```

## New Files

| File | Description |
|------|-------------|
| `BenchServerMain.java` | Start server, print SERVER_READY after ports bound |
| `BenchPublisherMain.java` | Start N publishers, independent TPS report |
| `BenchSubscriberMain.java` | Start N subscribers, independent TPS report |
| `start-bench-dist.sh` | Shell orchestration script |

## Key Design Decisions

1. **SERVER_READY marker**: Server prints `SERVER_READY` to stdout after both ports bound. Shell script waits for this before starting subscribers.
2. **SUBSCRIBER_READY marker**: Subscriber prints `SUBSCRIBER_READY` after all connections established and SUBSCRIBE packets sent. Shell waits before starting publishers.
3. **Process management**: Shell `trap` catches SIGINT/SIGTERM, kills all child processes via PID array.
4. **Independent JVM params**: Each process configurable `-Xmx`. Defaults: server=1g, subscriber=256m, publisher=256m.
5. **TPS output**: Each process prints independently with role prefix `[SERVER]` `[PUB-0]` `[SUB-AGG]`.
6. **Reuse existing classes**: BenchServer/BenchPublisher/BenchSubscriber/TpsTracker unchanged. New Main classes only assemble calls.
7. **Existing BenchRunner preserved**: No changes to single-process bench.

## CLI Design

Unified entry: `java -jar bench.jar <role> [args]`

### BenchServerMain (role=server)

| Arg | Default | Description |
|-----|---------|-------------|
| `--wsPort` | 8080 | WebSocket port |
| `--tcpPort` | 9090 | TCP port |
| `--workers` | 16 | Server worker threads |

### BenchSubscriberMain (role=subscriber)

| Arg | Default | Description |
|-----|---------|-------------|
| `--wsUrl` | ws://localhost:8080/ws | WebSocket URL |
| `--subscribers` | 5 | Number of subscriber instances |
| `--topic` | /topic/bench | Subscribe topic |
| `--report` | 5 | TPS report interval (seconds) |
| `--duration` | 5 | Duration (minutes, 0=unlimited) |

### BenchPublisherMain (role=publisher)

| Arg | Default | Description |
|-----|---------|-------------|
| `--host` | localhost | Server host |
| `--tcpPort` | 9090 | Server TCP port |
| `--publishers` | 1 | Number of publisher instances |
| `--topic` | /topic/bench | Publish topic |
| `--interval` | 10 | Send interval (microseconds) |
| `--payloadSize` | 2 | Payload size (bytes) |
| `--report` | 5 | TPS report interval (seconds) |
| `--duration` | 5 | Duration (minutes, 0=unlimited) |

## Shell Script Flow

```bash
1. Start server process, pipe stdout, wait for "SERVER_READY"
2. Start subscriber process in background
3. Wait for "SUBSCRIBER_READY" from subscriber stdout
4. Start publisher process in background
5. trap SIGINT/SIGTERM → kill all child processes
6. wait for all processes
```

## Environment Variables (Shell Script)

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_JVM` | -Xmx1g | Server JVM options |
| `SUB_JVM` | -Xmx256m | Subscriber JVM options |
| `PUB_JVM` | -Xmx256m | Publisher JVM options |
| `WS_PORT` | 8080 | WebSocket port |
| `TCP_PORT` | 9090 | TCP port |
| `WORKERS` | 16 | Server worker threads |
| `PUBLISHERS` | 1 | Publisher count |
| `SUBSCRIBERS` | 5 | Subscriber count |
| `TOPIC` | /topic/bench | Topic |
| `INTERVAL` | 10 | Send interval (μs) |
| `PAYLOAD_SIZE` | 2 | Payload size (bytes) |
| `DURATION` | 5 | Duration (minutes) |
| `REPORT` | 5 | Report interval (seconds) |
