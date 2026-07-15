# 缓存设计

## 一、设计目标

- 提供通用缓存抽象
- 支持多种缓存实现（本地缓存、Guava Cache等）
- 线程安全
- 高性能
- 可配置

---

## 二、缓存接口设计

### 2.1 Cache接口

```
Cache<K, V>:
- get(K key): V
- get(K key, Function<K, V> loader): V
- put(K key, V value): void
- remove(K key): void
- containsKey(K key): boolean
- size(): int
- clear(): void
- getAll(Set<K> keys): Map<K, V>
- putAll(Map<K, V> map): void
```

### 2.2 CacheBuilder

```
CacheBuilder<K, V>:
- maximumSize(long size): CacheBuilder<K, V>
- expireAfterWrite(long duration, TimeUnit unit): CacheBuilder<K, V>
- expireAfterAccess(long duration, TimeUnit unit): CacheBuilder<K, V>
- refreshAfterWrite(long duration, TimeUnit unit): CacheBuilder<K, V>
- initialCapacity(int capacity): CacheBuilder<K, V>
- concurrencyLevel(int level): CacheBuilder<K, V>
- removalListener(RemovalListener<K, V> listener): CacheBuilder<K, V>
- build(): Cache<K, V>
- build(CacheLoader<K, V> loader): LoadingCache<K, V>
```

### 2.3 LoadingCache接口

```
LoadingCache<K, V> extends Cache<K, V>:
- get(K key): V  // 自动加载
- getAll(Set<K> keys): Map<K, V>
- refresh(K key): void
```

---

## 三、缓存实现

### 3.1 ConcurrentHashMapCache

**特点**：
- 基于ConcurrentHashMap
- 无过期策略
- 无容量限制
- 适用于简单缓存场景

**实现**：
```
ConcurrentHashMapCache<K, V> implements Cache<K, V>:
- cache: ConcurrentHashMap<K, V>

get(key):
  return cache.get(key)

put(key, value):
  cache.put(key, value)

remove(key):
  cache.remove(key)
```

**适用场景**：
- ID反向查找（ConnectionRegistry）
- Topic订阅统计
- 简单的键值缓存

### 3.2 GuavaCache

**特点**：
- 基于Guava Cache
- 支持过期策略
- 支持容量限制
- 支持自动加载
- 支持移除监听

**实现**：
```
GuavaCache<K, V> implements Cache<K, V>:
- delegate: com.google.common.cache.Cache<K, V>

get(key):
  return delegate.getIfPresent(key)

get(key, loader):
  try:
    return delegate.get(key, () -> loader.apply(key))
  catch (ExecutionException e):
    throw new RuntimeException(e)

put(key, value):
  delegate.put(key, value)
```

**适用场景**：
- 服务实例缓存
- 配置缓存
- 需要过期策略的缓存

### 3.3 LoadingGuavaCache

**特点**：
- 支持自动加载
- 支持刷新

**实现**：
```
LoadingGuavaCache<K, V> implements LoadingCache<K, V>:
- delegate: com.google.common.cache.LoadingCache<K, V>

get(key):
  try:
    return delegate.get(key)
  catch (ExecutionException e):
    throw new RuntimeException(e)
```

---

## 四、缓存配置

### 4.1 CacheConfig

```
CacheConfig:
- maximumSize: long                    // 最大容量
- initialCapacity: int                 // 初始容量
- concurrencyLevel: int                // 并发级别
- expireAfterWrite: long               // 写入后过期时间
- expireAfterAccess: long              // 访问后过期时间
- refreshAfterWrite: long              // 写入后刷新时间
- timeUnit: TimeUnit                   // 时间单位
- removalListener: RemovalListener     // 移除监听器
```

### 4.2 RemovalListener

```
RemovalListener<K, V>:
- onRemoval(RemovalNotification<K, V> notification): void

RemovalNotification<K, V>:
- key: K
- value: V
- cause: RemovalCause

RemovalCause (enum):
- EXPLICIT: 显式删除
- REPLACED: 被替换
- EXPIRED: 过期
- SIZE: 容量限制淘汰
```

---

## 五、缓存管理器

### 5.1 CacheManager

```
CacheManager:
- caches: ConcurrentMap<String, Cache<?, ?>>
- configs: ConcurrentMap<String, CacheConfig>

方法:
- <K, V> createCache(String name, CacheConfig config): Cache<K, V>
- <K, V> getCache(String name): Cache<K, V>
- destroyCache(String name): void
- destroyAll(): void
- getCacheNames(): Set<String>
```

### 5.2 使用示例

```
// 创建缓存管理器
CacheManager cacheManager = new CacheManager();

// 创建缓存配置
CacheConfig config = new CacheConfig.Builder()
    .maximumSize(10000)
    .expireAfterWrite(1, TimeUnit.HOURS)
    .build();

// 创建缓存
Cache<Long, ConnectionInfo> connectionCache = 
    cacheManager.createCache("connection", config);

// 使用缓存
connectionCache.put(connectionId, connectionInfo);
ConnectionInfo info = connectionCache.get(connectionId);

// 销毁缓存
cacheManager.destroyCache("connection");
```

---

## 六、典型应用场景

### 6.1 ConnectionRegistry缓存

**需求**：
- 存储连接ID到连接信息的映射
- 支持快速查询
- 连接关闭时移除

**实现**：
```
ConnectionRegistry:
- cache: Cache<Long, ConnectionInfo>

// 使用ConcurrentHashMapCache（无过期）
CacheConfig config = new CacheConfig.Builder()
    .initialCapacity(1024)
    .concurrencyLevel(16)
    .build();

cache = new ConcurrentHashMapCache<>(config);
```

### 6.2 ServiceInstance缓存

**需求**：
- 缓存服务实例列表
- 定期刷新
- 支持过期

**实现**：
```
ServiceInstanceCache:
- cache: LoadingCache<String, List<ServiceInstance>>

// 使用GuavaCache（支持过期和刷新）
CacheConfig config = new CacheConfig.Builder()
    .maximumSize(1000)
    .expireAfterWrite(30, TimeUnit.SECONDS)
    .refreshAfterWrite(10, TimeUnit.SECONDS)
    .build();

cache = new LoadingGuavaCache<>(config, 
    key -> registry.discover(key));
```

### 6.3 Topic统计缓存

**需求**：
- 统计Topic订阅数
- 统计Topic消息数
- 统计Topic流量

**实现**：
```
TopicStatsCache:
- subscriptionCache: Cache<String, AtomicInteger>
- messageCountCache: Cache<String, LongAdder>
- trafficCache: Cache<String, LongAdder>

// 使用ConcurrentHashMapCache
CacheConfig config = new CacheConfig.Builder()
    .initialCapacity(256)
    .build();

subscriptionCache = new ConcurrentHashMapCache<>(config);
```

---

## 七、性能优化

### 7.1 选择合适的实现

| 场景 | 推荐实现 | 原因 |
|------|----------|------|
| 简单键值存储，无过期 | ConcurrentHashMapCache | 性能最高，无额外开销 |
| 需要过期策略 | GuavaCache | 内置过期机制 |
| 需要自动加载 | LoadingGuavaCache | 支持自动加载和刷新 |
| 高并发写入 | ConcurrentHashMapCache | 无锁读取，CAS写入 |

### 7.2 配置优化

**初始容量**：
- 预估容量大小
- 避免频繁扩容
- 建议：预估容量 / 0.75

**并发级别**：
- 与CPU核心数相关
- 建议：CPU核心数 * 2

**容量限制**：
- 根据内存大小设置
- 避免OOM

### 7.3 移除策略

**过期移除**：
- expireAfterWrite：写入后过期
- expireAfterAccess：访问后过期

**容量移除**：
- LRU策略
- 近期最少使用

---

## 八、监控统计

### 8.1 CacheStats

```
CacheStats:
- hitCount: long           // 命中次数
- missCount: long          // 未命中次数
- loadSuccessCount: long   // 加载成功次数
- loadExceptionCount: long // 加载异常次数
- totalLoadTime: long      // 总加载时间
- evictionCount: long      // 淘汰次数

方法:
- hitRate(): double        // 命中率
- averageLoadPenalty(): double  // 平均加载时间
```

### 8.2 统计接口

```
Cache:
- stats(): CacheStats
- recordStats(): void      // 开启统计
```

---

## 九、配置项

```properties
# 缓存配置
jwsch.cache.enabled=true
jwsch.cache.default.maximum.size=10000
jwsch.cache.default.expire.after.write=3600
jwsch.cache.default.expire.after.access=3600
jwsch.cache.default.concurrency.level=16

# Connection缓存配置
jwsch.cache.connection.initial.capacity=1024
jwsch.cache.concurrency.level=16

# ServiceInstance缓存配置
jwsch.cache.service.maximum.size=1000
jwsch.cache.service.expire.after.write=30
jwsch.cache.service.refresh.after.write=10
```

---

## 十、包结构

```
cn.itcraft.jwsch.common.cache
├── Cache.java                     # 缓存接口
├── LoadingCache.java              # 自动加载缓存接口
├── CacheBuilder.java              # 缓存构建器
├── CacheConfig.java               # 缓存配置
├── CacheManager.java              # 缓存管理器
├── CacheStats.java                # 缓存统计
├── RemovalListener.java           # 移除监听器
├── RemovalNotification.java       # 移除通知
├── RemovalCause.java              # 移除原因
├── impl/
│   ├── ConcurrentHashMapCache.java
│   ├── GuavaCache.java
│   └── LoadingGuavaCache.java
└── exception/
    └── CacheException.java
```

---