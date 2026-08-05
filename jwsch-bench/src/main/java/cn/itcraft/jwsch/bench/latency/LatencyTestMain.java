package cn.itcraft.jwsch.bench.latency;

/**
 * 延迟测试独立进程入口。
 * 
 * <p>启动 1 pub + 1 sub，测量端到端延迟。
 * 
 * <p>使用示例：
 * <pre>
 * java -Dio.netty.leakDetection.level=disabled -cp jwsch-bench.jar cn.itcraft.jwsch.bench.latency.LatencyTestMain --host localhost --tcpPort 9090 --wsUrl ws://localhost:8080/ws --topic /topic/latency --interval 100 --payloadSize 64 --duration 1
 * </pre>
 * 
 * <p>注意：必须添加 -Dio.netty.leakDetection.level=disabled 禁用 Netty 泄漏检测，
 * 否则高吞吐下每次 ByteBuf 分配都会创建 Throwable 记录调用栈，严重影响性能。
 */
public final class LatencyTestMain {
    
    /**
     * 延迟测试的主入口方法。
     * 
     * <p>启动一个发布者和一个订阅者，测量端到端延迟并输出统计结果。
     * 
     * @param args 命令行参数：
     *             --host <host>         服务器主机
     *             --tcpPort <port>      服务器 TCP 端口
     *             --wsUrl <url>         WebSocket URL
     *             --topic <topic>       主题
     *             --interval <micros>   发送间隔（微秒）
     *             --payloadSize <bytes> 负载大小（字节）
     *             --duration <minutes>  运行时长（分钟）
     *             --help, -h            显示帮助信息
     */
    public static void main(String[] args) {
        String host = "localhost";
        int tcpPort = 9090;
        String wsUrl = "ws://localhost:8080/ws";
        String topic = "/topic/latency";
        long interval = 100;
        int payloadSize = 64;
        int duration = 1;
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--host".equals(arg) && i + 1 < args.length) {
                host = args[++i];
            } else if ("--tcpPort".equals(arg) && i + 1 < args.length) {
                tcpPort = Integer.parseInt(args[++i]);
            } else if ("--wsUrl".equals(arg) && i + 1 < args.length) {
                wsUrl = args[++i];
            } else if ("--topic".equals(arg) && i + 1 < args.length) {
                topic = args[++i];
            } else if ("--interval".equals(arg) && i + 1 < args.length) {
                interval = Long.parseLong(args[++i]);
            } else if ("--payloadSize".equals(arg) && i + 1 < args.length) {
                payloadSize = Integer.parseInt(args[++i]);
            } else if ("--duration".equals(arg) && i + 1 < args.length) {
                duration = Integer.parseInt(args[++i]);
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                printHelp();
                System.exit(0);
            }
        }
        
        printBanner(host, tcpPort, wsUrl, topic, interval, payloadSize, duration);
        
        LatencyTracker latencyTracker = new LatencyTracker();
        LatencyPublisher publisher = null;
        LatencySubscriber subscriber = null;
        
        try {
            System.out.println("[TEST] Starting subscriber...");
            subscriber = new LatencySubscriber(1, wsUrl, topic, latencyTracker);
            subscriber.start();
            
            Thread.sleep(500);
            
            System.out.println("[TEST] Starting publisher...");
            publisher = new LatencyPublisher(host, tcpPort, topic, interval, payloadSize);
            publisher.start();
            
            System.out.println("[TEST] Running for " + duration + " minute(s)...");
            Thread.sleep(duration * 60 * 1000L);
            
            System.out.println("[TEST] Duration completed.");
            
        } catch (Exception e) {
            System.err.println("[TEST] Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (publisher != null) {
                System.out.println("[TEST] Stopping publisher...");
                publisher.stop();
            }
            if (subscriber != null) {
                System.out.println("[TEST] Stopping subscriber...");
                subscriber.stop();
            }
        }
        
        latencyTracker.reportFinal();
        System.out.println("[TEST] Shutdown complete.");
    }
    
    /**
     * 打印启动横幅信息。
     * 
     * @param host        服务器主机
     * @param tcpPort     TCP 端口
     * @param wsUrl       WebSocket URL
     * @param topic       主题
     * @param interval    发送间隔（微秒）
     * @param payloadSize 负载大小（字节）
     * @param duration    运行时长（分钟）
     */
    private static void printBanner(String host, int tcpPort, String wsUrl, 
                                    String topic, long interval, int payloadSize, int duration) {
        System.out.println("=== Latency Test ===");
        System.out.println("Host: " + host);
        System.out.println("TCP Port: " + tcpPort);
        System.out.println("WebSocket URL: " + wsUrl);
        System.out.println("Topic: " + topic);
        System.out.println("Send Interval: " + interval + "μs");
        System.out.println("Payload Size: " + payloadSize + " bytes");
        System.out.println("Duration: " + duration + " minute(s)");
        System.out.println();
    }
    
    /**
     * 打印帮助信息。
     */
    private static void printHelp() {
        System.out.println("Usage: java -Dio.netty.leakDetection.level=disabled -cp jwsch-bench.jar cn.itcraft.jwsch.bench.latency.LatencyTestMain [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --host <host>          Server host (default: localhost)");
        System.out.println("  --tcpPort <port>       Server TCP port (default: 9090)");
        System.out.println("  --wsUrl <url>          WebSocket URL (default: ws://localhost:8080/ws)");
        System.out.println("  --topic <topic>        Topic (default: /topic/latency)");
        System.out.println("  --interval <micros>    Send interval in microseconds (default: 100)");
        System.out.println("  --payloadSize <bytes>  Payload size (default: 64)");
        System.out.println("  --duration <minutes>   Duration in minutes (default: 1)");
        System.out.println("  --help, -h             Show this help");
    }
}