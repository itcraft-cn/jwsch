/**
 * 示例服务器应用入口。
 * 
 * <p>启动一个完整的 jwsch 服务器实例，包含 WebSocket 和 TCP 端点。
 * 使用 Builder 模式配置服务器参数，支持优雅关机。
 * 
 * <p>默认配置：
 * <ul>
 *   <li>WebSocket: 端口 8080, 路径 /ws, 4个Worker线程</li>
 *   <li>TCP: 端口 9090, 4个Worker线程</li>
 * </ul>
 * 
 * @author itcraft
 * @since 1.0
 */
package cn.itcraft.jwsch.sample.server;

import cn.itcraft.jwsch.srv.JwschServer;
import cn.itcraft.jwsch.srv.config.JwschConfig;
import cn.itcraft.jwsch.srv.config.TcpConfig;
import cn.itcraft.jwsch.srv.config.WebSocketConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleServerApplication {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleServerApplication.class);
    
    /**
     * 示例服务器的主入口方法。
     * 
     * <p>创建并启动 jwsch 服务器，注册优雅关机钩子，保持主线程运行。
     * 
     * @param args 命令行参数（当前未使用）
     */
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