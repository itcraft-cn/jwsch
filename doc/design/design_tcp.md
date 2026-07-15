# TCP参数配置设计

## 一、设计目标

- 提供足够的TCP参数配置自由度
- 服务端和客户端分别配置
- 合理的默认值
- 参数校验

---

## 二、WebSocket服务端配置

### 2.1 配置项

```properties
# WebSocket服务端TCP参数
jwsch.srv.websocket.tcp.nodelay=true
jwsch.srv.websocket.tcp.backlog=2048
jwsch.srv.websocket.tcp.sndbuf=1048576
jwsch.srv.websocket.tcp.rcvbuf=1048576
jwsch.srv.websocket.tcp.write.buffer.low=32768
jwsch.srv.websocket.tcp.write.buffer.high=65536

# 超时时间
jwsch.srv.websocket.timeout.connect=30000
jwsch.srv.websocket.timeout.read=0
jwsch.srv.websocket.timeout.write=0
```

### 2.2 参数说明

| 参数 | 配置项 | 类型 | 默认值 | 说明 |
|------|--------|------|--------|------|
| TCP_NODELAY | tcp.nodelay | boolean | true | 禁用Nagle算法，减少延迟 |
| SO_BACKLOG | tcp.backlog | int | 2048 | 连接队列深度 |
| SO_SNDBUF | tcp.sndbuf | int | 1048576 (1MB) | 发送缓冲区大小 |
| SO_RCVBUF | tcp.rcvbuf | int | 1048576 (1MB) | 接收缓冲区大小 |

---

## 三、TCP客户端配置

### 3.1 配置项

```properties
# TCP客户端参数
jwsch.cli.tcp.nodelay=true
jwsch.cli.tcp.keepalive=true
jwsch.cli.tcp.sndbuf=1048576
jwsch.cli.tcp.rcvbuf=1048576
jwsch.cli.tcp.write.buffer.low=32768
jwsch.cli.tcp.write.buffer.high=65536

# 超时时间
jwsch.cli.tcp.timeout.connect=30000
jwsch.cli.tcp.timeout.read=0
jwsch.cli.tcp.timeout.write=0
```

### 3.2 参数说明

| 参数 | 配置项 | 类型 | 默认值 | 说明 |
|------|--------|------|--------|------|
| TCP_NODELAY | tcp.nodelay | boolean | true | 禁用Nagle算法 |
| SO_KEEPALIVE | tcp.keepalive | boolean | true | 启用TCP Keep-Alive |
| SO_SNDBUF | tcp.sndbuf | int | 1048576 | 发送缓冲区大小 |
| SO_RCVBUF | tcp.rcvbuf | int | 1048576 | 接收缓冲区大小 |

---

## 四、写缓冲区水位（WriteBufferWaterMark）

### 4.1 配置

```properties
jwsch.srv.websocket.tcp.write.buffer.low=32768
jwsch.srv.websocket.tcp.write.buffer.high=65536
```

### 4.2 水位机制

- 缓冲区字节数 < 低水位：Channel可写
- 缓冲区字节数 > 高水位：Channel不可写
- 缓冲区字节数从高水位降到低水位：Channel恢复可写

### 4.3 作用

- 控制小包发送频率
- 防止内存溢出
- 流量控制

---

## 五、超时配置

### 5.1 超时类型

| 参数 | 配置项 | 默认值 | 说明 |
|------|--------|--------|------|
| 连接超时 | timeout.connect | 30000 (30s) | 连接建立超时 |
| 读超时 | timeout.read | 0（不超时） | 读取数据超时 |
| 写超时 | timeout.write | 0（不超时） | 写入数据超时 |

### 5.2 使用场景

**读超时**：
- 设置为0：不限制（配合心跳使用）
- 设置为N秒：N秒未收到数据则超时

**写超时**：
- 设置为0：不限制
- 设置为N秒：N秒未写出数据则超时

---

## 六、配置类设计

### 6.1 TcpConfig基类

```
TcpConfig:
- nodelay: boolean
- sndbuf: int
- rcvbuf: int
- writeBufferWaterMark: WriteBufferWaterMark
- connectTimeout: int
- readTimeout: int
- writeTimeout: int
```

### 6.2 TcpServerConfig

```
TcpServerConfig extends TcpConfig:
- backlog: int
```

### 6.3 TcpClientConfig

```
TcpClientConfig extends TcpConfig:
- keepalive: boolean
```

---

## 七、Netty Bootstrap配置

### 7.1 服务端配置

```
ServerBootstrap bootstrap = new ServerBootstrap();
bootstrap
    .option(ChannelOption.SO_BACKLOG, tcpConfig.getBacklog())
    .option(ChannelOption.SO_RCVBUF, tcpConfig.getRcvbuf())
    .childOption(ChannelOption.TCP_NODELAY, tcpConfig.isNodelay())
    .childOption(ChannelOption.SO_SNDBUF, tcpConfig.getSndbuf())
    .childOption(ChannelOption.SO_RCVBUF, tcpConfig.getRcvbuf())
    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, 
        new WriteBufferWaterMark(low, high))
    .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 
        tcpConfig.getConnectTimeout())
    .childOption(ChannelOption.ALLOCATOR, 
        PooledByteBufAllocator.DEFAULT);
```

### 7.2 客户端配置

```
Bootstrap bootstrap = new Bootstrap();
bootstrap
    .option(ChannelOption.TCP_NODELAY, tcpConfig.isNodelay())
    .option(ChannelOption.SO_KEEPALIVE, tcpConfig.isKeepalive())
    .option(ChannelOption.SO_SNDBUF, tcpConfig.getSndbuf())
    .option(ChannelOption.SO_RCVBUF, tcpConfig.getRcvbuf())
    .option(ChannelOption.WRITE_BUFFER_WATER_MARK,
        new WriteBufferWaterMark(low, high))
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 
        tcpConfig.getConnectTimeout())
    .option(ChannelOption.ALLOCATOR, 
        PooledByteBufAllocator.DEFAULT);
```

---

## 八、参数校验

### 8.1 校验规则

| 参数 | 校验规则 |
|------|----------|
| backlog | 1 ~ 65535 |
| sndbuf | 1024 ~ 16777216 (1KB ~ 16MB) |
| rcvbuf | 1024 ~ 16777216 (1KB ~ 16MB) |
| writeBuffer.low | 1024 ~ 1048576 |
| writeBuffer.high | 必须 > writeBuffer.low |
| connectTimeout | 1000 ~ 300000 (1s ~ 5min) |
| readTimeout | 0 ~ 86400 (0表示不限制) |
| writeTimeout | 0 ~ 86400 (0表示不限制) |

---

## 九、最佳实践

### 9.1 低延迟场景

```properties
jwsch.srv.websocket.tcp.nodelay=true
jwsch.srv.websocket.tcp.write.buffer.low=8192
jwsch.srv.websocket.tcp.write.buffer.high=16384
```

### 9.2 高吞吐场景

```properties
jwsch.srv.websocket.tcp.sndbuf=4194304
jwsch.srv.websocket.tcp.rcvbuf=4194304
jwsch.srv.websocket.tcp.write.buffer.low=131072
jwsch.srv.websocket.tcp.write.buffer.high=262144
```

### 9.3 高并发场景

```properties
jwsch.srv.websocket.tcp.backlog=4096
jwsch.srv.websocket.tcp.sndbuf=524288
jwsch.srv.websocket.tcp.rcvbuf=524288
```

---

## 十、系统参数调优

**Linux系统参数**（需配合调整）：
```bash
# /etc/sysctl.conf
net.core.somaxconn=4096
net.core.rmem_max=16777216
net.core.wmem_max=16777216
net.ipv4.tcp_rmem=4096 87380 16777216
net.ipv4.tcp_wmem=4096 65536 16777216
```