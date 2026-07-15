package cn.itcraft.jwsch.bench.latency;

/**
 * 延迟测试发布者独立进程入口。
 * 
 * <p>发送带时间戳的消息，用于测量端到端延迟。
 * 
 * <p>使用示例：
 * <pre>
 * java -Dio.netty.leakDetection.level=disabled -cp jwsch-bench.jar cn.itcraft.jwsch.bench.latency.LatencyPublisherMain --host localhost --tcpPort 9090 --topic /topic/latency --interval 100 --payloadSize 64 --duration 1
 * </pre>
 * 
 * <p>注意：必须添加 -Dio.netty.leakDetection.level=disabled 禁用 Netty 泄漏检测，
 * 否则高吞吐下每次 ByteBuf 分配都会创建 Throwable 记录调用栈，严重影响性能。
 */
public final class LatencyPublisherMain {
    
    public static void main(String[] args) {
        String host = "localhost";
        int tcpPort = 9090;
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
        
        printBanner(host, tcpPort, topic, interval, payloadSize, duration);
        
        LatencyPublisher publisher = null;
        
        try {
            publisher = new LatencyPublisher(host, tcpPort, topic, interval, payloadSize);
            publisher.start();
            
            System.out.println("PUBLISHER_READY");
            
            if (duration > 0) {
                Thread.sleep(duration * 60 * 1000L);
                System.out.println("[PUB] Duration completed.");
            } else {
                Thread.currentThread().join();
            }
            
        } catch (Exception e) {
            System.err.println("[PUB] Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (publisher != null) {
                System.out.println("[PUB] Stopping...");
                publisher.stop();
            }
        }
        
        System.out.println("[PUB] Total sent: " + (publisher != null ? publisher.getSendCount() : 0));
        System.out.println("[PUB] Shutdown complete.");
    }
    
    private static void printBanner(String host, int tcpPort, String topic, 
                                    long interval, int payloadSize, int duration) {
        System.out.println("=== Latency Test Publisher ===");
        System.out.println("Host: " + host);
        System.out.println("TCP Port: " + tcpPort);
        System.out.println("Topic: " + topic);
        System.out.println("Send Interval: " + interval + "μs");
        System.out.println("Payload Size: " + payloadSize + " bytes");
        System.out.println("Duration: " + duration + " minute(s)");
        System.out.println();
    }
    
    private static void printHelp() {
        System.out.println("Usage: java -Dio.netty.leakDetection.level=disabled -cp jwsch-bench.jar cn.itcraft.jwsch.bench.latency.LatencyPublisherMain [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --host <host>          Server host (default: localhost)");
        System.out.println("  --tcpPort <port>       Server TCP port (default: 9090)");
        System.out.println("  --topic <topic>        Topic (default: /topic/latency)");
        System.out.println("  --interval <micros>    Send interval in microseconds (default: 100)");
        System.out.println("  --payloadSize <bytes>  Payload size (default: 64)");
        System.out.println("  --duration <minutes>   Duration in minutes, 0=unlimited (default: 1)");
        System.out.println("  --help, -h             Show this help");
        System.out.println();
        System.out.println("Output:");
        System.out.println("  Prints 'PUBLISHER_READY' when ready to send.");
    }
}