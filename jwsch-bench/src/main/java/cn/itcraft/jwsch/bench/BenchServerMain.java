package cn.itcraft.jwsch.bench;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cn.itcraft.jwsch.common.flowcontrol.FlowControlConfig;

/**
 * Benchmark 服务端独立进程入口。
 * 
 * <p>启动 JwschServer，端口绑定完成后打印 SERVER_READY 标记，
 * 供 Shell 脚本等待。
 * 
 * <p>使用示例：
 * <pre>
 * java -jar jwsch-bench.jar server --wsPort 8080 --tcpPort 9090 --workers 16
 * java -jar jwsch-bench.jar server --inboundRateLimit 0
 * </pre>
 */
public final class BenchServerMain {
    
    /**
     * 服务端独立进程入口。
     * 
     * <p>启动 JwschServer，端口绑定完成后打印 SERVER_READY 标记，
     * 供 Shell 脚本等待。
     * 
     * @param args 命令行参数，支持 --wsPort, --tcpPort, --workers, --inboundRateLimit 等选项
     */
    public static void main(String[] args) {
        int wsPort = 8080;
        int tcpPort = 9090;
        int workers = 16;
        // -1 表示沿用默认流控配置；>=0 时覆盖对应项；inboundRateLimit=0 表示关闭入站限流。
        int inboundRateLimit = -1;
        int burst = 12000;
        int outboundQueue = 0;
        int outboundDisconnect = 0;
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--wsPort".equals(arg) && i + 1 < args.length) {
                wsPort = Integer.parseInt(args[++i]);
            } else if ("--tcpPort".equals(arg) && i + 1 < args.length) {
                tcpPort = Integer.parseInt(args[++i]);
            } else if ("--workers".equals(arg) && i + 1 < args.length) {
                workers = Integer.parseInt(args[++i]);
            } else if ("--inboundRateLimit".equals(arg) && i + 1 < args.length) {
                inboundRateLimit = Integer.parseInt(args[++i]);
            } else if ("--burst".equals(arg) && i + 1 < args.length) {
                burst = Integer.parseInt(args[++i]);
            } else if ("--outboundQueue".equals(arg) && i + 1 < args.length) {
                outboundQueue = Integer.parseInt(args[++i]);
            } else if ("--outboundDisconnect".equals(arg) && i + 1 < args.length) {
                outboundDisconnect = Integer.parseInt(args[++i]);
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                printHelp();
                System.exit(0);
            }
        }
        
        FlowControlConfig flowControl = buildFlowControl(inboundRateLimit, burst, outboundQueue, outboundDisconnect);
        
        printBanner(wsPort, tcpPort, workers, flowControl);
        
        BenchServer server = new BenchServer(wsPort, tcpPort, workers, flowControl);
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
    
    /**
     * 构建压测用流控配置。
     * 
     * @param inboundRateLimit 入站限流令牌数/秒；0 表示关闭；-1 表示使用默认值
     * @param burst 入站突发令牌数
     * @param outboundQueue 出站队列上限；0 表示使用默认值
     * @param outboundDisconnect 出站断连阈值；0 表示使用默认值
     * @return 流控配置
     */
    private static FlowControlConfig buildFlowControl(int inboundRateLimit, int burst,
                                                      int outboundQueue, int outboundDisconnect) {
        if (inboundRateLimit < 0 && outboundQueue == 0 && outboundDisconnect == 0) {
            return FlowControlConfig.defaultConfig();
        }
        FlowControlConfig.Builder builder = FlowControlConfig.builder();
        if (inboundRateLimit == 0) {
            builder.inboundEnabled(false);
        } else if (inboundRateLimit > 0) {
            builder.inboundEnabled(true)
                .maxTokensPerSecond(inboundRateLimit)
                .burstSize(burst);
        }
        if (outboundQueue > 0) {
            builder.maxQueueSize(outboundQueue);
        }
        if (outboundDisconnect > 0) {
            builder.disconnectThreshold(outboundDisconnect);
        }
        return builder.build();
    }
    
    private static void printBanner(int wsPort, int tcpPort, int workers, FlowControlConfig flowControl) {
        System.out.println("=== Jwsch Benchmark Server ===");
        System.out.println("WebSocket Port: " + wsPort);
        System.out.println("TCP Port: " + tcpPort);
        System.out.println("Worker Threads: " + workers);
        System.out.println("Inbound Rate Limit: " + (flowControl.isInboundEnabled()
            ? flowControl.getMaxTokensPerSecond() + " tokens/s (burst " + flowControl.getBurstSize() + ")"
            : "disabled"));
        System.out.println("Outbound Queue Limit: " + (flowControl.isOutboundEnabled()
            ? flowControl.getMaxQueueSize() + " (disconnect " + flowControl.getDisconnectThreshold() + ")"
            : "disabled"));
        System.out.println();
    }
    
    private static void printHelp() {
        System.out.println("Usage: java -jar jwsch-bench.jar server [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --wsPort <port>              WebSocket port (default: 8080)");
        System.out.println("  --tcpPort <port>             TCP port (default: 9090)");
        System.out.println("  --workers <count>            Worker threads (default: 16)");
        System.out.println("  --inboundRateLimit <n>       Inbound tokens/s; 0=disabled, -1=default (default: -1)");
        System.out.println("  --burst <n>                  Inbound burst size (default: 12000)");
        System.out.println("  --outboundQueue <n>          Outbound queue size; 0=default");
        System.out.println("  --outboundDisconnect <n>     Outbound disconnect threshold; 0=default");
        System.out.println("  --help, -h                   Show this help");
        System.out.println();
        System.out.println("Output:");
        System.out.println("  Prints 'SERVER_READY' when both ports are bound.");
    }
}