# Jwsch Sample

完整的jwsch中间件平台示例项目，展示三个子模块如何协同工作。

## 项目结构

```
jwsch-sample/
├── sample-server/     # 子模块1: jwsch-srv 启动器
├── sample-webapp/     # 子模块2: Spring Boot + Vue 3 前端
└── sample-pusher/     # 子模块3: CLI推送工具
```

## 架构

```
┌─────────────────┐     WebSocket      ┌─────────────────┐
│  sample-webapp  │◄──────────────────►│  sample-server  │
│  (Vue 3 SPA)    │    ws://localhost   │  (jwsch-srv)    │
│  jwsch-js       │      :8080/ws       │  port: 8080     │
│  port: 3000     │                     └────────┬────────┘
└─────────────────┘                              │ TCP
                                                 │ :9090
                                                 ▼
                                        ┌─────────────────┐
                                        │  sample-pusher  │
                                        │  (jwsch-cli)    │
                                        │  CLI推送工具     │
                                        └─────────────────┘
```

## 快速开始

### 1. 构建项目

```bash
# 安装jwsch-js依赖并构建
cd jwsch-js && npm install && npm run build && cd ..

# 构建Java项目
mvn install -DskipTests
```

### 2. 启动 sample-server

```bash
java -jar jwsch-sample/sample-server/target/sample-server-1.0.0-SNAPSHOT.jar
```

输出:
```
Jwsch Sample Server started successfully
WebSocket endpoint: ws://localhost:8080/ws
```

### 3. 启动 sample-webapp

```bash
# 开发模式 (前端热重载)
cd jwsch-sample/sample-webapp/frontend
npm install
npm run dev
# 访问 http://localhost:5173

# 生产模式
java -jar jwsch-sample/sample-webapp/target/sample-webapp-1.0.0-SNAPSHOT.jar
# 访问 http://localhost:3000
```

### 4. 启动 sample-pusher

```bash
java -jar jwsch-sample/sample-pusher/target/sample-pusher-1.0.0-SNAPSHOT.jar \
  --host localhost \
  --port 9090 \
  --topic /topic/news \
  --interval 5000 \
  --message "Hello from pusher!"
```

参数说明:
- `--host`: jwsch-server地址 (默认: localhost)
- `--port`: TCP端口 (默认: 9090)
- `--topic`: 推送Topic (默认: /topic/news)
- `--interval`: 推送间隔ms (默认: 5000)
- `--message`: 推送消息内容

## 模块详解

### sample-server

jwsch-srv的启动器，提供WebSocket服务。

- 端口: 8080
- 路径: /ws
- 功能: 消息路由、Topic订阅管理

### sample-webapp

Spring Boot 2.x + Vue 3 单页应用。

**后端 (Spring Boot)**
- 端口: 3000
- API: /api/info, /api/health

**前端 (Vue 3 + jwsch-js)**
- 连接WebSocket
- 订阅Topic
- 实时显示推送消息

### sample-pusher

CLI推送工具，使用jwsch-cli通过TCP连接推送消息。

```bash
# 帮助
java -jar sample-pusher.jar --help

# 自定义推送
java -jar sample-pusher.jar \
  --topic /topic/alerts \
  --interval 1000 \
  --message '{"type":"alert","level":"info","text":"Test alert"}'
```

## 开发指南

### 前端开发

```bash
cd jwsch-sample/sample-webapp/frontend
npm run dev
```

访问 http://localhost:5173，API代理到 http://localhost:3000

### 跳过前端构建

```bash
mvn package -Dskip.frontend=true
```

## 技术栈

| 模块 | 技术 |
|------|------|
| sample-server | jwsch-srv, Netty 4.2, Logback |
| sample-webapp | Spring Boot 2.7, Vue 3, Vite, jwsch-js |
| sample-pusher | jwsch-cli, Commons CLI |

## 许可证

Apache License 2.0