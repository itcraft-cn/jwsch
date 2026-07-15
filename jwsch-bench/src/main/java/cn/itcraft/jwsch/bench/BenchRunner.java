package cn.itcraft.jwsch.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark 主入口。
 * 
 * <p>启动 JwschServer、N 个 WebSocket Subscriber、M 个 TCP Publisher，
 * 定时输出 TPS 统计。
 * 
 * <p>使用示例：
 * <pre>
 * java -jar jwsch-bench.jar
 * java -jar jwsch-bench.jar --publishers 3 --subscribers 30 --duration 1
 * </pre>
 */
public final class BenchRunner {
    
    public static void main(String[] args) {
        BenchConfig config = parseArgs(args);
        
        printBanner(config);
        
        final BenchServer server;
        final List<BenchPublisher> publishers = new ArrayList<>();
        final List<BenchSubscriber> subscribers = new ArrayList<>();
        final ScheduledExecutorService reporter;
        
        try {
            server = new BenchServer(config.getWsPort(), config.getTcpPort(), config.getWorkerThreads());
            server.start();
            
            System.out.println("Waiting for server to initialize...");
            Thread.sleep(2000);
            
            TpsTracker pubTracker = new TpsTracker("Publisher", config.getPublisherCount());
            TpsTracker subTracker = new TpsTracker("Subscriber", config.getSubscriberCount());
            
            String wsUrl = "ws://localhost:" + config.getWsPort() + "/ws";
            for (int i = 0; i < config.getSubscriberCount(); i++) {
                BenchSubscriber sub = new BenchSubscriber(
                    i + 1, wsUrl, config.getTopic(), subTracker);
                sub.start();
                subscribers.add(sub);
            }
            
            System.out.println("Waiting for subscribers to register...");
            Thread.sleep(2000);
            
            for (int i = 0; i < config.getPublisherCount(); i++) {
                BenchPublisher pub = new BenchPublisher(
                    "localhost", config.getTcpPort(),
                    config.getTopic(), config.getSendIntervalMicros(), 
                    config.getPayloadSize(), pubTracker
                );
                pub.start();
                publishers.add(pub);
            }
            
            reporter = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "bench-reporter");
                t.setDaemon(true);
                return t;
            });
            reporter.scheduleAtFixedRate(() -> {
                System.out.println();
                pubTracker.report();
                subTracker.report();
            }, config.getReportIntervalSeconds(), config.getReportIntervalSeconds(), TimeUnit.SECONDS);
            
            if (config.getDurationMinutes() > 0) {
                long durationMs = config.getDurationMinutes() * 60 * 1000L;
                Thread.sleep(durationMs);
                
                System.out.println();
                System.out.println("Benchmark completed.");
                
                shutdown(publishers, subscribers, server, reporter);
            } else {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println();
                    System.out.println("Stopping benchmark...");
                    shutdown(publishers, subscribers, server, reporter);
                }));
                
                Thread.currentThread().join();
            }
            
        } catch (Exception e) {
            System.err.println("Benchmark failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void shutdown(List<BenchPublisher> publishers, List<BenchSubscriber> subscribers,
                                 BenchServer server, ScheduledExecutorService reporter) {
        System.out.println("Stopping publishers...");
        for (BenchPublisher pub : publishers) {
            pub.stop();
        }
        System.out.println("Publishers stopped.");
        
        System.out.println("Stopping subscribers...");
        for (BenchSubscriber sub : subscribers) {
            sub.stop();
        }
        System.out.println("Subscribers stopped.");
        
        System.out.println("Shutting down server...");
        if (server != null) {
            server.shutdown();
        }
        
        if (reporter != null) {
            reporter.shutdown();
        }
        
        System.out.println("Shutdown complete.");
    }
    
    private static void printBanner(BenchConfig config) {
        System.out.println("=== Jwsch Benchmark ===");
        System.out.println("WebSocket Port: " + config.getWsPort());
        System.out.println("TCP Port: " + config.getTcpPort());
        System.out.println("Worker Threads: " + config.getWorkerThreads());
        System.out.println("Publishers: " + config.getPublisherCount());
        System.out.println("Subscribers: " + config.getSubscriberCount());
        System.out.println("Topic: " + config.getTopic());
        System.out.println("Send Interval: " + config.getSendIntervalMicros() + "μs");
        System.out.println("Payload Size: " + config.getPayloadSize() + " bytes");
        System.out.println("Total Message Size: " + config.getMessageSizeBytes() + " bytes (8 + " + config.getPayloadSize() + ")");
        System.out.println("Duration: " + (config.getDurationMinutes() > 0 ? 
            config.getDurationMinutes() + " minutes" : "unlimited"));
        System.out.println("Report Interval: " + config.getReportIntervalSeconds() + "s");
        System.out.println();
    }
    
    private static BenchConfig parseArgs(String[] args) {
        BenchConfig.Builder builder = BenchConfig.builder();
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            if ("--wsPort".equals(arg) && i + 1 < args.length) {
                builder.wsPort(Integer.parseInt(args[++i]));
            } else if ("--tcpPort".equals(arg) && i + 1 < args.length) {
                builder.tcpPort(Integer.parseInt(args[++i]));
            } else if ("--publishers".equals(arg) && i + 1 < args.length) {
                builder.publisherCount(Integer.parseInt(args[++i]));
            } else if ("--subscribers".equals(arg) && i + 1 < args.length) {
                builder.subscriberCount(Integer.parseInt(args[++i]));
            } else if ("--topic".equals(arg) && i + 1 < args.length) {
                builder.topic(args[++i]);
            } else if ("--interval".equals(arg) && i + 1 < args.length) {
                builder.sendIntervalMicros(Long.parseLong(args[++i]));
            } else if ("--payloadSize".equals(arg) && i + 1 < args.length) {
                builder.payloadSize(Integer.parseInt(args[++i]));
            } else if ("--duration".equals(arg) && i + 1 < args.length) {
                builder.durationMinutes(Integer.parseInt(args[++i]));
            } else if ("--report".equals(arg) && i + 1 < args.length) {
                builder.reportIntervalSeconds(Integer.parseInt(args[++i]));
            } else if ("--workers".equals(arg) && i + 1 < args.length) {
                builder.workerThreads(Integer.parseInt(args[++i]));
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                printHelp();
                System.exit(0);
            }
        }
        
        return builder.build();
    }
    
    private static void printHelp() {
        System.out.println("Usage: java -jar jwsch-bench.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --wsPort <port>         WebSocket port (default: 8080)");
        System.out.println("  --tcpPort <port>        TCP port (default: 9090)");
        System.out.println("  --workers <count>       Worker threads (default: 16)");
        System.out.println("  --publishers <count>    Number of publishers (default: 1)");
        System.out.println("  --subscribers <count>   Number of subscribers (default: 5)");
        System.out.println("  --topic <topic>         Topic to subscribe/publish (default: /topic/bench)");
        System.out.println("  --interval <micros>     Send interval in microseconds (default: 10)");
        System.out.println("  --payloadSize <bytes>   Payload size in bytes, 2-2M (default: 2)");
        System.out.println("  --duration <minutes>    Duration in minutes, 0 for unlimited (default: 5)");
        System.out.println("  --report <seconds>      Report interval in seconds (default: 5)");
        System.out.println("  --help, -h              Show this help");
    }
}