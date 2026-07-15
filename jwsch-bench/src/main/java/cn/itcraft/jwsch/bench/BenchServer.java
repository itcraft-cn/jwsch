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
    
    public void start() {
        server.start();
        System.out.println("Server started: WebSocket=" + wsPort + ", TCP=" + tcpPort);
    }
    
    public void shutdown() {
        server.shutdown();
        System.out.println("Server shutdown");
    }
}