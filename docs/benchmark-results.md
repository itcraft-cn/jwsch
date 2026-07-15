# Jwsch Benchmark Results

## Test Environment

- **Hardware**: Single machine, loopback network
- **JVM**: Java 8, Server 1GB heap + 1GB direct, Subscriber 1GB heap + 1GB direct, Publisher 512MB heap + 256MB direct
- **Topology**: 1 Publisher → Server → 50 Subscribers
- **Duration**: 1 minute per test

## Benchmark 1: 20KB Payload

| Interval | PUB TPS (理论) | PUB TPS (实际) | SUB TPS (理论) | SUB TPS (实际) | Fan-out | Output | Status |
|----------|----------------|----------------|----------------|----------------|---------|--------|--------|
| 100ms | 10 | 10 | 500 | 500 | 50× | 10 MB/s | ✓ 稳定 |
| 10ms | 100 | 100 | 5,000 | 5,000 | 50× | 100 MB/s | ✓ 稳定 |
| 1ms | 1,000 | 1,000 | 50,000 | 50,000 | 50× | 1 GB/s | ✓ 稳定 |
| 500μs | 2,000 | 2,000 | 100,000 | 100,000 | 50× | 2 GB/s | ✓ 稳定 |
| 250μs | 4,000 | 4,000 | 200,000 | ~200K→55K | 50×→14× | 4 GB/s | ⚠ OOM @ 50s |

### 优化后 (writeToBytes + wrappedBuffer)

| Interval | PUB TPS (理论) | PUB TPS (实际) | SUB TPS (理论) | SUB TPS (实际) | Fan-out | Output | Status |
|----------|----------------|----------------|----------------|----------------|---------|--------|--------|
| 250μs | 4,000 | ~4,000 | 200,000 | 200,000 | 50× | 4 GB/s | ✓ 稳定 |

**结论**:
- 稳定极限: 500μs = 2 GB/s fan-out
- 峰值极限: 250μs = 4 GB/s fan-out (短时)
- 瓶颈: Direct Buffer 容量
- **优化效果**: 250μs 从 OOM 提升为稳定运行，完美 50× 扇出

## Benchmark 2: 1KB Payload

| Interval | PUB TPS (理论) | PUB TPS (实际) | SUB TPS (理论) | SUB TPS (实际) | Fan-out | Output | Status |
|----------|----------------|----------------|----------------|----------------|---------|--------|--------|
| 100ms | 10 | 10 | 500 | 500 | 50× | 0.5 MB/s | ✓ 稳定 |
| 10ms | 100 | 100 | 5,000 | 5,000 | 50× | 5 MB/s | ✓ 稳定 |
| 1ms | 1,000 | 1,000 | 50,000 | 50,000 | 50× | 50 MB/s | ✓ 稳定 |
| 500μs | 2,000 | 2,000 | 100,000 | 100,000 | 50× | 100 MB/s | ✓ 稳定 |
| 250μs | 4,000 | 4,000 | 200,000 | 200,000 | 50× | 200 MB/s | ✓ 稳定 |
| 100μs | 10,000 | 10,000 | 500,000 | 500,000 | 50× | 500 MB/s | ✓ 稳定 |
| 50μs | 20,000 | 20,000 | 1,000,000 | 700,000 | 35× | 700 MB/s | ✓ 稳定 (fan-out 下降) |
| 25μs | 40,000 | 40,000 | 2,000,000 | 120,000 | 3× | 120 MB/s | ✗ Publisher OOM |
| 10μs | 100,000 | - | 5,000,000 | 29K→0 | 0× | - | ✗ Publisher OOM |

### 优化后 (isWritable + PooledByteBufAllocator)

| Interval | PUB TPS (理论) | PUB TPS (实际) | SUB TPS (理论) | SUB TPS (实际) | Fan-out | Output | Status |
|----------|----------------|----------------|----------------|----------------|---------|--------|--------|
| 25μs | 40,000 | ~22,000 | 2,000,000 | ~710,000 | ~31× | 700 MB/s | ✓ 稳定 (优雅降级) |
| 25μs (Direct) | 40,000 | ~25,000 | 2,000,000 | ~750,000 | ~30× | 750 MB/s | ✓ 稳定 (+12% PUB) |

**结论**:
- 稳定极限: 100μs = 500 MB/s fan-out, 完美 50× 扇出
- 警戒区: 50μs = 700 MB/s fan-out, 扇出降到 35×
- 极限区: 25μs/10μs = Publisher OOM, 无法支撑 40K+ TPS 发送速率
- 瓶颈: Publisher JVM heap (无法支撑 40K+ msg/s 的发送调度)
- **优化效果**: 25μs 从 OOM 提升为稳定运行，isWritable 背压使扇出从 3× 提升到 31×（优雅降级）
- **Direct Buffer**: 改用池化 Direct Buffer 替代 Heap Buffer，避免堆到直接内存拷贝，PUB 吞吐 +12%
