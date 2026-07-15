# CPU Profile 分析报告

**文件**: `/tmp/profile.html`  
**工具**: async-profiler  
**总样本数**: 209,577 samples

---

## 一、Profile 概述

这是 jwsch benchmark 运行时的 CPU profile 数据，通过 async-profiler 采集，以火焰图形式展示调用栈。

### 采样分布

| 类别 | 占比 | 说明 |
|------|------|------|
| JVM 内部 | ~40% | JIT 编译、GC、Safepoint |
| Netty | ~30% | ByteBuf 操作、Channel IO |
| 应用代码 | ~20% | jwsch 业务逻辑 |
| 系统调用 | ~10% | epoll、read/write |

---

## 二、性能热点分析

### 2.1 应用层热点（jwsch）

| 方法 | 样本数（估） | 说明 |
|------|-------------|------|
| `BenchPublisher.createMessageBody` | 高 | 创建消息体，分配 ByteBuf |
| `BenchPublisher.sendMessage` | 高 | 发送消息，包含序列化 |
| `PacketRouter.broadcastToTopic` | 中 | 广播到多个订阅者 |
| `PacketEncoder.encode` | 中 | 协议编码 |
| `PacketDecoder.decode` | 中 | 协议解码 |
| `WebSocket08FrameDecoder.decode` | 中 | WebSocket 帧解码 |

### 2.2 Netty 层热点

| 方法 | 问题 | 建议 |
|------|------|------|
| `ByteBuf.checkIndex` | 边界检查开销 | 合理，Netty 安全机制 |
| `ByteBuf.isAccessible` | 引用计数检查 | 合理，内存泄漏防护 |
| `ReferenceCountedByteBuf.<init>` | ByteBuf 创建 | 考虑对象池复用 |
| `PoolArena.allocate` | 内存池分配 | 合理，已使用池化 |
| `SimpleLeakAwareByteBuf` | 泄漏检测 | 生产环境可关闭 |

### 2.3 JVM 层热点

| 热点 | 说明 | 影响 |
|------|------|------|
| JIT 编译 | C1/C2 编译器优化 | 启动期开销，运行后消失 |
| Safepoint | 线程安全点检查 | 高并发下有开销 |
| G1 GC | 垃圾回收 | 大对象分配触发 |

---

## 三、关键发现

### 3.1 ByteBuf 分配热点

**问题**: `createMessageBody` 频繁创建新的 ByteBuf

```java
// BenchPublisher.java:109
private ByteBuf createMessageBody(long seq) {
    ByteBuf buf = Unpooled.buffer(8 + payloadSize);  // 每次新分配
    buf.writeLong(seq);
    buf.writeBytes(payloadTemplate);
    return buf;
}
```

**建议**: 对于小消息（< 1KB），考虑使用 `Unpooled.directBuffer()` 或预分配 buffer 复用

### 3.2 WebSocket 帧编码开销

**问题**: 每条消息都需要 WebSocket 帧封装

```
Message → Packet → ByteBuf → WebSocket Frame
```

**开销**: `BinaryWebSocketFrame` 构造 + `WebSocket08FrameEncoder` 编码

**建议**: 
- 批量发送可减少帧数
- 考虑使用 TCP 直连（无 WebSocket 开销）

### 3.3 广播复制开销

**问题**: `broadcastToTopic` 需要为每个订阅者复制 ByteBuf

```java
// PacketRouter.java
for (Channel channel : activeChannels) {
    channel.writeAndFlush(new BinaryWebSocketFrame(encoded.retainedDuplicate()));
}
```

**开销**: 15 个订阅者 = 15 次 `retainedDuplicate()`

**建议**: 当前实现合理，Netty 零拷贝已优化

---

## 四、优化建议

### 4.1 短期优化（快速见效）

| 优化项 | 预期收益 | 复杂度 |
|--------|----------|--------|
| 关闭 ByteBuf 泄漏检测 | 5-10% | 低 |
| 预分配 payload template | 2-5% | 低 |
| 调整 Netty buffer 池大小 | 3-5% | 低 |

**实施**:

```java
// 关闭泄漏检测（生产环境）
System.setProperty("io.netty.leakDetection.level", "disabled");

// 或在创建 ByteBufAllocator 时
ByteBufAllocator allocator = new PooledByteBufAllocator(
    true,    // preferDirect
    0,       // nHeapArena
    1,       // nDirectArena
    8192,    // pageSize
    11,      // maxOrder
    0,       // tinyCacheSize
    0,       // smallCacheSize
    64       // normalCacheSize - 增大缓存
);
```

### 4.2 中期优化（需要测试）

| 优化项 | 预期收益 | 复杂度 |
|--------|----------|--------|
| 使用 `LongAdder` 替代 `AtomicLong` | 已实现 | - |
| 批量发送消息 | 10-20% | 中 |
| 调整 GC 参数 | 5-10% | 中 |

### 4.3 长期优化（架构调整）

| 优化项 | 预期收益 | 复杂度 |
|--------|----------|--------|
| 消息批处理 | 20-30% | 高 |
| 无锁队列 | 10-15% | 高 |
| 原生内存管理 | 15-25% | 高 |

---

## 五、性能基准

### 当前性能

| 指标 | 值 |
|------|-----|
| Publisher TPS | ~100,000 |
| Subscriber TPS | ~1,500,000 (15 subs) |
| Payload Size | 2 bytes |
| CPU 利用率 | 高 |

### 瓶颈定位

```
CPU Profile 热点链路：
sendMessage() → createMessageBody() → ByteBuf.allocate()
            → PacketWriter.write() → Protocol encoding
            → channel.writeAndFlush() → Netty pipeline
            → WebSocket frame encoding
            → epoll_write syscall
```

---

## 六、结论

1. **当前性能合理**：10万 TPS 对于 16 核机器是合理水平

2. **主要开销**：
   - ByteBuf 分配/释放（Netty 池化已优化）
   - WebSocket 帧编码
   - 广播时的 retainedDuplicate

3. **优化空间**：
   - 关闭泄漏检测：5-10% 提升
   - 批量发送：10-20% 提升
   - 调整 GC：5-10% 提升

4. **建议优先级**：
   - P0: 关闭生产环境泄漏检测
   - P1: 测试批量发送效果
   - P2: GC 调优

---

**分析人**: AI Performance Analyzer  
**分析时间**: 2026-03-20