# 测试设计

## 一、测试策略

### 1.1 测试层次

| 测试类型 | 说明 | 执行时机 |
|----------|------|----------|
| 单元测试 | 测试单个类/方法 | 每次提交 |
| 性能测试 | JMH基准测试 | 每日/版本发布前 |
| 连通性测试 | 端到端集成测试 | 每日/版本发布前 |

### 1.2 测试覆盖率要求

| 模块 | 行覆盖率 | 分支覆盖率 |
|------|----------|------------|
| jwsch-common | ≥80% | ≥70% |
| jwsch-cli | ≥75% | ≥65% |
| jwsch-srv | ≥70% | ≥60% |

---

## 二、单元测试

### 2.1 测试框架

- **JUnit 4.13.2**：测试框架
- **Mockito 3.12.4**：Mock框架
- **AssertJ 3.23.1**：断言库（可选）

### 2.2 Maven依赖

```xml
<dependencies>
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>3.12.4</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 2.3 测试命名规范

```
测试类命名: {被测类名}Test
测试方法命名: test{方法名}_{场景描述}

示例:
- PacketEncoderTest
- testEncode_normalPacket
- testEncode_nullPacket_throwsException
```

### 2.4 测试分类

#### 2.4.1 协议模块测试

```
PacketEncoderTest:
- testEncode_normalPacket
- testEncode_packetWithTopic
- testEncode_packetWithNullBody
- testEncode_packetExceedsMaxLength

PacketDecoderTest:
- testDecode_normalPacket
- testDecode_invalidMagic
- testDecode_invalidHeaderLength
- testDecode_incompletePacket
- testDecode_packetWithTopic

PacketHeaderTest:
- testBuilder_normalCase
- testBuilder_invalidMagic
- testIsValid_validHeader
- testIsValid_invalidHeader
```

#### 2.4.2 ID生成测试

```
IdGeneratorTest:
- testGenerateFrontendId_ipv4
- testGenerateFrontendId_ipv6
- testGenerateBackendId_normal
- testGenerateNodeId_normal
- testGenerateId_sameInput_sameOutput
- testGenerateId_differentInput_differentOutput
```

#### 2.4.3 错误码测试

```
ErrorCodeTest:
- testGetCode_success
- testGetDesc_success
- testAllErrorCodes_validRange
- testValueOf_validCode
```

#### 2.4.4 缓存测试

```
ConcurrentHashMapCacheTest:
- testGet_normalCase
- testPut_normalCase
- testRemove_normalCase
- testContainsKey_true
- testContainsKey_false
- testSize_emptyCache
- testClear_normalCase
- testConcurrentAccess_multiThread

GuavaCacheTest:
- testGet_normalCase
- testGet_withLoader
- testExpireAfterWrite
- testExpireAfterAccess
- testMaximumSize_eviction
```

#### 2.4.5 连接管理测试

```
ConnectionRegistryTest:
- testRegister_normalCase
- testUnregister_normalCase
- testLookup_localConnection
- testLookup_remoteConnection
- testLookupByRemoteAddress_normalCase

WebSocketConnectionManagerTest:
- testAddConnection_normalCase
- testRemoveConnection_normalCase
- testGetConnection_existing
- testGetConnection_nonExisting
- testBroadcast_normalCase
```

#### 2.4.6 负载均衡测试

```
RandomLoadBalanceTest:
- testSelect_normalCase
- testSelect_emptyList_throwsException
- testSelect_distribution

RoundRobinLoadBalanceTest:
- testSelect_normalCase
- testSelect_roundRobin
- testSelect_emptyList_throwsException

ConsistentHashLoadBalanceTest:
- testSelect_normalCase
- testSelect_sameKey_sameInstance
- testSelect_differentKey_distribution
- testNodeChange_minimalImpact
```

#### 2.4.7 注册中心测试

```
InMemoryServiceRegistryTest:
- testRegister_normalCase
- testDeregister_normalCase
- testDiscover_normalCase
- testSubscribe_normalCase
- testSubscribe_serviceChange
```

### 2.5 测试示例

```java
public class PacketEncoderTest {
    
    @Test
    public void testEncode_normalPacket() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.REQUEST)
            .errorCode(ErrorCode.SUCCESS.getCode())
            .sourceId(12345L)
            .targetId(67890L)
            .build();
        
        byte[] body = "test body".getBytes(StandardCharsets.UTF_8);
        Packet packet = new Packet(header, body);
        
        ByteBuf out = Unpooled.buffer();
        PacketEncoder encoder = new PacketEncoder();
        
        encoder.encode(null, packet, out);
        
        assertEquals(ProtocolConsts.FIXED_HEADER_LENGTH + body.length, out.readableBytes());
    }
    
    @Test(expected = NullPointerException.class)
    public void testEncode_nullPacket_throwsException() {
        PacketEncoder encoder = new PacketEncoder();
        ByteBuf out = Unpooled.buffer();
        
        encoder.encode(null, null, out);
    }
}
```

---

## 三、JMH性能测试

### 3.1 JMH简介

JMH（Java Microbenchmark Harness）是OpenJDK提供的Java微基准测试工具。

### 3.2 Maven依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.openjdk.jmh</groupId>
        <artifactId>jmh-core</artifactId>
        <version>1.36</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.openjdk.jmh</groupId>
        <artifactId>jmh-generator-annprocess</artifactId>
        <version>1.36</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 3.3 性能测试场景

#### 3.3.1 协议编解码性能

```
PacketEncoderBenchmark:
- benchmarkEncode_normalPacket
- benchmarkEncode_packetWithTopic
- benchmarkEncode_packetWithLargeBody

PacketDecoderBenchmark:
- benchmarkDecode_normalPacket
- benchmarkDecode_packetWithTopic
- benchmarkDecode_packetWithLargeBody
```

**测试目标**：
- 编码吞吐量：≥100万次/秒
- 解码吞吐量：≥100万次/秒

#### 3.3.2 ID生成性能

```
IdGeneratorBenchmark:
- benchmarkGenerateFrontendId
- benchmarkGenerateBackendId
- benchmarkGenerateNodeId
```

**测试目标**：
- 生成吞吐量：≥1000万次/秒

#### 3.3.3 缓存性能

```
ConcurrentHashMapCacheBenchmark:
- benchmarkGet_singleThread
- benchmarkPut_singleThread
- benchmarkGet_multiThread
- benchmarkPut_multiThread
- benchmarkMixed_multiThread

GuavaCacheBenchmark:
- benchmarkGet_withLoader
- benchmarkGet_cached
- benchmarkPut_normalCase
```

**测试目标**：
- Get吞吐量：≥1000万次/秒
- Put吞吐量：≥500万次/秒

#### 3.3.4 负载均衡性能

```
LoadBalanceBenchmark:
- benchmarkRandomLoadBalance
- benchmarkRoundRobinLoadBalance
- benchmarkConsistentHashLoadBalance
```

**测试目标**：
- 选择吞吐量：≥1000万次/秒

#### 3.3.5 连接管理性能

```
ConnectionRegistryBenchmark:
- benchmarkRegister
- benchmarkUnregister
- benchmarkLookup
- benchmarkLookupByRemoteAddress
```

**测试目标**：
- 查找吞吐量：≥1000万次/秒

### 3.4 JMH测试示例

```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class PacketEncoderBenchmark {
    
    private PacketEncoder encoder;
    private Packet packet;
    private ByteBuf buffer;
    
    @Setup
    public void setup() {
        encoder = new PacketEncoder();
        
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.REQUEST)
            .errorCode(ErrorCode.SUCCESS.getCode())
            .sourceId(12345L)
            .targetId(67890L)
            .build();
        
        byte[] body = new byte[1024];
        packet = new Packet(header, body);
    }
    
    @Benchmark
    public void benchmarkEncode_normalPacket() {
        buffer = Unpooled.buffer();
        encoder.encode(null, packet, buffer);
        buffer.release();
    }
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(PacketEncoderBenchmark.class.getSimpleName())
            .forks(1)
            .warmupIterations(5)
            .measurementIterations(10)
            .build();
        
        new Runner(opt).run();
    }
}
```

### 3.5 运行JMH测试

```bash
# 运行所有性能测试
mvn test -Pjmh

# 运行指定测试
mvn test -Pjmh -Dtest=PacketEncoderBenchmark
```

---

## 四、连通性测试

### 4.1 测试架构

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│ WebSocket   │  WS     │   jwsch     │   TCP   │ TCP Server  │
│ Mock Client │◄───────►│   Server    │◄───────►│ Mock Server │
└─────────────┘         └─────────────┘         └─────────────┘
```

### 4.2 WebSocket Mock Client

#### 4.2.1 依赖

```xml
<dependency>
    <groupId>org.java-websocket</groupId>
    <artifactId>Java-WebSocket</artifactId>
    <version>1.5.3</version>
    <scope>test</scope>
</dependency>
```

#### 4.2.2 实现

```java
public class WebSocketMockClient extends WebSocketClient {
    
    private final BlockingQueue<Packet> receivedPackets = new LinkedBlockingQueue<>();
    
    public WebSocketMockClient(String serverUri) {
        super(URI.create(serverUri));
    }
    
    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("WebSocket connected");
    }
    
    @Override
    public void onMessage(String message) {
        // 处理文本消息
    }
    
    @Override
    public void onMessage(ByteBuffer bytes) {
        // 处理二进制消息
        Packet packet = decodePacket(bytes.array());
        receivedPackets.offer(packet);
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("WebSocket closed: " + reason);
    }
    
    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
    
    public void sendPacket(Packet packet) {
        byte[] data = encodePacket(packet);
        this.send(data);
    }
    
    public Packet receivePacket(long timeout, TimeUnit unit) throws InterruptedException {
        return receivedPackets.poll(timeout, unit);
    }
}
```

### 4.3 TCP Mock Server

#### 4.3.1 实现

```java
public class TcpMockServer {
    
    private final int port;
    private ServerSocket serverSocket;
    private final List<Socket> clients = new CopyOnWriteArrayList<>();
    
    public TcpMockServer(int port) {
        this.port = port;
    }
    
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket client = serverSocket.accept();
                    clients.add(client);
                    new Thread(() -> handleClient(client)).start();
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    
    private void handleClient(Socket client) {
        try {
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
            
            while (!client.isClosed()) {
                // 读取请求
                Packet request = readPacket(in);
                
                // 处理并返回响应
                Packet response = handleRequest(request);
                writePacket(out, response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            clients.remove(client);
        }
    }
    
    public void stop() throws IOException {
        for (Socket client : clients) {
            client.close();
        }
        serverSocket.close();
    }
}
```

### 4.4 连通性测试用例

#### 4.4.1 WebSocket连接测试

```
WebSocketConnectionTest:
- testConnect_normalCase
- testConnect_invalidPort
- testConnect_maxConnections
- testDisconnect_normalCase
- testReconnect_afterDisconnect
```

#### 4.4.2 消息转发测试

```
MessageForwardTest:
- testRequestResponse_normalCase
- testRequestResponse_timeout
- testBroadcast_normalCase
- testPush_normalCase
- testSubscribe_normalCase
```

#### 4.4.3 端到端测试

```
EndToEndTest:
- testWebSocketToTcp_normalCase
- testBroadcast_multiClient
- testClusterForward_multiNode
```

### 4.5 连通性测试示例

```java
public class EndToEndTest {
    
    private JwschServer jwschServer;
    private TcpMockServer tcpMockServer;
    private WebSocketMockClient wsClient;
    
    @Before
    public void setup() throws Exception {
        // 启动TCP Mock Server
        tcpMockServer = new TcpMockServer(9090);
        tcpMockServer.start();
        
        // 启动jwsch服务
        JwschConfig config = new JwschConfig();
        config.getWebsocketConfig().setPort(8080);
        jwschServer = new JwschServer(config);
        jwschServer.start();
        
        // 启动WebSocket Mock Client
        wsClient = new WebSocketMockClient("ws://localhost:8080/ws");
        wsClient.connect();
        Thread.sleep(1000);  // 等待连接建立
    }
    
    @After
    public void teardown() throws Exception {
        wsClient.close();
        jwschServer.shutdown();
        tcpMockServer.stop();
    }
    
    @Test
    public void testWebSocketToTcp_normalCase() throws Exception {
        // 构造请求
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.REQUEST)
            .sourceId(wsClient.getConnectionId())
            .targetId(0)  // 由路由器决定
            .topic("/api/user/get")
            .build();
        
        byte[] body = "{\"userId\":100}".getBytes(StandardCharsets.UTF_8);
        Packet request = new Packet(header, body);
        
        // 发送请求
        wsClient.sendPacket(request);
        
        // 等待响应
        Packet response = wsClient.receivePacket(5, TimeUnit.SECONDS);
        
        assertNotNull(response);
        assertEquals(Command.RESPONSE, response.getCommand());
        assertEquals(ErrorCode.SUCCESS.getCode(), response.getHeader().getErrorCode());
    }
    
    @Test
    public void testBroadcast_multiClient() throws Exception {
        // 创建多个客户端
        List<WebSocketMockClient> clients = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            WebSocketMockClient client = new WebSocketMockClient("ws://localhost:8080/ws");
            client.connect();
            clients.add(client);
        }
        Thread.sleep(1000);
        
        // 发送广播
        Packet broadcast = new Packet.Builder()
            .command(Command.BROADCAST)
            .topic("/broadcast/test")
            .body("test message".getBytes())
            .build();
        
        clients.get(0).sendPacket(broadcast);
        
        // 验证所有客户端都收到消息
        for (WebSocketMockClient client : clients) {
            Packet received = client.receivePacket(2, TimeUnit.SECONDS);
            assertNotNull(received);
        }
        
        // 清理
        for (WebSocketMockClient client : clients) {
            client.close();
        }
    }
}
```

---

## 五、测试执行

### 5.1 Maven命令

```bash
# 运行单元测试
mvn test

# 运行指定测试
mvn test -Dtest=PacketEncoderTest

# 运行性能测试
mvn test -Pjmh

# 生成测试覆盖率报告
mvn test jacoco:report
```

### 5.2 CI/CD集成

```yaml
# .gitlab-ci.yml
test:
  stage: test
  script:
    - mvn test
    - mvn test -Pjmh
  artifacts:
    reports:
      junit: target/surefire-reports/*.xml
    paths:
      - target/site/jacoco/
```

---

## 六、测试报告

### 6.1 单元测试报告

- 位置：`target/surefire-reports/`
- 格式：XML/HTML
- 内容：测试用例数、通过数、失败数、跳过数

### 6.2 覆盖率报告

- 位置：`target/site/jacoco/`
- 格式：HTML
- 内容：行覆盖率、分支覆盖率、类覆盖率

### 6.3 性能测试报告

- 位置：`target/jmh-results/`
- 格式：JSON/CSV
- 内容：吞吐量、平均时间、P95/P99

---

## 七、测试包结构

```
src/test/java/cn/itcraft/jwsch
├── common/
│   ├── protocol/
│   │   ├── PacketEncoderTest.java
│   │   ├── PacketDecoderTest.java
│   │   └── PacketHeaderTest.java
│   ├── id/
│   │   └── IdGeneratorTest.java
│   ├── cache/
│   │   ├── ConcurrentHashMapCacheTest.java
│   │   └── GuavaCacheTest.java
│   └── exception/
│       └── ErrorCodeTest.java
├── srv/
│   ├── connection/
│   │   └── ConnectionRegistryTest.java
│   ├── loadbalance/
│   │   ├── RandomLoadBalanceTest.java
│   │   ├── RoundRobinLoadBalanceTest.java
│   │   └── ConsistentHashLoadBalanceTest.java
│   └── registry/
│       └── InMemoryServiceRegistryTest.java
├── integration/
│   ├── WebSocketConnectionTest.java
│   ├── MessageForwardTest.java
│   └── EndToEndTest.java
├── mock/
│   ├── WebSocketMockClient.java
│   └── TcpMockServer.java
└── jmh/
    ├── PacketEncoderBenchmark.java
    ├── PacketDecoderBenchmark.java
    ├── IdGeneratorBenchmark.java
    ├── ConcurrentHashMapCacheBenchmark.java
    └── LoadBalanceBenchmark.java
```

---