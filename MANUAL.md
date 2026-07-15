# jwsch Cluster Manual

[中文文档](MANUAL_cn.md) | [README](README.md) | [Changelog](CHANGELOG.md)

## Table of Contents

1. [Cluster Architecture](#1-cluster-architecture)
2. [Configuration Guide](#2-configuration-guide)
3. [Node Deployment](#3-node-deployment)
4. [Message Routing](#4-message-routing)
5. [Operations & Monitoring](#5-operations--monitoring)
6. [Troubleshooting](#6-troubleshooting)

---

## 1. Cluster Architecture

### 1.1 Topology

jwsch cluster uses a Mesh topology where all nodes are interconnected:

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

### 1.2 Node Roles

| Role | Description |
|------|-------------|
| **Base Node** | Node where `cluster-port == base-port`; new nodes connect here first |
| **Non-Base Node** | Node where `cluster-port != base-port`; auto-connects to Base Node after startup |

### 1.3 Port Allocation

```
base-port = 9090
port-range = 3

→ Available ports: 9090, 9091, 9092
→ Node-A: bind-port=9090 (Base)
→ Node-B: bind-port=9091 (Non-Base)
→ Node-C: bind-port=9092 (Non-Base)
```

---

## 2. Configuration Guide

### 2.1 YAML Configuration

**Single-node config (node1.yaml)**:

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

**Node B config (node2.yaml)**:

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

### 2.2 Configuration Reference

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | boolean | false | Enable clustering |
| `node-prefix` | String | jwsch | Node prefix for nodeId generation |
| `base-port` | int | 9090 | Cluster base port |
| `port-range` | int | 3 | Port range |
| `bind-port` | int | -1 | Bind port, -1 means use base-port |
| `startup-wait-seconds` | int | 5 | Non-base node startup wait time |
| `sync-interval-seconds` | int | 30 | Full sync interval |
| `heartbeat-interval-seconds` | int | 10 | Heartbeat send interval |
| `heartbeat-timeout-seconds` | int | 30 | Heartbeat timeout |
| `nodes` | List | [] | Cluster node list |

### 2.3 advertise-host Configuration

Node's external communication address, three configuration methods:

**Method 1: JVM argument (recommended)**

```bash
java -Djwsch.advertise.host=192.168.1.10 -jar jwschd.jar
```

**Method 2: Environment variable**

```bash
export JWSCH_ADVERTISE_HOST=192.168.1.10
java -jar jwschd.jar
```

**Method 3: Auto-detect**

```bash
java -jar jwschd.jar
# Automatically picks the first non-loopback network interface address
```

**Priority**: JVM arg > Environment variable > Auto-detect

---

## 3. Node Deployment

### 3.1 Startup Order

```
1. Start Node-A (Base Node, bind-port=9090)
2. Wait 5 seconds
3. Start Node-B (bind-port=9091) → auto-connects to Node-A
4. Start Node-C (bind-port=9092) → auto-connects to Node-A
```

### 3.2 Startup Script Example

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

### 3.3 Docker Deployment

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

## 4. Message Routing

### 4.1 REQUEST (by targetId)

```
Publisher → Server-A → lookup targetId's node
                       ↓
             If local → send directly
             If remote → CLUSTER_FORWARD to target node
```

### 4.2 PUSH (by topic)

```
Publisher → Server-A → lookup connections subscribed to topic
                       ↓
             Local subscribers → send directly
             Remote subscribers → CLUSTER_BROADCAST
                       ↓
             Server-B receives → check local subscriptions → send to subscribers
```

### 4.3 BROADCAST (all nodes)

```
Publisher → Server-A → CLUSTER_BROADCAST to all nodes
                       ↓
             All nodes receive → send to all local connections
```

### 4.4 BloomFilter Optimization

Each node maintains a BloomFilter recording topic hashes subscribed by local connections:

```java
// Before sending PUSH
if (!remoteNode.getBloomFilter().mightHaveTopic(topicHash)) {
    // This node definitely has no subscription for this topic, skip
    return;
}
// Possibly subscribed (3% false positive rate), send CLUSTER_BROADCAST
```

---

## 5. Operations & Monitoring

### 5.1 Log Keywords

| Keyword | Description |
|---------|-------------|
| `Cluster started` | Node startup complete |
| `Node joined` | New node joined |
| `Node left` | Node left |
| `Cluster sync` | Sync event |
| `Cluster forward` | Message forwarding |

### 5.2 Key Metrics

| Metric | Description |
|--------|-------------|
| `cluster.nodes.count` | Number of cluster nodes |
| `cluster.connections.local` | Local connection count |
| `cluster.connections.remote` | Remote connection count |
| `cluster.forward.count` | Forwarded message count |
| `cluster.sync.duration` | Sync duration |

### 5.3 Health Check

```bash
# Check node status
curl http://localhost:8081/health

# Check cluster members
curl http://localhost:8081/cluster/members
```

---

## 6. Troubleshooting

### 6.1 Node Cannot Join Cluster

**Symptom**: Log shows `Connection refused` or `Timeout`

**Checklist**:

1. Check network connectivity: `telnet <base-node> <base-port>`
2. Check firewall: ensure cluster ports are open
3. Check `advertise-host`: ensure correct external IP
4. Check `bind-port`: ensure port is not already in use

### 6.2 Messages Not Delivered

**Symptom**: Subscriber does not receive sent messages

**Checklist**:

1. Check subscription status: `TopicSubscription.getTopicHashesForConnection()`
2. Check BloomFilter: `NodeBloomFilter.mightHaveTopic()`
3. Check forwarding logs: search for `Cluster forward`
4. Check connection status: `ClusterConnectionRegistry.findNodeForConnection()`

### 6.3 Node Frequently Disconnects

**Symptom**: Log shows frequent `Node disconnected` / `Node reconnected`

**Checklist**:

1. Check network stability
2. Check heartbeat config: `heartbeat-interval-seconds` / `heartbeat-timeout-seconds`
3. Check GC pauses: long GC pauses can cause heartbeat timeout
4. Check system load: CPU/memory/network bandwidth

### 6.4 Performance Tuning

**Recommendations**:

1. Disable Netty leak detection: `-Dio.netty.leakDetection.level=disabled`
2. Adjust EventLoop thread count: based on CPU cores
3. Adjust BloomFilter capacity: `expectedInsertions` based on actual topic count
4. Adjust sync interval: `sync-interval-seconds` based on business requirements

---

## Appendix

### A. Cluster Command Reference

| Command | Code | Description |
|---------|------|-------------|
| CLUSTER_JOIN | 0x10 | Node join |
| CLUSTER_MEMBERSHIP | 0x11 | Membership update |
| CLUSTER_SYNC | 0x12 | Connection sync |
| CLUSTER_FORWARD | 0x13 | Message forwarding |
| CLUSTER_BROADCAST | 0x14 | Message broadcast |
| CLUSTER_HEARTBEAT | 0x15 | Heartbeat |

### B. NodeSelector Strategies

| Strategy | Description | Use Case |
|----------|-------------|----------|
| Random | Random selection | Load balancing |
| RoundRobin | Round-robin selection | Even distribution |
| Priority | Priority-based selection | Primary/backup failover |
| Single | Fixed node | Testing / single node |
