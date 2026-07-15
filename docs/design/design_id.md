# ID生成策略设计

## 一、设计目标

将三种输入源统一映射为 `long` 类型ID：
1. 前端连接：`IPv4:port` 或 `IPv6:port`
2. 后端连接：`IPv4:port` 或 `IPv6:port`
3. 节点标识：`配置前缀 + hostname`

---

## 二、输入标准化

### 2.1 IPv4:port

**格式**：`xxx.xxx.xxx.xxx:pppp`

**示例**：
- `192.168.1.100:8080`
- `10.0.0.1:9000`

**标准化规则**：
- IPv4地址保持原格式
- 端口用冒号分隔
- 输出字符串长度：7-21字符

### 2.2 IPv6:port

**格式**：`[xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx]:pppp`

**示例**：
- `[2001:db8:85a3::8a2e:370:7334]:8080`
- `[::1]:9000`

**标准化规则**：
- IPv6地址用方括号包裹
- 端口用冒号分隔
- 输出字符串长度：6-51字符

### 2.3 节点标识

**格式**：`prefix-hostname`

**示例**：
- `10-node-01`
- `prod-server-192`

**标准化规则**：
- 前缀和hostname用短横线连接
- 前缀**必填**
- hostname通过Java API获取

**获取hostname**：
```
String hostname = InetAddress.getLocalHost().getHostName();
```

---

## 三、Hash算法选择

### 3.1 选择：MurmurHash3

**选择理由**：
- 高性能：每秒可处理GB级数据
- 均匀分布：输出随机性好，冲突率低
- 无需复杂依赖：Google Guava提供实现
- 非加密：适合ID生成场景，不涉及安全

### 3.2 算法特性

| 特性 | 说明 |
|------|------|
| 输入 | 任意长度字节数组 |
| 输出 | 128位或64位hash值 |
| 种子 | 支持自定义种子，增加随机性 |
| 性能 | GB级/秒 |

### 3.3 64位变体

**说明**：使用64位输出版本，直接生成 `long` 类型ID

**参数**：
- 种子：可配置，默认 `0x1234ABCD`
- 输入：标准化字符串的UTF-8字节数组
- 输出：64位 `long` 类型hash值

---

## 四、ID结构方案

### 4.1 选择：纯Hash值

**结构**：
```
64位ID = MurmurHash3_64(标准化输入字符串)
```

**优点**：
- 简单直接
- 均匀分布
- 无结构解析开销

### 4.2 冲突概率分析

**MurmurHash3冲突概率**：

| ID数量 | 冲突概率 |
|--------|----------|
| 1万 | 约 0.00000000000003% |
| 10万 | 约 0.000000000003% |
| 100万 | 约 0.000000000003% |

**结论**：冲突概率极低，可忽略不计

---

## 五、ID生成器设计

### 5.1 职责

1. 输入标准化：将三种输入转成统一字符串格式
2. Hash计算：调用MurmurHash3生成64位hash
3. ID返回：返回最终ID

### 5.2 接口定义

```
IdGenerator:
- generateId(String input): long
- generateFrontendId(String ip, int port): long
- generateBackendId(String ip, int port): long
- generateNodeId(String prefix, String hostname): long
```

### 5.3 使用示例

#### 前端连接ID

```
String ip = "192.168.1.100";
int port = 8080;
long connectionId = idGenerator.generateFrontendId(ip, port);
// 内部: MurmurHash3("192.168.1.100:8080")
```

#### 后端连接ID

```
String ip = "10.0.0.1";
int port = 9000;
long connectionId = idGenerator.generateBackendId(ip, port);
// 内部: MurmurHash3("10.0.0.1:9000")
```

#### 节点ID

```
String prefix = "10";  // 从配置读取
String hostname = InetAddress.getLocalHost().getHostName(); // "node-01"
long nodeId = idGenerator.generateNodeId(prefix, hostname);
// 内部: MurmurHash3("10-node-01")
```

---

## 六、启动校验

### 6.1 配置必填项

**配置项**：
```properties
jwsch.node.prefix=10  # 必填
```

### 6.2 启动时校验

- 启动时检查 `jwsch.node.prefix` 是否配置
- 未配置则启动失败，抛出 `ConfigException`

---

## 七、配置项

```properties
# ID生成配置
jwsch.id.hash.seed=0x1234ABCD
jwsch.node.prefix=10  # 必填
```

---

## 八、依赖

**Maven依赖**：
```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>31.1-jre</version>
</dependency>
```

**使用类**：
```
com.google.common.hash.Hashing.murmur3_128(seed)
```

---

## 九、性能考虑

### 9.1 计算性能

- MurmurHash3：每秒可处理GB级数据
- 单次计算：纳秒级
- 无需加锁：纯计算，无共享状态

### 9.2 内存效率

- 输入：字符串UTF-8字节数组
- 输出：long类型（8字节）
- 无中间对象：直接计算hash

---