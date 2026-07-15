package cn.itcraft.jwsch.bench;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark 服务端独立进程入口。
 * 
 * <p>启动 JwschServer，端口绑定完成后打印 SERVER_READY 标记，
 * 供 Shell 脚本等待。
 * 
 * <p>使用示例：
 * <pre>
 * java -jar jwsch-bench.jar server --wsPort 8080 --tcpPort 9090 --workers 16
 * </pre>
 */
public final class BenchServerMain {
    
    public static void main(String[] args) {
        int wsPort = 8080;
        int tcpPort = 9090;
        int workers = 16;
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--wsPort".equals(arg) && i + 1 < args.length) {
                wsPort = Integer.parseInt(args[++i]);
            } else if ("--tcpPort".equals(arg) && i + 1 < args.length) {
                tcpPort = Integer.parseInt(args[++i]);
            } else if ("--workers".equals(arg) && i + 1 < args.length) {
                workers = Integer.parseInt(args[++i]);
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                printHelp();
                System.exit(0);
            }
        }
        
        printBanner(wsPort, tcpPort, workers);
        
        BenchServer server = new BenchServer(wsPort, tcpPort, workers);
        server.start();
        
        System.out.println("SERVER_READY");
        
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[SERVER] Shutting down...");
            server.shutdown();
            shutdownLatch.countDown();
        }));
        
        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("[SERVER] Shutdown complete.");
    }
    
    private static void printBanner(int wsPort, int tcpPort, int workers) {
        System.out.println("=== Jwsch Benchmark Server ===");
        System.out.println("WebSocket Port: " + wsPort);
        System.out.println("TCP Port: " + tcpPort);
        System.out.println("Worker Threads: " + workers);
        System.out.println();
    }
    
    private static void printHelp() {
        System.out.println("Usage: java -jar jwsch-bench.jar server [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --wsPort <port>    WebSocket port (default: 8080)");
        System.out.println("  --tcpPort <port>   TCP port (default: 9090)");
        System.out.println("  --workers <count>  Worker threads (default: 16)");
        System.out.println("  --help, -h         Show this help");
        System.out.println();
        System.out.println("Output:");
        System.out.println("  Prints 'SERVER_READY' when both ports are bound.");
    }
}