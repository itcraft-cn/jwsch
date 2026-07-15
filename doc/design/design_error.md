# 错误码设计

## 一、错误码枚举设计

### 1.1 ErrorCode枚举

错误码定义为枚举，仅包含两个字段：

```
ErrorCode (enum):
- code: short       // 码值
- desc: String      // 描述
```

### 1.2 枚举定义示例

```
public enum ErrorCode {
    SUCCESS(0, "成功"),
    
    // 协议错误 E0001-E0999
    INVALID_MAGIC(1, "魔数无效"),
    INVALID_VERSION(2, "版本无效"),
    INVALID_COMMAND(3, "命令无效"),
    INVALID_HEADER_LENGTH(4, "包头长度无效"),
    INVALID_BODY_LENGTH(5, "包体长度无效"),
    INVALID_TOPIC_LENGTH(6, "Topic长度无效"),
    DECODE_FAILED(7, "解码失败"),
    ENCODE_FAILED(8, "编码失败"),
    
    // 连接错误 E1001-E1999
    CONNECTION_CLOSED(1001, "连接已关闭"),
    CONNECTION_TIMEOUT(1002, "连接超时"),
    CONNECTION_REFUSED(1003, "连接被拒绝"),
    CONNECTION_RESET(1004, "连接重置"),
    TOO_MANY_CONNECTIONS(1005, "连接数过多"),
    HEARTBEAT_TIMEOUT(1006, "心跳超时"),
    WEBSOCKET_UPGRADE_FAILED(1007, "WebSocket升级失败"),
    
    // 路由错误 E2001-E2999
    SERVICE_NOT_FOUND(2001, "服务未找到"),
    NO_AVAILABLE_INSTANCE(2002, "无可用实例"),
    ROUTE_FAILED(2003, "路由失败"),
    LOAD_BALANCE_FAILED(2004, "负载均衡失败"),
    REQUEST_TIMEOUT(2005, "请求超时"),
    RESPONSE_TIMEOUT(2006, "响应超时"),
    
    // 注册中心错误 E3001-E3999
    REGISTER_FAILED(3001, "注册失败"),
    DEREGISTER_FAILED(3002, "注销失败"),
    DISCOVER_FAILED(3003, "发现失败"),
    SUBSCRIBE_FAILED(3004, "订阅失败"),
    
    // 系统错误 E9001-E9999
    INTERNAL_ERROR(9001, "内部错误"),
    CONFIG_ERROR(9002, "配置错误"),
    OUT_OF_MEMORY(9003, "内存溢出"),
    THREAD_POOL_REJECTED(9004, "线程池拒绝");
    
    private final short code;
    private final String desc;
    
    ErrorCode(int code, String desc) {
        this.code = (short) code;
        this.desc = desc;
    }
    
    public short getCode() {
        return code;
    }
    
    public String getDesc() {
        return desc;
    }
}
```

---

## 二、错误码分类

| 范围 | 分类 | 枚举前缀 |
|------|------|----------|
| 0 | 成功 | SUCCESS |
| 1-999 | 协议错误 | INVALID_*, DECODE_*, ENCODE_* |
| 1001-1999 | 连接错误 | CONNECTION_*, HEARTBEAT_*, WEBSOCKET_* |
| 2001-2999 | 路由错误 | SERVICE_*, ROUTE_*, LOAD_BALANCE_*, REQUEST_*, RESPONSE_* |
| 3001-3999 | 注册中心错误 | REGISTER_*, DEREGISTER_*, DISCOVER_*, SUBSCRIBE_* |
| 9001-9999 | 系统错误 | INTERNAL_*, CONFIG_*, OUT_OF_*, THREAD_POOL_* |

---

## 三、错误码详细定义

### 3.1 协议错误（1-999）

| 枚举名 | code | desc |
|--------|------|------|
| INVALID_MAGIC | 1 | 魔数无效 |
| INVALID_VERSION | 2 | 版本无效 |
| INVALID_COMMAND | 3 | 命令无效 |
| INVALID_HEADER_LENGTH | 4 | 包头长度无效 |
| INVALID_BODY_LENGTH | 5 | 包体长度无效 |
| INVALID_TOPIC_LENGTH | 6 | Topic长度无效 |
| DECODE_FAILED | 7 | 解码失败 |
| ENCODE_FAILED | 8 | 编码失败 |

### 3.2 连接错误（1001-1999）

| 枚举名 | code | desc |
|--------|------|------|
| CONNECTION_CLOSED | 1001 | 连接已关闭 |
| CONNECTION_TIMEOUT | 1002 | 连接超时 |
| CONNECTION_REFUSED | 1003 | 连接被拒绝 |
| CONNECTION_RESET | 1004 | 连接重置 |
| TOO_MANY_CONNECTIONS | 1005 | 连接数过多 |
| HEARTBEAT_TIMEOUT | 1006 | 心跳超时 |
| WEBSOCKET_UPGRADE_FAILED | 1007 | WebSocket升级失败 |

### 3.3 路由错误（2001-2999）

| 枚举名 | code | desc |
|--------|------|------|
| SERVICE_NOT_FOUND | 2001 | 服务未找到 |
| NO_AVAILABLE_INSTANCE | 2002 | 无可用实例 |
| ROUTE_FAILED | 2003 | 路由失败 |
| LOAD_BALANCE_FAILED | 2004 | 负载均衡失败 |
| REQUEST_TIMEOUT | 2005 | 请求超时 |
| RESPONSE_TIMEOUT | 2006 | 响应超时 |

### 3.4 注册中心错误（3001-3999）

| 枚举名 | code | desc |
|--------|------|------|
| REGISTER_FAILED | 3001 | 注册失败 |
| DEREGISTER_FAILED | 3002 | 注销失败 |
| DISCOVER_FAILED | 3003 | 发现失败 |
| SUBSCRIBE_FAILED | 3004 | 订阅失败 |

### 3.5 系统错误（9001-9999）

| 枚举名 | code | desc |
|--------|------|------|
| INTERNAL_ERROR | 9001 | 内部错误 |
| CONFIG_ERROR | 9002 | 配置错误 |
| OUT_OF_MEMORY | 9003 | 内存溢出 |
| THREAD_POOL_REJECTED | 9004 | 线程池拒绝 |

---

## 四、错误码使用

### 4.1 包头错误码字段

错误码直接体现在包头的 `errorCode` 字段中：
- 类型：short (2字节)
- 位置：命令(1B)后面
- 值：0表示成功，非0表示错误

### 4.2 错误响应格式

当发生错误时：
- 包头：`errorCode` 设置为对应的错误码
- 包体：错误消息（UTF-8字符串，可选补充信息）

**示例**：
```
错误枚举: SERVICE_NOT_FOUND
code: 2001
desc: "服务未找到"

包头:
  errorCode: 2001

包体:
  "服务未找到: user-service" (UTF-8)
```

### 4.3 成功响应

成功时，`errorCode` 为 0：
```
包头:
  errorCode: 0
  
包体:
  业务数据（由业务方定义）
```

---

## 五、异常类设计

### 5.1 异常类层次

```
JwschException (extends RuntimeException)
├── ProtocolException
├── ConnectionException
├── RouteException
├── RegistryException
└── ConfigException
```

### 5.2 基类设计

```
JwschException:
- errorCode: ErrorCode    // 错误码枚举
- message: String         // 详细消息
- cause: Throwable

构造方法:
- JwschException(ErrorCode errorCode)
- JwschException(ErrorCode errorCode, String message)
- JwschException(ErrorCode errorCode, String message, Throwable cause)
```

### 5.3 子类示例

```
ProtocolException extends JwschException:
- ProtocolException(ErrorCode errorCode)
- ProtocolException(ErrorCode errorCode, String message)
- ProtocolException(ErrorCode errorCode, String message, Throwable cause)
```

---

## 六、异常处理器

### 6.1 Netty异常处理器

```
ExceptionHandler extends ChannelInboundHandlerAdapter:
- exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
```

### 6.2 处理逻辑

```
exceptionCaught(ctx, cause):
  if (cause instanceof JwschException):
    errorCode = ((JwschException) cause).getErrorCode()
    sendErrorResponse(ctx, errorCode, cause.getMessage())
  else:
    sendErrorResponse(ctx, ErrorCode.INTERNAL_ERROR, "Internal error")
  
  logger.error("Exception caught", cause)
```

---

## 七、使用示例

### 7.1 抛出异常

```
if (magic != MAGIC) {
    throw new ProtocolException(
        ErrorCode.INVALID_MAGIC, 
        "Invalid magic: " + Arrays.toString(magic)
    );
}
```

### 7.2 捕获并处理

```
try {
    packet = decoder.decode(buf);
} catch (ProtocolException e) {
    logger.error("Decode failed: code={}, desc={}, msg={}", 
        e.getErrorCode().getCode(), 
        e.getErrorCode().getDesc(), 
        e.getMessage());
    sendErrorResponse(ctx, e.getErrorCode(), e.getMessage());
}
```

### 7.3 发送错误响应

```
void sendErrorResponse(ChannelHandlerContext ctx, ErrorCode errorCode, String detail) {
    PacketHeader header = new PacketHeader.Builder()
        .command(Command.RESPONSE)
        .errorCode(errorCode.getCode())
        .build();
    
    String body = errorCode.getDesc() + (detail != null ? ": " + detail : "");
    Packet packet = new Packet(header, body.getBytes(StandardCharsets.UTF_8));
    
    ctx.writeAndFlush(packet);
}
```

---

## 八、日志规范

### 8.1 日志级别

| 级别 | 场景 |
|------|------|
| ERROR | 系统错误、严重异常 |
| WARN | 可恢复的异常、业务告警 |
| INFO | 重要操作、连接建立/关闭 |
| DEBUG | 详细调试信息 |

### 8.2 日志格式

```
ERROR - 错误码: 2001, 描述: 服务未找到, 消息: user-service
WARN - 连接超时: connectionId=12345, timeout=30000ms
INFO - 连接建立: connectionId=12345, remoteAddress=192.168.1.100:8080
DEBUG - 解码消息: command=REQUEST, requestId=100, bodyLen=256
```

---

## 九、扩展规则

### 9.1 新增错误码

1. 确定错误码分类
2. 选择合适的code值（遵循分类规则）
3. 添加枚举常量

### 9.2 示例

```
// 新增协议错误
INVALID_CHECKSUM(9, "校验和无效"),

// 新增连接错误
SSL_HANDSHAKE_FAILED(1008, "SSL握手失败"),

// 新增路由错误
CIRCUIT_BREAKER_OPEN(2007, "熔断器已打开"),
```