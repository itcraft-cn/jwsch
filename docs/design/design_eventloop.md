# EventLoop设计

## 一、设计目标

### 1.1 服务端EventLoop

- 单一EventLoopGroup集合
- 跟随服务端启动/关闭
- 管理WebSocket连接IO

### 1.2 客户端EventLoop

- 支持多个JwschClient实例
- 默认共享EventLoop
- 支持独立EventLoop

---

## 二、服务端EventLoop设计

### 2.1 架构

```
JwschServer:
- bossGroup: EventLoopGroup (1线程)
- workerGroup: EventLoopGroup (N线程)
```

### 2.2 职责

| EventLoopGroup | 线程数 | 职责 |
|----------------|--------|------|
| bossGroup | 1 | 处理连接请求（accept） |
| workerGroup | N | 处理WebSocket连接的IO事件 |

### 2.3 生命周期

```
启动:
- 创建bossGroup和workerGroup
- 绑定端口，开始监听

运行:
- 处理连接、读写事件

关闭:
- shutdownGracefully()
- 等待所有任务完成
```

### 2.4 配置

```properties
jwsch.srv.websocket.boss.threads=1
jwsch.srv.websocket.worker.threads=8
```

---

## 三、客户端EventLoop设计

### 3.1 设计目标

- 支持多个JwschClient实例
- 默认共享EventLoop（减少资源占用）
- 支持独立EventLoop（特殊场景隔离）

### 3.2 架构设计

```
SharedEventLoopManager (单例)
├── sharedWorkerGroup: EventLoopGroup
└── refCount: AtomicInteger

JwschClient:
├── workerGroup: EventLoopGroup
│   ├── shared=true: 引用 sharedWorkerGroup
│   └── shared=false: 独立 workerGroup
└── config: ClientConfig
    └── sharedEventLoop: boolean
```

### 3.3 EventLoopHolder

```
EventLoopHolder:
- eventLoopGroup: EventLoopGroup
- shared: boolean
- referenceCount: AtomicInteger

方法:
- getEventLoopGroup(): EventLoopGroup
- isShared(): boolean
- retain(): void        // 增加引用计数
- release(): boolean    // 减少引用计数，返回是否可销毁
```

### 3.4 SharedEventLoopManager

```
SharedEventLoopManager (单例):
- sharedWorkerGroup: EventLoopGroup
- refCount: AtomicInteger
- lock: Object
- config: EventLoopConfig

方法:
- acquire(): EventLoopGroup    // 获取共享EventLoop
- release(): void              // 释放引用
- isInitialized(): boolean     // 是否已初始化
```

---

## 四、生命周期管理

### 4.1 共享模式

**流程**：
```
Client1.start():
  workerGroup = SharedEventLoopManager.acquire()
  // 创建sharedWorkerGroup，refCount=1

Client2.start():
  workerGroup = SharedEventLoopManager.acquire()
  // 复用sharedWorkerGroup，refCount=2

Client1.shutdown():
  SharedEventLoopManager.release()
  // refCount=1，不销毁

Client2.shutdown():
  SharedEventLoopManager.release()
  // refCount=0，销毁sharedWorkerGroup
```

**实现**：
```
acquire():
  synchronized(lock):
    if (sharedWorkerGroup == null):
      sharedWorkerGroup = new NioEventLoopGroup(threads)
    refCount.incrementAndGet()
    return sharedWorkerGroup

release():
  synchronized(lock):
    if (refCount.decrementAndGet() == 0):
      sharedWorkerGroup.shutdownGracefully()
      sharedWorkerGroup = null
```

### 4.2 独立模式

**流程**：
```
Client.start():
  workerGroup = new NioEventLoopGroup(threads)
  // 创建独立workerGroup

Client.shutdown():
  workerGroup.shutdownGracefully()
  // 销毁自己的workerGroup
```

---

## 五、配置设计

### 5.1 配置项

```properties
# 客户端EventLoop配置
jwsch.cli.eventloop.shared=true
jwsch.cli.eventloop.worker.threads=8
```

### 5.2 配置类

```
EventLoopConfig:
- shared: boolean
- workerThreads: int

ClientConfig:
- eventLoopConfig: EventLoopConfig
- tcpConfig: TcpClientConfig
```

---

## 六、使用示例

### 6.1 共享EventLoop（推荐）

```java
ClientConfig config1 = new ClientConfig();
config1.getEventLoopConfig().setShared(true);

ClientConfig config2 = new ClientConfig();
config2.getEventLoopConfig().setShared(true);

JwschClient client1 = new JwschClient(config1);
JwschClient client2 = new JwschClient(config2);

client1.start();  // 创建sharedWorkerGroup
client2.start();  // 复用sharedWorkerGroup

client1.shutdown();  // 不销毁EventLoop
client2.shutdown();  // 最后一个关闭时销毁EventLoop
```

### 6.2 独立EventLoop

```java
ClientConfig config = new ClientConfig();
config.getEventLoopConfig().setShared(false);
config.getEventLoopConfig().setWorkerThreads(4);

JwschClient client = new JwschClient(config);
client.start();   // 创建独立的workerGroup
client.shutdown(); // 销毁自己的workerGroup
```

---

## 七、线程模型

### 7.1 服务端线程模型

```
Boss Thread (1个):
- 监听连接请求
- 将连接注册到Worker

Worker Threads (N个):
- 处理连接IO事件
- 执行ChannelHandler
- 处理业务逻辑
```

### 7.2 客户端线程模型

```
Worker Threads (N个):
- 处理连接IO事件
- 执行ChannelHandler
- 处理响应回调
```

### 7.3 线程数建议

| 场景 | Boss线程 | Worker线程 |
|------|----------|------------|
| 低并发 | 1 | CPU核心数 |
| 中并发 | 1 | CPU核心数 * 2 |
| 高并发 | 1 | CPU核心数 * 2 ~ 4 |

---

## 八、资源管理

### 8.1 资源计数

**SharedEventLoopManager**维护引用计数：
```
refCount:
- 0: 未初始化或已销毁
- 1: 1个客户端使用
- N: N个客户端共享
```

### 8.2 优雅关闭

```
shutdown():
  if (shared):
    SharedEventLoopManager.release()
  else:
    workerGroup.shutdownGracefully(100, 300, TimeUnit.MILLISECONDS)
    // 安静期100ms，超时300ms
```

### 8.3 资源泄漏防护

- 使用`ReferenceCountUtil.releaseLater()`测试
- 启用内存泄漏检测
- 单元测试覆盖生命周期场景

---

## 九、最佳实践

### 9.1 推荐配置

**大部分场景**：共享EventLoop
```properties
jwsch.cli.eventloop.shared=true
jwsch.cli.eventloop.worker.threads=8
```

**特殊场景**：独立EventLoop
- 高优先级客户端
- 隔离故障影响
- 独立资源限制

### 9.2 注意事项

1. **共享模式下，任何一个客户端异常不会影响其他客户端**
2. **独立模式下，资源占用更多，但隔离性更好**
3. **关闭顺序：先关闭所有客户端，再关闭SharedEventLoopManager**
4. **避免在ChannelHandler中执行阻塞操作**