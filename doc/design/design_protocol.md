# 协议设计

## 一、设计目标

设计高效的二进制协议，满足以下要求：
- 固定包头，便于快速解析
- 变长Topic，灵活支持订阅
- 零拷贝友好，减少数据复制
- 业务无关，包体由业务方定义

---

## 二、协议格式

### 2.1 整体结构

```
[固定包头 27B] + [变长Topic] + [包体]
```

### 2.2 固定包头（27字节）

```
┌──────────┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────┐
│ 魔数(2B) │包头长(2B)│包体长(4B)│ 命令(1B) │错误码(2B)│源ID(8B)  │目标ID(8B) │
│[e7,34]   │ short    │ int      │ byte     │ short    │ long     │ long     │
└──────────┴──────────┴──────────┴──────────┴──────────┴──────────┴──────────┘
```

### 2.3 字段说明

| 字段 | 类型 | 长度 | 说明 |
|------|------|------|------|
| magic | byte[2] | 2B | 魔数 `[0xe7, 0x34]`，协议识别 |
| headerLength | short | 2B | 整个包头长度（27 + topic长度） |
| bodyLength | int | 4B | 包体长度 |
| command | byte | 1B | 命令类型 |
| errorCode | short | 2B | 错误码，0表示成功 |
| sourceId | long | 8B | 源连接ID |
| targetId | long | 8B | 目标连接ID，可为空（值为0） |
| topic | String | 变长 | Topic字符串（ASCII），可为空 |

### 2.4 变长Topic

- 编码：ASCII
- 最大长度：256字节
- 长度计算：`topic长度 = headerLength - 27`
- 为空时不占空间

### 2.5 包体

- 长度由 `bodyLength` 指定
- 内容由业务方定义
- 默认最大长度：99999字节
- 可配置最大长度：9999999字节

---

## 三、命令类型

### 3.1 命令定义

| 命令 | 值 | 场景 | 源ID | 目标ID | Topic |
|------|-----|------|------|--------|-------|
| REQUEST | 0x01 | 客户端请求 | 客户端连接ID | 后端服务ID | 可选 |
| RESPONSE | 0x02 | 服务端响应 | 后端服务ID | 客户端连接ID | 空 |
| PUSH | 0x03 | 服务端推送 | 后端服务ID | 客户端连接ID | 可选 |
| BROADCAST | 0x04 | 广播消息 | 广播源ID | 空（广播所有） | 可选 |
| SUBSCRIBE | 0x05 | 订阅Topic | 客户端连接ID | 空 | 必填 |
| HEARTBEAT | 0x06 | 心跳检测 | 发送方ID | 接收方ID | 空 |
| ACK | 0x07 | 消息确认 | 接收方ID | 发送方ID | 空 |

### 3.2 命令使用场景

#### REQUEST（请求）

**场景**：前端请求后端服务

**流程**：
1. 前端发送REQUEST，sourceId为前端连接ID
2. jwsch路由到后端服务，targetId为后端连接ID
3. 后端处理并返回RESPONSE

**示例**：
```
前端连接ID: 12345
后端服务ID: 67890

REQUEST:
  sourceId: 12345
  targetId: 67890
  topic: /api/user/get
  body: {"userId": 100}
```

#### RESPONSE（响应）

**场景**：后端服务返回响应

**流程**：
1. 后端发送RESPONSE，sourceId为后端连接ID
2. jwsch根据targetId路由到前端连接
3. 前端收到响应

**示例**：
```
RESPONSE:
  sourceId: 67890
  targetId: 12345
  body: {"code": 0, "data": {...}}
```

#### PUSH（推送）

**场景**：后端服务主动推送消息

**流程**：
1. 后端发送PUSH，targetId指定前端连接
2. jwsch转发到指定前端连接

**示例**：
```
PUSH:
  sourceId: 67890
  targetId: 12345
  topic: /notify/message
  body: {"message": "新消息"}
```

#### BROADCAST（广播）

**场景**：广播消息到所有前端连接

**流程**：
1. 后端发送BROADCAST，targetId为0
2. jwsch转发到所有活跃的前端连接

**示例**：
```
BROADCAST:
  sourceId: 67890
  targetId: 0
  topic: /broadcast/announcement
  body: {"message": "系统公告"}
```

#### SUBSCRIBE（订阅）

**场景**：前端订阅Topic

**流程**：
1. 前端发送SUBSCRIBE，topic为订阅主题
2. jwsch记录订阅关系
3. 后续PUSH/BROADCAST到该topic的消息会被转发

**示例**：
```
SUBSCRIBE:
  sourceId: 12345
  targetId: 0
  topic: /topic/news
```

#### HEARTBEAT（心跳）

**场景**：连接保活

**流程**：
1. 定时发送HEARTBEAT
2. 接收方立即回复HEARTBEAT

**示例**：
```
HEARTBEAT:
  sourceId: 12345
  targetId: 67890
```

---

## 四、协议约束

### 4.1 必须满足

- 魔数必须为 `[0xe7, 0x34]`，否则拒绝连接
- 包头长度必须 >= 27
- 包体长度范围：`[0, 配置的最大长度]`
- Topic长度范围：`[0, 256]`
- 源ID和目标ID为 `long` 类型，0表示空
- 错误码为 `short` 类型，0表示成功

### 4.2 校验规则

| 字段 | 校验规则 |
|------|----------|
| magic | 必须为 `[0xe7, 0x34]` |
| headerLength | `[27, 27 + 256]` |
| bodyLength | `[0, jwsch.protocol.body.length.max]` |
| command | `[0x01, 0x07]` |
| errorCode | `[0, 9999]` |
| topic.length | `[0, 256]` |

---

## 五、零拷贝设计

### 5.1 Packet结构

Packet内部直接持有ByteBuf，而非byte[]：

```
Packet:
- header: PacketHeader
- bodyBuf: ByteBuf  // 直接持有ByteBuf引用
```

### 5.2 关键方法

```
Packet:
- getBodyBuf(): ByteBuf
- release(): void           // 释放bodyBuf引用
- retain(): Packet          // 增加引用计数
- resetReaderIndex(): void  // 重置读指针（广播用）
```

### 5.3 编解码零拷贝

#### Decoder零拷贝

使用slice/duplicate，避免数据复制：

```
// 解析body（零拷贝）
ByteBuf bodyBuf = in.slice(in.readerIndex(), bodyLength);
bodyBuf.retain();  // 增加引用计数

// 构造Packet
Packet packet = new Packet(header, bodyBuf);

// 不需要in.readBytes()，避免复制
in.skipBytes(bodyLength);
```

#### Encoder零拷贝

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

## 六、错误码使用

### 6.1 错误码字段

错误码直接体现在包头的 `errorCode` 字段中：
- 位置：命令(1B)后面
- 类型：short (2B)
- 值：0表示成功，非0表示错误

### 6.2 错误响应

当发生错误时，包头中的 `errorCode` 设置为对应的错误码，包体中包含错误消息（UTF-8字符串）。

**示例**：
```
错误码: E2001 (0x07D1)
错误消息: "Service not found: user-service"

包头:
  command: RESPONSE (0x02)
  errorCode: 0x07D1 (2001)
  
包体:
  "Service not found: user-service" (UTF-8)
```

### 6.3 成功响应

成功时，`errorCode` 为 0：
```
包头:
  command: RESPONSE (0x02)
  errorCode: 0x0000 (0)
  
包体:
  业务数据（由业务方定义）
```

---

## 七、配置项

```properties
# 协议配置
jwsch.protocol.body.length.default=99999
jwsch.protocol.body.length.max=9999999
```

---

## 八、性能考虑

### 8.1 解析性能

- 固定包头27字节，可用`if (in.readableBytes() < 27) return;`快速判断
- 魔数校验在最早位置，快速拒绝非法连接
- 长度字段前置，便于内存预分配

### 8.2 内存效率

- 使用PooledByteBufAllocator
- 零拷贝设计减少数据复制
- 引用计数管理生命周期

### 8.3 扩展性

- 包体完全由业务方定义
- 命令字段可扩展新类型
- 错误码字段预留，便于错误处理
- Topic支持业务自定义

---