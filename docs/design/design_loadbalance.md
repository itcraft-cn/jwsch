# 负载均衡设计

## 一、设计目标

- 支持多种负载均衡策略
- 支持会话粘性（一致性哈希）
- 支持故障转移
- 策略可扩展

---

## 二、策略接口

### 2.1 LoadBalance接口

```
LoadBalance:
- select(List<ServiceInstance> instances, String key): ServiceInstance
```

**参数说明**：
- `instances`：可用实例列表
- `key`：路由键（可选，用于一致性哈希）

**返回值**：
- 选中的服务实例

---

## 三、实现策略

### 3.1 随机负载均衡（RandomLoadBalance）

**策略**：随机选择一个实例

**优点**：
- 简单高效
- 无状态

**缺点**：
- 无法保证均匀分布
- 不支持会话粘性

**实现**：
```
RandomLoadBalance:
- random: Random

select(instances, key):
  int index = random.nextInt(instances.size())
  return instances.get(index)
```

**适用场景**：
- 实例性能相近
- 无状态服务

### 3.2 轮询负载均衡（RoundRobinLoadBalance）

**策略**：按顺序依次选择实例

**优点**：
- 公平分配
- 实现简单

**缺点**：
- 不考虑实例性能差异
- 不支持会话粘性

**实现**：
```
RoundRobinLoadBalance:
- counters: ConcurrentMap<String, AtomicLong>

select(instances, key):
  AtomicLong counter = counters.computeIfAbsent(key, k -> new AtomicLong(0))
  long index = counter.getAndIncrement() % instances.size()
  return instances.get((int) index)
```

**适用场景**：
- 实例性能相近
- 需要公平分配

### 3.3 一致性哈希负载均衡（ConsistentHashLoadBalance）

**策略**：根据key计算哈希，选择最近的实例

**优点**：
- 支持会话粘性
- 相同key路由到相同实例
- 节点变化时影响最小

**缺点**：
- 实现复杂
- 需要维护虚拟节点

**实现**：
```
ConsistentHashLoadBalance:
- virtualNodes: TreeMap<Long, ServiceInstance>
- virtualNodeCount: int (默认160)

select(instances, key):
  long hash = hash(key)
  Entry<Long, ServiceInstance> entry = virtualNodes.ceilingEntry(hash)
  if (entry == null):
    entry = virtualNodes.firstEntry()
  return entry.getValue()
```

**虚拟节点**：
- 每个实例创建N个虚拟节点
- 虚拟节点名称：`instanceId#i`
- 提高分布均匀性

**适用场景**：
- 需要会话粘性
- 有状态服务
- 缓存服务

### 3.4 加权轮询负载均衡（WeightedRoundRobinLoadBalance）

**策略**：按权重轮询选择实例

**优点**：
- 考虑实例性能差异
- 更合理的分配

**缺点**：
- 实现复杂
- 需要维护权重状态

**实现**：
```
WeightedRoundRobinLoadBalance:
- weightCounters: ConcurrentMap<String, WeightedCounter>

select(instances, key):
  计算总权重
  选择当前权重最大的实例
  更新权重计数
```

**适用场景**：
- 实例性能差异大
- 需要按权重分配

---

## 四、故障转移

### 4.1 故障检测

**失败计数**：
```
ServiceInstance:
- failCount: AtomicInteger
- lastFailTime: long
- status: InstanceStatus (UP/DOWN)
```

**判定规则**：
- 连续失败N次（默认5次）标记为DOWN
- M秒后（默认30秒）尝试恢复

### 4.2 重试机制

**配置项**：
```properties
jwsch.loadbalance.retry.times=3
jwsch.loadbalance.retry.interval=100  # 重试间隔（毫秒）
```

**重试策略**：
```
RetryPolicy:
- maxRetries: int
- retryInterval: int

execute(instance, request):
  for (i = 0; i < maxRetries; i++):
    try:
      return sendRequest(instance, request)
    catch (Exception e):
      if (i == maxRetries - 1):
        throw e
      Thread.sleep(retryInterval)
      instance = selectAnotherInstance()
```

### 4.3 故障节点剔除

**剔除规则**：
- 标记为DOWN的实例不参与负载均衡
- 定时检查DOWN状态实例
- 恢复后重新加入可用列表

**实现**：
```
FaultTolerantLoadBalance:
- instances: List<ServiceInstance>
- downInstances: List<ServiceInstance>
- scheduler: ScheduledExecutorService

select(instances, key):
  过滤掉DOWN状态的实例
  从可用实例中选择

checkDownInstances():
  定时检查DOWN实例
  尝试恢复
```

---

## 五、配置设计

### 5.1 配置项

```properties
# 负载均衡配置
jwsch.loadbalance.strategy=ROUND_ROBIN
jwsch.loadbalance.retry.times=3
jwsch.loadbalance.retry.interval=100
jwsch.loadbalance.failure.threshold=5
jwsch.loadbalance.recovery.interval=30
```

### 5.2 配置类

```
LoadBalanceConfig:
- strategy: LoadBalanceStrategy (RANDOM/ROUND_ROBIN/CONSISTENT_HASH/WEIGHTED_ROUND_ROBIN)
- retryTimes: int
- retryInterval: int
- failureThreshold: int
- recoveryInterval: int
```

---

## 六、策略选择

### 6.1 选择依据

| 场景 | 推荐策略 |
|------|----------|
| 无状态服务，实例性能相近 | 轮询 |
| 无状态服务，需要简单高效 | 随机 |
| 有状态服务，需要会话粘性 | 一致性哈希 |
| 实例性能差异大 | 加权轮询 |

### 6.2 性能对比

| 策略 | 时间复杂度 | 空间复杂度 |
|------|------------|------------|
| 随机 | O(1) | O(1) |
| 轮询 | O(1) | O(N) |
| 一致性哈希 | O(log N) | O(N * virtualNodeCount) |
| 加权轮询 | O(N) | O(N) |

---

## 七、扩展性设计

### 7.1 SPI扩展

**接口定义**：
```
LoadBalanceFactory:
- create(LoadBalanceConfig config): LoadBalance
```

**实现方式**：
```
ServiceLoader<LoadBalanceFactory> loader = ServiceLoader.load(LoadBalanceFactory.class)
```

### 7.2 自定义策略

**步骤**：
1. 实现 `LoadBalance` 接口
2. 实现 `LoadBalanceFactory` 接口
3. 在 META-INF/services 注册

**示例**：
```
public class CustomLoadBalance implements LoadBalance {
    @Override
    public ServiceInstance select(List<ServiceInstance> instances, String key) {
        // 自定义逻辑
    }
}
```