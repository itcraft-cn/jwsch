# ByteBuf零拷贝管理设计

## 一、设计目标

- 零拷贝：减少数据复制，提升性能
- 内存池：减少GC压力，提升分配性能
- 引用计数：精确管理内存生命周期
- 内存泄漏检测：开发/测试环境检测，生产环境禁用

---

## 二、内存池配置

### 2.1 ByteBuf分配器

**选择**：`PooledByteBufAllocator`（池化分配器）

**优点**：
- 减少GC压力
- 提升分配性能
- 复用内存块

### 2.2 配置项

```properties
jwsch.bytebuf.pool.enabled=true
jwsch.bytebuf.pool.direct=true                    # 使用直接内存
jwsch.bytebuf.pool.leak.detection=SIMPLE          # 内存泄漏检测级别
```

### 2.3 泄漏检测级别

| 级别 | 说明 | 性能影响 | 适用场景 |
|------|------|----------|----------|
| DISABLED | 禁用检测 | 无 | 生产环境 |
| SIMPLE | 简单采样检测 | 低（~1%） | 生产环境（默认） |
| ADVANCED | 高级采样检测 | 中（~10%） | 测试环境 |
| PARANOID | 每次分配都检测 | 高（~50%） | 调试阶段 |

---

## 三、引用计数管理

### 3.1 基本原则

1. **谁分配，谁释放**：分配ByteBuf的组件负责最终释放
2. **引用计数规则**：
   - 新分配的ByteBuf引用计数为1
   - 每次 `retain()` 引用计数+1
   - 每次 `release()` 引用计数-1
   - 引用计数为0时，内存被回收

### 3.2 引用计数流程

#### 点对点消息

```
解码器(decoder) -> handler -> encoder -> 写出 -> release

引用计数变化：
1. 解码：refCnt = 1
2. handler处理：refCnt = 1
3. 写出成功：release -> refCnt = 0（内存回收）
4. 写出失败：release -> refCnt = 0（内存回收）
```

#### 广播消息

```
解码器(decoder) -> handler -> 遍历所有连接
                    -> retain + 写出 -> release（每个连接）
                    -> 最终release（原始refCnt-1）

引用计数变化：
1. 解码：refCnt = 1
2. 广播N个连接：retain N次 -> refCnt = N+1
3. 每个连接写出：release -> refCnt递减
4. 最后一个写出完成：refCnt = 0（内存回收）
```

---

## 四、点对点消息处理

### 4.1 请求转发

```
WebSocket接收 -> 解码 -> 查找后端连接 -> 写出 -> 释放
```

**实现要点**：
1. 解码后的Packet持有ByteBuf引用
2. 直接转发整个Packet对象
3. 写出后必须释放（成功或失败都要释放）

---

## 五、广播消息处理

### 5.1 广播策略

- 不重新构建Packet
- 使用引用计数
- 每个Channel写出后独立release

### 5.2 实现要点

```
1. 保留原始ByteBuf引用
2. 遍历所有目标连接
3. 每个连接 retain() + writeAndFlush()
4. 使用ChannelFutureListener监听写出结果
5. 写出完成后自动release()
```

### 5.3 异常情况处理

| 异常情况 | 处理策略 |
|----------|----------|
| Channel不活跃 | 跳过，不计入成功数 |
| 写出失败 | 记录日志，不影响其他连接 |
| 连接中途断开 | Netty自动释放 |
| ByteBuf已释放 | 抛出IllegalReferenceCountException（需捕获） |
| 引用计数异常 | 记录错误日志，跳过该连接 |

---

## 六、Packet结构调整

### 6.1 持有ByteBuf引用

Packet内部直接持有ByteBuf，而非byte[]

```
Packet:
- header: PacketHeader
- bodyBuf: ByteBuf  // 直接持有ByteBuf引用
```

### 6.2 关键方法

```
Packet:
- getBodyBuf(): ByteBuf
- release(): void           // 释放bodyBuf引用
- retain(): Packet          // 增加引用计数
- resetReaderIndex(): void  // 重置读指针（广播用）
```

---

## 七、编解码器设计

### 7.1 Decoder设计

**零拷贝技巧**：
```
// 解析body（零拷贝）
ByteBuf bodyBuf = in.slice(in.readerIndex(), bodyLength);
bodyBuf.retain();  // 增加引用计数

// 构造Packet
Packet packet = new Packet(header, bodyBuf);

// 不需要in.readBytes()，避免复制
in.skipBytes(bodyLength);
```

### 7.2 Encoder设计

```
void encode(Packet msg, ByteBuf out) {
    // 写固定包头
    out.writeBytes(MAGIC);
    out.writeShort(msg.getHeaderLength());
    ...
    
    // 写body（零拷贝）
    ByteBuf bodyBuf = msg.getBodyBuf();
    if (bodyBuf != null) {
        out.writeBytes(bodyBuf);  // Netty内部优化
    }
}
```

---

## 八、内存管理规范

### 8.1 编码规范

**必须**：
1. 每个retain()必须有对应的release()
2. 异常分支也要确保release()
3. 使用try-finally或ChannelFutureListener确保释放

**禁止**：
1. 禁止忘记release()
2. 禁止重复release()
3. 禁止访问已释放的ByteBuf

### 8.2 单元测试要求

**测试用例**：
1. 正常流程：验证ByteBuf最终释放
2. 异常流程：验证ByteBuf仍然释放
3. 广播流程：验证引用计数正确

---

## 九、性能优化总结

| 场景 | 优化策略 | 收益 |
|------|----------|------|
| 点对点 | 直接转发ByteBuf | 零拷贝，减少GC |
| 广播 | retain + resetReaderIndex | 避免重复构建 |
| 编解码 | slice/duplicate | 避免数据复制 |
| 内存分配 | PooledByteBufAllocator | 减少分配开销 |