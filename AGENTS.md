# jwsch 项目开发指南

## 角色定位

1. 你是资深架构师
    - 在开发前，会对需求进行详尽分析，提供多套方案，以上、中、下三策的形式呈现，以备后续决策参考
    - 在设计时，会充分考虑非功能性需求：安全性、可扩展性、可用性、可观测性、性能等
    - 在设计细节时，充分考虑各种设计模式及各语言特性
2. 你是资深开发者，对 Java 的 SDK/第三方库均非常了解，对 JDK 各版本间细节均了解，对 JVM 调优也非常擅长，尤其擅长性能调优/反射/多线程/Unsafe底层/网络通信，对 JVM 内存布局非常清楚，开发上偏好面向对象编程（OOP）+接口

---

## 项目概述

jwsch 是基于 Netty 实现的中间件平台，用于前后端消息转发和通信。

**技术栈**：Java 8 + Netty 4.2.x + SLF4J + Guava + Micrometer

**模块结构**：

```
jwsch/
├── jwsch-common/    # 共享模块：协议、ID生成、缓存、异常
├── jwsch-cli/       # 客户端模块：TCP连接、连接池
├── jwsch-srv/       # 服务端模块：WebSocket服务、路由、集群
├── jwschd/          # 独立部署模块：YAML配置、启动器
├── jwsch-bench/     # 压测模块：Benchmark工具
└── jwsch-sample/    # 示例模块：server/webapp/pusher
```

---

## 环境信息

通过 skill /java-env 获取

**JDK**：Java 8，目录：`/usr/lib/jvm` 或 `/home/helly/lang`

**Maven**：优先使用 mvnd (`/home/helly/app/maven-mvnd`)，备选 `/home/helly/app/apache-maven`

**环境变量**：

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

---

## 交互规则

1. 每次沟通产出文件后，均执行 git 提交
2. git 仅以当前 `user.name` 提交，不推送到远端
3. git 提交均遵循约定式提交规范（Conventional Commits）执行

---

## 构建与测试命令

### 构建命令

```bash
# 编译项目
mvnd compile

# 打包（跳过测试）
mvnd package -Dmaven.test.skip=true

# 清理并重新打包
mvnd clean package -Dmaven.test.skip=true

# 安装到本地仓库
mvnd install -DskipTests
```

### 测试命令

```bash
# 运行所有单元测试（通过 TestSuite）
mvnd test -pl jwsch-test -Dtest=AllTests

# 运行单个模块测试套件
mvnd test -pl jwsch-test -Dtest=CommonTestSuite
mvnd test -pl jwsch-test -Dtest=CliTestSuite
mvnd test -pl jwsch-test -Dtest=SrvTestSuite

# 运行单个测试类
mvnd test -pl jwsch-test -Dtest=PacketEncoderTest

# 运行单个测试方法
mvnd test -pl jwsch-test -Dtest=PacketEncoderTest#testEncode_normalPacket

# 运行集成测试
mvnd test -pl jwsch-test -Dtest=*IntegrationTest

# 生成测试覆盖率报告（需要 jacoco 插件）
mvnd test -pl jwsch-test jacoco:report
```

### 测试模块结构

测试统一放在 `jwsch-test` 模块：

```
jwsch-test/
├── src/test/java/cn/itcraft/jwsch/
│   ├── AllTests.java              # 全量测试入口
│   ├── common/
│   │   ├── CommonTestSuite.java   # common 模块测试套件
│   │   └── ...                    # 18 个测试
│   ├── cli/
│   │   ├── CliTestSuite.java      # cli 模块测试套件
│   │   └── ...                    # 12 个测试
│   └── srv/
│       ├── SrvTestSuite.java      # srv 模块测试套件
│       └── ...                    # 34 个测试
```

### 快速启动

```bash
# 启动示例服务（WebSocket:8080, TCP:9090, Webapp:3000）
./start-sample.sh

# 启动压测（1 pub × 12 sub）
./start-bench.sh --publishers 1 --subscribers 12 --duration 1

# 调试模式启动
./debug-sample.sh
```

---

## 编码规范

授权读取：/disk2/helly_data/code/markdown/self-ai-spec/spec.java.md

### 通用规则

1. **对文件有总体注释，对重点代码有详尽注释**
2. **不使用行尾注释**
3. **静态不可变变量名使用大写**：`static final int MAX_SIZE = 1024;`
4. **静态可变变量名使用小写**：`static AtomicInteger counter = new AtomicInteger();`
5. **类设计遵循单一职责原则**
6. **优先使用组合而非继承**
7. **禁用 `var` 关键字**（Java 8不支持）

### 命名约定

| 类型       | 命名风格      | 示例                                   |
|----------|-----------|--------------------------------------|
| 类名       | 帕斯卡命名法    | `UserStatus`, `PacketEncoder`        |
| 方法名      | 驼峰命名法     | `valueOf`, `registerAll`             |
| 变量名      | 驼峰命名法     | `connectionId`, `packetSize`         |
| 常量名      | 大写蛇形命名法   | `MAX_BODY_LENGTH`, `DEFAULT_TIMEOUT` |
| 布尔方法/变量  | is/has前缀  | `isActive`, `hasConnection`          |
| Handler类 | Handler后缀 | `WebSocketHandler`                   |
| Config类  | Config后缀  | `WebSocketConfig`                    |
| 常量类      | Consts后缀  | `ProtocolConsts`                     |

### 格式化规范

- **缩进**：4空格，不使用制表符
- **花括号**：与语句在同一行 `public void method() {`
- **行长度**：最大120字符
- **修饰符顺序**：`public static final`
- **getter方法**：单行 `public int getPort() { return port; }`

### 导入规范

```java
// 分组导入，空行分隔，按字母排序
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;
import io.netty.buffer.ByteBuf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

### 性能约定

1. **Zerocopy优先**：使用 `ByteBuf.slice()`, `writeBytes(src, srcIndex, length)` 避免 `duplicate()` 和 `copy()`
2. **对象池化**：Netty ByteBuf 使用 `retain()`/`release()` 管理生命周期
3. **高并发计数**：使用 `LongAdder` 而非 `AtomicLong`
4. **线程安全集合**：使用 `ConcurrentHashMap`

```java
// Zerocopy 示例：避免移动 readerIndex
buf.writeBytes(body, body.readerIndex(), bodyLength);  // 正确
buf.writeBytes(body);  // 错误：会移动 readerIndex
```

---

## 设计模式

### Builder 模式（配置类）

```java
public final class WebSocketConfig {
    private final int port;
    private final String path;
    
    private WebSocketConfig(Builder builder) {
        this.port = builder.port;
        this.path = builder.path;
    }
    
    public int getPort() { return port; }
    public String getPath() { return path; }
    
    public static final class Builder {
        private int port = 8080;
        private String path = "/ws";
        
        public Builder port(int port) { this.port = port; return this; }
        public Builder path(String path) { this.path = path; return this; }
        public WebSocketConfig build() { return new WebSocketConfig(this); }
    }
    
    public static Builder builder() { return new Builder(); }
}

// 使用
WebSocketConfig config = WebSocketConfig.builder()
    .port(8080)
    .path("/ws")
    .build();
```

### 工具类

```java
public final class PacketWriter {
    private PacketWriter() {}
    
    public static ByteBuf write(Packet packet, ByteBufAllocator alloc) { ... }
}
```

### 常量类

```java
public final class ProtocolConsts {
    public static final byte[] MAGIC = { (byte) 0xe7, (byte) 0x34 };
    public static final int FIXED_HEADER_LENGTH = 27;
    
    private ProtocolConsts() {}
}
```

---

## 协议规范

**二进制协议格式**：

```
| Magic(2B) | HeaderLen(2B) | BodyLen(4B) | Cmd(1B) | ErrCode(2B) | SrcId(8B) | TgtId(8B) | Topic(NB) | Body(NB) |
```

**命令类型**：

- `REQUEST (0x01)`: 请求
- `RESPONSE (0x02)`: 响应
- `PUSH (0x03)`: 推送
- `BROADCAST (0x04)`: 广播
- `SUBSCRIBE (0x05)`: 订阅
- `HEARTBEAT (0x06)`: 心跳

---

## 错误处理

```java
// 尽早验证参数
public void send(Packet packet) {
    Objects.requireNonNull(packet, "packet cannot be null");
    if (!packet.isValid()) {
        throw new IllegalArgumentException("Invalid packet");
    }
}

// 日志记录
private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketServer.class);
LOGGER.info("Connection: id={}, remote={}", connectionId, remoteAddress);
LOGGER.error("Decode failed: reason={}", reason, exception);
```

---

## 测试规范

### 命名约定

- 测试类：`{被测类名}Test`
- 测试方法：`test{方法名}_{场景}`

### 端口管理

测试中使用动态端口避免冲突：

```java
private static final AtomicInteger PORT_COUNTER = new AtomicInteger(30000);
int port = PORT_COUNTER.getAndIncrement();
```

---

## Git提交规范

```
<type>(<scope>): <subject>

<body>
```

**Type**: feat | fix | docs | refactor | test | chore

**示例**:

```
fix(protocol): use zerocopy in PacketWriter.write()

- Change buf.writeBytes(body) to buf.writeBytes(body, readerIndex, length)
- Prevents moving readerIndex, allows multiple clients to receive same packet
```

---

## 注意事项

1. 修改代码后需 `mvnd clean package` 重新打包 JAR
2. Builder 模式的配置类不可变，字段使用 `final`
3. 遵循约定式提交规范
4. 保持代码风格一致性