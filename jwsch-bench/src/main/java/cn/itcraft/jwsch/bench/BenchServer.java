package cn.itcraft.jwsch.bench;

import cn.itcraft.jwsch.srv.JwschServer;
import cn.itcraft.jwsch.srv.config.JwschConfig;
import cn.itcraft.jwsch.srv.config.TcpConfig;
import cn.itcraft.jwsch.srv.config.WebSocketConfig;

/**
 * Benchmark 服务端封装。
 * 
 * <p>封装 JwschServer，提供 WebSocket 和 TCP 双协议支持。
 */
public final class BenchServer {
    
    private final JwschServer server;
    private final int wsPort;
    private final int tcpPort;
    
    /**
     * 创建 Benchmark 服务端。
     * 
     * @param wsPort WebSocket 端口
     * @param tcpPort TCP 端口
     * @param workerThreads 工作线程数
     */
    public BenchServer(int wsPort, int tcpPort, int workerThreads) {
        this.wsPort = wsPort;
        this.tcpPort = tcpPort;
        
        JwschConfig config = JwschConfig.builder()
            .webSocket(WebSocketConfig.builder()
                .port(wsPort)
                .path("/ws")
                .bossThreads(1)
                .workerThreads(workerThreads)
                .maxFrameSize(512 * 1024)
                .tcpNoDelay(true)
                .keepAlive(true)
                .build())
            .tcp(TcpConfig.builder()
                .port(tcpPort)
                .bossThreads(1)
                .workerThreads(workerThreads)
                .tcpNoDelay(true)
                .keepAlive(true)
                .build())
            .build();
        
        this.server = new JwschServer(config);
    }
    
    /**
     * 启动服务器。
     * 
     * <p>启动 WebSocket 和 TCP 服务，打印端口信息。
     */
    public void start() {
        server.start();
        System.out.println("Server started: WebSocket=" + wsPort + ", TCP=" + tcpPort);
    }
    
    /**
     * 关闭服务器。
     * 
     * <p>优雅关闭 WebSocket 和 TCP 服务。
     */
    public void shutdown() {
        server.shutdown();
        System.out.println("Server shutdown");
    }
}