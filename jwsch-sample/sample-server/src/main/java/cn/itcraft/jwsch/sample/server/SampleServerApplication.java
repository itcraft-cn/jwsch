package cn.itcraft.jwsch.sample.server;

import cn.itcraft.jwsch.srv.JwschServer;
import cn.itcraft.jwsch.srv.config.JwschConfig;
import cn.itcraft.jwsch.srv.config.TcpConfig;
import cn.itcraft.jwsch.srv.config.WebSocketConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleServerApplication {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleServerApplication.class);
    
    public static void main(String[] args) {
        LOGGER.info("Starting Jwsch Sample Server...");
        
        JwschConfig config = JwschConfig.builder()
            .webSocket(WebSocketConfig.builder()
                .port(8080)
                .path("/ws")
                .bossThreads(1)
                .workerThreads(4)
                .maxFrameSize(65536)
                .tcpNoDelay(true)
                .keepAlive(true)
                .build())
            .tcp(TcpConfig.builder()
                .port(9090)
                .bossThreads(1)
                .workerThreads(4)
                .tcpNoDelay(true)
                .keepAlive(true)
                .build())
            .build();
        
        JwschServer server = new JwschServer(config);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down Jwsch Sample Server...");
            server.shutdown();
        }));
        
        server.start();
        
        LOGGER.info("Jwsch Sample Server started successfully");
        LOGGER.info("WebSocket endpoint: ws://localhost:{}/ws", 
            config.getWebSocket().getPort());
        LOGGER.info("TCP endpoint: localhost:{}", 
            config.getTcp().getPort());
        
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}