# Changelog

[中文文档](CHANGELOG.md) | [README](README_en.md) | [Manual](MANUAL_en.md)

All notable changes to this project are documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.0.0-SNAPSHOT] - 2026-10-02

### Added - Distributed Benchmark

- **jwsch-bench**: New distributed Java -> server -> Node throughput/latency test
- **dist-test**: Raw result logs of the 2026-10-02 distributed benchmark run
- **docs(dist)**: Assessment of cross-protocol (TCP -> WebSocket) benchmark metrics

## [1.0.0-SNAPSHOT] - 2026-09-16

### Added - Packet Size Limits

- **Client & Server**: Enforce packet total length (Header + Body) on both sides; oversize packets are dropped without closing the connection
  - Soft limit: default 200KB, configurable via `tcp.max-packet-length` / `websocket.max-packet-length`
  - Hard limit: 500KB (hardcoded); configurations above it are force-clamped
- **Oversize logging**: `log-oversize-content` dumps the first 200 bytes of dropped packets from a dedicated daemon thread that starts lazily (off by default, no thread at all when off)

### Added - Benchmark Results

- **README**: Throughput ladder benchmark (10B-100KB payloads, Subscribe/Broadcast/Point-to-point) recorded in README

### Fixed

- **Review**: Address H2-H4, all M and L1/L4/L5/L6 findings (report: `docs/review/code-review-20260916-001-java.md`)
- **Tests**: Repair stale JUnit suites to match production behavior

### Documentation

- **MANUAL**: Document packet size soft/hard limits (section 5.2)

## [1.0.0-SNAPSHOT] - 2026-09-07

### Fixed - Protocol Resource Management

- **WebSocketHandler**: Release `Packet` after handling to prevent reference leaks
- **TcpClient**: Make reconnect asynchronous so callers are not blocked
- **Build**: Restore method bodies destroyed by a doc commit and fix duplicate package declarations

## [1.0.0-SNAPSHOT] - 2026-08-05

### Documentation - Comprehensive JavaDoc

- Add English JavaDoc comments across all modules: `jwsch-common`, `jwsch-cli`, `jwsch-srv`, `jwschd`, `jwsch-bench`, `jwsch-sample`

## [1.0.0-SNAPSHOT] - 2026-07-15

### Added - Bilingual Documentation

- **Docs**: Bilingual (en/zh) README, CHANGELOG, MANUAL
- **License**: Add LICENSE file

## [1.0.0-SNAPSHOT] - 2025-07-14

### Added - Cluster Mesh Support

#### jwsch-common
- **Command**: New cluster commands (0x10-0x15): CLUSTER_JOIN, CLUSTER_MEMBERSHIP, CLUSTER_SYNC, CLUSTER_FORWARD, CLUSTER_BROADCAST, CLUSTER_HEARTBEAT
- **TopicHash**: New xxHash64-based topic hash for high-performance topic comparison

#### jwsch-cli
- **NodeSelector**: New node selection strategy interface with 4 implementations:
  - `RandomSelector`: Random selection from available nodes
  - `RoundRobinSelector`: Round-robin selection
  - `PrioritySelector`: Priority-based selection
  - `SingleSelector`: Fixed single-node selection
- **TcpClientConfig**: New cluster config fields: `nodes`, `basePort`, `portRange`, `selectorType`, `reconnectDelaySeconds`
- **TcpClient**: New cluster connection method `connectCluster()` with auto-reconnect

#### jwsch-srv
- **ClusterConfig**: Refactored config class, `nodeId` auto-calculation, `advertise-host` read from JVM/env, supports `base-port` and `port-range` port allocation
- **ClusterConnectionRegistry**: Connection registry tracking local/remote node connections
- **InMemoryClusterNodeRegistry**: In-memory node registry with heartbeat tracking
- **NodeBloomFilter**: Guava BloomFilter topic pre-filter (~3% false positive rate)
- **ClusterMessage**: Cluster message base class with 6 message types:
  - `ClusterJoin`: Node join notification
  - `ClusterMembership`: Node membership update
  - `ClusterSync`: Connection/topic sync
  - `ClusterForward`: Forward REQUEST to target node
  - `ClusterBroadcast`: Spread PUSH/BROADCAST to all nodes
  - `ClusterHeartbeat`: Node heartbeat
- **ClusterEncoder/ClusterDecoder**: Cluster message codec
- **ClusterServer**: Cluster TCP server for inter-node communication
- **ClusterClient**: Cluster TCP client for connecting to other nodes
- **ClusterMeshManager**: Cluster Mesh lifecycle management center
- **ClusterServerHandler/ClusterClientHandler**: Netty cluster protocol handlers
- **ClusterForwarder**: Message forwarding logic based on targetId/topic/broadcast
- **ClusterSyncService**: Periodic full sync + event-driven incremental sync
- **ConnectionManager**: Connection lifecycle management interface
- **PacketRouter**: Integrated ClusterForwarder for cross-node routing
- **JwschServer**: Assembles all cluster components

#### jwsch-test
- **ClusterIntegrationTest**: 4 cluster integration tests:
  - 2-node Mesh topology verification
  - REQUEST routing by targetId
  - PUSH routing by topic
  - BROADCAST to all nodes
- **ClusterConfigTest**: Updated for new API
- **ClusterServerTest**: Updated for new API
- **TopicSubscriptionTest**: Updated for TopicHash

### Technical Details

- **Node ID Format**: `{node-prefix}-{advertise-host}-{cluster-port}`
- **Startup Rule**: Non-base-port nodes wait `startup-wait-seconds` before connecting to base-port node
- **Sync Strategy**: 30-second periodic full sync + connection/disconnect/subscribe/unsubscribe event-driven incremental sync
- **Topic Hash**: xxHash64 for fast comparison, stored as `long` instead of `String`
- **BloomFilter**: ~9-91KB per node (10K expected insertions, 3% false positive rate)
- **Wildcards**: Not supported (explicit design decision)

### Build & Test

```bash
# Build
mvnd clean package -Dmaven.test.skip=true

# Run cluster tests
mvnd test -pl jwsch-test -Dtest="Cluster*Test"

# Run all tests
mvnd test -pl jwsch-test -Dtest=SrvTestSuite
```