# 模块结构设计

## 一、Maven模块拆分

```
jwsch/
├── jwsch-common/          # 共享模块
├── jwsch-srv/             # 服务端模块
├── jwsch-cli/             # 客户端模块
└── pom.xml                # 父POM
```

---

## 二、模块依赖关系

```
┌─────────────┐
│  jwsch-srv  │
└──────┬──────┘
       │ depends on
       ▼
┌─────────────┐
│ jwsch-common│
└──────┬──────┘
       ▲
       │ depends on
┌──────┴──────┐
│  jwsch-cli  │
└─────────────┘
```

**说明**：
- jwsch-common：基础模块，被srv和cli依赖
- jwsch-srv：依赖common和cli（内部使用cli连接后端）
- jwsch-cli：仅依赖common

---

## 三、各模块职责

### 3.1 jwsch-common

**职责**：共享代码和基础组件

**包结构**：
```
cn.itcraft.jwsch.common
├── protocol/
│   ├── ProtocolConsts.java
│   ├── Command.java
│   ├── PacketHeader.java
│   ├── Packet.java
│   ├── PacketEncoder.java
│   ├── PacketDecoder.java
│   └── ErrorResponse.java
├── config/
│   ├── TcpConfig.java           # TCP配置基类
│   ├── WriteBufferWaterMark.java
│   └── ConfigValidator.java
├── id/
│   └── IdGenerator.java         # ID生成器
├── exception/
│   ├── JwschException.java
│   ├── ProtocolException.java
│   ├── ConnectionException.java
│   ├── RouteException.java
│   ├── RegistryException.java
│   └── ErrorCode.java
├── cache/
│   ├── Cache.java               # 缓存接口
│   ├── LoadingCache.java        # 自动加载缓存
│   ├── CacheBuilder.java        # 缓存构建器
│   ├── CacheConfig.java         # 缓存配置
│   ├── CacheManager.java        # 缓存管理器
│   └── impl/
│       ├── ConcurrentHashMapCache.java
│       └── GuavaCache.java
├── util/
│   ├── StringUtils.java
│   └── ValidateUtils.java
└── bytebuf/
    ├── ByteBufAllocatorFactory.java
    └── ByteBufConfig.java
```

**核心类**：
- `ProtocolConsts`：协议常量
- `Packet`：协议消息
- `IdGenerator`：ID生成器
- `TcpConfig`：TCP配置基类
- `Cache`：通用缓存接口
- `CacheManager`：缓存管理器

### 3.2 jwsch-srv

**职责**：WebSocket服务端实现

**包结构**：
```
cn.itcraft.jwsch.srv
├── JwschServer.java             # 主入口
├── config/
│   ├── ServerConfig.java        # 服务端总配置
│   ├── WebSocketServerConfig.java
│   ├── HttpServerConfig.java
│   └── TcpServerConfig.java     # 继承common.TcpConfig
├── server/
│   ├── websocket/
│   │   ├── WebSocketServer.java
│   │   ├── WebSocketInitializer.java
│   │   ├── WebSocketHandler.java
│   │   └── WebSocketConnectionManager.java
│   └── http/
│       ├── HttpServer.java
│       └── HealthCheckHandler.java
├── router/
│   ├── PacketRouter.java
│   └── ResponseMapping.java
├── registry/
│   ├── ServiceRegistry.java
│   ├── ServiceChangeListener.java
│   ├── ServiceInstance.java
│   └── InMemoryServiceRegistry.java
├── loadbalance/
│   ├── LoadBalance.java
│   ├── RandomLoadBalance.java
│   ├── RoundRobinLoadBalance.java
│   └── ConsistentHashLoadBalance.java
└── client/
    └── BackendClient.java       # 后端连接管理（内部使用cli）
```

**核心类**：
- `JwschServer`：服务端主入口
- `WebSocketServer`：WebSocket服务
- `PacketRouter`：消息路由器
- `InMemoryServiceRegistry`：内存注册中心

### 3.3 jwsch-cli

**职责**：TCP客户端实现

**包结构**：
```
cn.itcraft.jwsch.cli
├── JwschClient.java             # 主入口
├── config/
│   ├── ClientConfig.java        # 客户端总配置
│   └── TcpClientConfig.java     # 继承common.TcpConfig
├── client/
│   ├── TcpClient.java
│   ├── TcpClientInitializer.java
│   ├── TcpHandler.java
│   └── ReconnectHandler.java
├── pool/
│   ├── ConnectionPool.java
│   └── ConnectionPoolManager.java
└── eventloop/
    ├── EventLoopHolder.java     # EventLoop持有者
    └── SharedEventLoopManager.java  # 共享EventLoop管理
```

**核心类**：
- `JwschClient`：客户端主入口
- `TcpClient`：TCP连接管理
- `SharedEventLoopManager`：共享EventLoop管理

---

## 四、模块依赖详情

### 4.1 jwsch-common依赖

```xml
<dependencies>
    <dependency>
        <groupId>io.netty</groupId>
        <artifactId>netty-all</artifactId>
    </dependency>
    <dependency>
        <groupId>com.google.guava</groupId>
        <artifactId>guava</artifactId>
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
    </dependency>
</dependencies>
```

### 4.2 jwsch-srv依赖

```xml
<dependencies>
    <dependency>
        <groupId>cn.itcraft</groupId>
        <artifactId>jwsch-common</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.itcraft</groupId>
        <artifactId>jwsch-cli</artifactId>
    </dependency>
</dependencies>
```

### 4.3 jwsch-cli依赖

```xml
<dependencies>
    <dependency>
        <groupId>cn.itcraft</groupId>
        <artifactId>jwsch-common</artifactId>
    </dependency>
</dependencies>
```

---

## 五、模块独立使用

### 5.1 仅使用common

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jwsch-common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**适用场景**：
- 仅需协议编解码
- 自定义服务端/客户端实现

### 5.2 仅使用cli

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jwsch-cli</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**适用场景**：
- 仅需TCP客户端功能
- 连接jwsch服务端或其他服务

### 5.3 使用完整服务端

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jwsch-srv</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**适用场景**：
- 完整的中间件服务端
- WebSocket接入 + TCP转发

---

## 六、模块边界原则

### 6.1 依赖原则

- **单向依赖**：srv -> cli -> common
- **禁止循环依赖**
- **common零依赖**：common不依赖srv或cli

### 6.2 类职责原则

| 模块 | 职责边界 |
|------|----------|
| common | 协议、配置、异常、工具类 |
| cli | TCP连接、EventLoop管理、连接池 |
| srv | WebSocket服务、路由、注册中心、负载均衡 |

### 6.3 接口原则

- common：提供基础接口和实现
- cli：依赖common接口，提供客户端功能
- srv：依赖common和cli，提供服务端功能

---

## 七、版本管理

### 7.1 统一版本

所有模块使用相同版本号：
```
jwsch-common: 1.0.0-SNAPSHOT
jwsch-cli: 1.0.0-SNAPSHOT
jwsch-srv: 1.0.0-SNAPSHOT
```

### 7.2 版本依赖

父POM使用`<dependencyManagement>`统一管理版本：
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.itcraft</groupId>
            <artifactId>jwsch-common</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---