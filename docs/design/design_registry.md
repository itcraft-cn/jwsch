# 服务注册中心设计

## 一、设计目标

- 服务注册与发现
- 支持多种实现（内存、Nacos、ZooKeeper）
- 可插拔设计（SPI）
- 支持服务变更监听

---

## 二、接口定义

### 2.1 ServiceRegistry接口

```
ServiceRegistry:
- register(ServiceInstance instance): void
- deregister(String serviceName, String instanceId): void
- discover(String serviceName): List<ServiceInstance>
- subscribe(String serviceName, ServiceChangeListener listener): void
- unsubscribe(String serviceName, ServiceChangeListener listener): void
- shutdown(): void
```

### 2.2 ServiceChangeListener接口

```
ServiceChangeListener:
- onServiceChange(String serviceName, List<ServiceInstance> instances): void
```

---

## 三、实例模型

### 3.1 ServiceInstance

```
ServiceInstance:
- serviceName: String          # 服务名
- instanceId: String           # 实例ID
- host: String                 # 主机地址
- port: int                    # 端口
- metadata: Map<String,String> # 元数据
- weight: int                  # 权重
- status: InstanceStatus       # 状态

InstanceStatus:
- UP: 正常
- DOWN: 下线
```

---

## 四、内存注册中心实现

### 4.1 InMemoryServiceRegistry

**适用场景**：单机部署、开发测试

**实现**：
```
InMemoryServiceRegistry:
- serviceMap: ConcurrentMap<String, List<ServiceInstance>>
- listeners: ConcurrentMap<String, List<ServiceChangeListener>>
```

---

## 五、扩展实现（Phase 2+）

### 5.1 Nacos注册中心

### 5.2 ZooKeeper注册中心

---

## 六、配置项

```properties
jwsch.registry.type=MEMORY
```

---