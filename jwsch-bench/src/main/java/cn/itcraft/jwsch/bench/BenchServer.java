package cn.itcraft.jwsch.bench;

import cn.itcraft.jwsch.common.flowcontrol.FlowControlConfig;
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
     * 创建 Benchmark 服务端（使用默认流控配置）。
     * 
     * @param wsPort WebSocket 端口
     * @param tcpPort TCP 端口
     * @param workerThreads 工作线程数
     */
    public BenchServer(int wsPort, int tcpPort, int workerThreads) {
        this(wsPort, tcpPort, workerThreads, FlowControlConfig.defaultConfig());
    }
    
    /**
     * 创建 Benchmark 服务端。
     * 
     * @param wsPort WebSocket 端口
     * @param tcpPort TCP 端口
     * @param workerThreads 工作线程数
     * @param flowControl 流控配置，用于放开压测场景下的入站限流等
     */
    public BenchServer(int wsPort, int tcpPort, int workerThreads, FlowControlConfig flowControl) {
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
            .flowControl(flowControl)
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