package cn.itcraft.jwsch.bench;

import java.util.concurrent.CountDownLatch;

/**
 * 价格发布者启动入口。
 * 
 * 启动参数：
 *   --host localhost
 *   --port 9090
 *   --interval 10
 */
public final class PricePublisherMain {
    
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 9090;
        int intervalMs = 10;
        
        for (int i = 0; i < args.length; i++) {
            if ("--host".equals(args[i]) && i + 1 < args.length) {
                host = args[++i];
            } else if ("--port".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            } else if ("--interval".equals(args[i]) && i + 1 < args.length) {
                intervalMs = Integer.parseInt(args[++i]);
            }
        }
        
        System.out.println("=== Price Publisher ===");
        System.out.println("Host: " + host);
        System.out.println("Port: " + port);
        System.out.println("Interval: " + intervalMs + "ms");
        System.out.println();
        
        PricePublisher publisher = new PricePublisher(host, port);
        publisher.start(intervalMs);
        
        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[PRICE] Shutting down...");
            publisher.stop();
            latch.countDown();
        }));
        
        latch.await();
    }
}
