package cn.itcraft.jwsch.bench.latency;

/**
 * 延迟测试订阅者独立进程入口。
 * 
 * <p>接收消息并计算端到端延迟，最终输出统计结果。
 * 
 * <p>使用示例：
 * <pre>
 * java -Dio.netty.leakDetection.level=disabled -cp jwsch-bench.jar cn.itcraft.jwsch.bench.latency.LatencySubscriberMain --wsUrl ws://localhost:8080/ws --topic /topic/latency --duration 1
 * </pre>
 * 
 * <p>注意：必须添加 -Dio.netty.leakDetection.level=disabled 禁用 Netty 泄漏检测，
 * 否则高吞吐下每次 ByteBuf 分配都会创建 Throwable 记录调用栈，严重影响性能。
 */
public final class LatencySubscriberMain {
    
    public static void main(String[] args) {
        String wsUrl = "ws://localhost:8080/ws";
        String topic = "/topic/latency";
        int duration = 1;
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--wsUrl".equals(arg) && i + 1 < args.length) {
                wsUrl = args[++i];
            } else if ("--topic".equals(arg) && i + 1 < args.length) {
                topic = args[++i];
            } else if ("--duration".equals(arg) && i + 1 < args.length) {
                duration = Integer.parseInt(args[++i]);
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                printHelp();
                System.exit(0);
            }
        }
        
        printBanner(wsUrl, topic, duration);
        
        LatencyTracker latencyTracker = new LatencyTracker();
        LatencySubscriber subscriber = null;
        
        try {
            subscriber = new LatencySubscriber(1, wsUrl, topic, latencyTracker);
            subscriber.start();
            
            System.out.println("SUBSCRIBER_READY");
            
            if (duration > 0) {
                Thread.sleep(duration * 60 * 1000L);
                System.out.println("[SUB] Duration completed.");
            } else {
                Thread.currentThread().join();
            }
            
        } catch (Exception e) {
            System.err.println("[SUB] Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (subscriber != null) {
                System.out.println("[SUB] Stopping...");
                subscriber.stop();
            }
        }
        
        latencyTracker.reportFinal();
        System.out.println("[SUB] Shutdown complete.");
    }
    
    private static void printBanner(String wsUrl, String topic, int duration) {
        System.out.println("=== Latency Test Subscriber ===");
        System.out.println("WebSocket URL: " + wsUrl);
        System.out.println("Topic: " + topic);
        System.out.println("Duration: " + duration + " minute(s)");
        System.out.println();
    }
    
    private static void printHelp() {
        System.out.println("Usage: java -Dio.netty.leakDetection.level=disabled -cp jwsch-bench.jar cn.itcraft.jwsch.bench.latency.LatencySubscriberMain [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --wsUrl <url>          WebSocket URL (default: ws://localhost:8080/ws)");
        System.out.println("  --topic <topic>        Topic (default: /topic/latency)");
        System.out.println("  --duration <minutes>   Duration in minutes, 0=unlimited (default: 1)");
        System.out.println("  --help, -h             Show this help");
        System.out.println();
        System.out.println("Output:");
        System.out.println("  Prints 'SUBSCRIBER_READY' when connected and subscribed.");
        System.out.println("  At end, prints latency statistics (avg/min/max/p50/p90/p95/p99).");
    }
}