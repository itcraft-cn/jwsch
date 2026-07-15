package cn.itcraft.jwsch.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark 发布者独立进程入口。
 * 
 * <p>启动 N 个 TCP Publisher，独立 TPS 输出。
 * 
 * <p>使用示例：
 * <pre>
 * java -jar jwsch-bench.jar publisher --host localhost --tcpPort 9090 --publishers 1 --interval 50 --payloadSize 20480
 * </pre>
 */
public final class BenchPublisherMain {
    
    public static void main(String[] args) {
        String host = "localhost";
        int tcpPort = 9090;
        int publishers = 1;
        String topic = "/topic/bench";
        long interval = 10;
        int payloadSize = 2;
        int report = 5;
        int duration = 5;
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--host".equals(arg) && i + 1 < args.length) {
                host = args[++i];
            } else if ("--tcpPort".equals(arg) && i + 1 < args.length) {
                tcpPort = Integer.parseInt(args[++i]);
            } else if ("--publishers".equals(arg) && i + 1 < args.length) {
                publishers = Integer.parseInt(args[++i]);
            } else if ("--topic".equals(arg) && i + 1 < args.length) {
                topic = args[++i];
            } else if ("--interval".equals(arg) && i + 1 < args.length) {
                interval = Long.parseLong(args[++i]);
            } else if ("--payloadSize".equals(arg) && i + 1 < args.length) {
                payloadSize = Integer.parseInt(args[++i]);
            } else if ("--report".equals(arg) && i + 1 < args.length) {
                report = Integer.parseInt(args[++i]);
            } else if ("--duration".equals(arg) && i + 1 < args.length) {
                duration = Integer.parseInt(args[++i]);
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                printHelp();
                System.exit(0);
            }
        }
        
        printBanner(host, tcpPort, publishers, topic, interval, payloadSize, report, duration);
        
        TpsTracker tracker = new TpsTracker("PUB", publishers);
        List<BenchPublisher> pubList = new ArrayList<>();
        
        try {
            for (int i = 0; i < publishers; i++) {
                BenchPublisher pub = new BenchPublisher(
                    host, tcpPort, topic, interval, payloadSize, tracker);
                pub.start();
                pubList.add(pub);
            }
        } catch (Exception e) {
            System.err.println("[PUB] Failed to start publishers: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        
        ScheduledExecutorService reporter = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "pub-reporter");
            t.setDaemon(true);
            return t;
        });
        reporter.scheduleAtFixedRate(tracker::report, report, report, TimeUnit.SECONDS);
        
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[PUB] Shutting down...");
            for (BenchPublisher pub : pubList) {
                pub.stop();
            }
            reporter.shutdown();
            shutdownLatch.countDown();
        }));
        
        try {
            if (duration > 0) {
                Thread.sleep(duration * 60 * 1000L);
                System.out.println("[PUB] Duration completed.");
                for (BenchPublisher pub : pubList) {
                    pub.stop();
                }
                reporter.shutdown();
            } else {
                shutdownLatch.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("[PUB] Final stats: total=" + tracker.getTotalCount());
        System.out.println("[PUB] Shutdown complete.");
    }
    
    private static void printBanner(String host, int tcpPort, int publishers, 
                                    String topic, long interval, int payloadSize,
                                    int report, int duration) {
        System.out.println("=== Jwsch Benchmark Publisher ===");
        System.out.println("Host: " + host);
        System.out.println("TCP Port: " + tcpPort);
        System.out.println("Publishers: " + publishers);
        System.out.println("Topic: " + topic);
        System.out.println("Send Interval: " + interval + "μs");
        System.out.println("Payload Size: " + payloadSize + " bytes");
        System.out.println("Message Size: " + (8 + payloadSize) + " bytes");
        System.out.println("Report Interval: " + report + "s");
        System.out.println("Duration: " + (duration > 0 ? duration + " minutes" : "unlimited"));
        System.out.println();
    }
    
    private static void printHelp() {
        System.out.println("Usage: java -jar jwsch-bench.jar publisher [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --host <host>          Server host (default: localhost)");
        System.out.println("  --tcpPort <port>       Server TCP port (default: 9090)");
        System.out.println("  --publishers <count>   Number of publishers (default: 1)");
        System.out.println("  --topic <topic>        Topic to publish (default: /topic/bench)");
        System.out.println("  --interval <micros>    Send interval in microseconds (default: 10)");
        System.out.println("  --payloadSize <bytes>  Payload size (default: 2)");
        System.out.println("  --report <seconds>     TPS report interval (default: 5)");
        System.out.println("  --duration <minutes>   Duration, 0=unlimited (default: 5)");
        System.out.println("  --help, -h             Show this help");
    }
}