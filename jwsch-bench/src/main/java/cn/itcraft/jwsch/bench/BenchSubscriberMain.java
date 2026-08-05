package cn.itcraft.jwsch.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark 订阅者独立进程入口。
 * 
 * <p>启动 N 个 WebSocket Subscriber，所有订阅者就绪后打印 SUBSCRIBER_READY 标记，
 * 供 Shell 脚本等待。独立 TPS 输出。
 * 
 * <p>使用示例：
 * <pre>
 * java -jar jwsch-bench.jar subscriber --wsUrl ws://localhost:8080/ws --subscribers 50 --topic /topic/bench
 * </pre>
 */
public final class BenchSubscriberMain {
    
    /**
     * Benchmark 订阅者独立进程的主入口方法。
     * 
     * <p>解析命令行参数，启动指定数量的 WebSocket 订阅者，监控 TPS 并输出统计信息。
     * 当所有订阅者就绪后会打印 "SUBSCRIBER_READY" 标记供 Shell 脚本同步。
     * 
     * @param args 命令行参数：
     *             --wsUrl <url>          WebSocket URL
     *             --subscribers <count>  订阅者数量
     *             --topic <topic>        订阅主题
     *             --report <seconds>     TPS 报告间隔
     *             --duration <minutes>   运行时长（0表示无限）
     *             --help, -h             显示帮助信息
     */
    public static void main(String[] args) {
        String wsUrl = "ws://localhost:8080/ws";
        int subscribers = 5;
        String topic = "/topic/bench";
        int report = 5;
        int duration = 5;
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--wsUrl".equals(arg) && i + 1 < args.length) {
                wsUrl = args[++i];
            } else if ("--subscribers".equals(arg) && i + 1 < args.length) {
                subscribers = Integer.parseInt(args[++i]);
            } else if ("--topic".equals(arg) && i + 1 < args.length) {
                topic = args[++i];
            } else if ("--report".equals(arg) && i + 1 < args.length) {
                report = Integer.parseInt(args[++i]);
            } else if ("--duration".equals(arg) && i + 1 < args.length) {
                duration = Integer.parseInt(args[++i]);
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                printHelp();
                System.exit(0);
            }
        }
        
        printBanner(wsUrl, subscribers, topic, report, duration);
        
        TpsTracker tracker = new TpsTracker("SUB", subscribers);
        List<BenchSubscriber> subList = new ArrayList<>();
        
        try {
            for (int i = 0; i < subscribers; i++) {
                BenchSubscriber sub = new BenchSubscriber(i + 1, wsUrl, topic, tracker);
                sub.start();
                subList.add(sub);
                Thread.sleep(50);
            }
            
            Thread.sleep(2000);
        } catch (Exception e) {
            System.err.println("[SUB] Failed to start subscribers: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        
        System.out.println("SUBSCRIBER_READY");
        
        ScheduledExecutorService reporter = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sub-reporter");
            t.setDaemon(true);
            return t;
        });
        reporter.scheduleAtFixedRate(tracker::report, report, report, TimeUnit.SECONDS);
        
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[SUB] Shutting down...");
            for (BenchSubscriber sub : subList) {
                sub.stop();
            }
            reporter.shutdown();
            shutdownLatch.countDown();
        }));
        
        try {
            if (duration > 0) {
                Thread.sleep(duration * 60 * 1000L);
                System.out.println("[SUB] Duration completed.");
                for (BenchSubscriber sub : subList) {
                    sub.stop();
                }
                reporter.shutdown();
            } else {
                shutdownLatch.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("[SUB] Final stats: total=" + tracker.getTotalCount());
        System.out.println("[SUB] Shutdown complete.");
    }
    
    /**
     * 打印启动横幅信息。
     * 
     * @param wsUrl        WebSocket URL
     * @param subscribers  订阅者数量
     * @param topic        订阅主题
     * @param report       报告间隔（秒）
     * @param duration     运行时长（分钟，0表示无限）
     */
    private static void printBanner(String wsUrl, int subscribers, String topic,
                                    int report, int duration) {
        System.out.println("=== Jwsch Benchmark Subscriber ===");
        System.out.println("WebSocket URL: " + wsUrl);
        System.out.println("Subscribers: " + subscribers);
        System.out.println("Topic: " + topic);
        System.out.println("Report Interval: " + report + "s");
        System.out.println("Duration: " + (duration > 0 ? duration + " minutes" : "unlimited"));
        System.out.println();
    }
    
    /**
     * 打印帮助信息。
     */
    private static void printHelp() {
        System.out.println("Usage: java -jar jwsch-bench.jar subscriber [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --wsUrl <url>          WebSocket URL (default: ws://localhost:8080/ws)");
        System.out.println("  --subscribers <count>  Number of subscribers (default: 5)");
        System.out.println("  --topic <topic>        Topic to subscribe (default: /topic/bench)");
        System.out.println("  --report <seconds>     TPS report interval (default: 5)");
        System.out.println("  --duration <minutes>   Duration, 0=unlimited (default: 5)");
        System.out.println("  --help, -h             Show this help");
        System.out.println();
        System.out.println("Output:");
        System.out.println("  Prints 'SUBSCRIBER_READY' when all subscribers are connected and subscribed.");
    }
}