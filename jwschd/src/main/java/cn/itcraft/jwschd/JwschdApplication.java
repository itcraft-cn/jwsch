package cn.itcraft.jwschd;

import cn.itcraft.jwsch.srv.JwschServer;
import cn.itcraft.jwsch.srv.config.JwschConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JwschdApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwschdApplication.class);

    private static final String VERSION = "1.0.0-SNAPSHOT";

    private static volatile JwschServer server;

    public static void main(String[] args) {
        printBanner();

        try {
            JwschConfig config = ConfigLoader.load(args);

            if (!config.isEnabled()) {
                LOGGER.info("Jwschd is disabled");
                return;
            }

            server = new JwschServer(config);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Shutting down jwschd...");
                if (server != null) {
                    server.shutdown();
                }
                LOGGER.info("jwschd stopped");
            }));

            server.start();

            printStartupInfo(config);

            Thread.currentThread().join();

        } catch (Exception e) {
            LOGGER.error("Failed to start jwschd", e);
            System.exit(1);
        }
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("  ___ (_)_      _______  __");
        System.out.println(" / _ \\| \\ \\ /\\ / / _ \\/> /");
        System.out.println("|  __/| |\\ V  V /  __/>  < ");
        System.out.println(" \\___|/ | \\_/\\_/ \\___/_/\\_\\");
        System.out.println("    |__/");
        System.out.println();
        System.out.println("  Enterprise Java WebSocket Exchange Daemon v" + VERSION);
        System.out.println();
    }

    private static void printStartupInfo(JwschConfig config) {
        LOGGER.info("");
        LOGGER.info("==========================================================");
        LOGGER.info(" jwschd started successfully");
        LOGGER.info("==========================================================");

        LOGGER.info("  WebSocket : ws://0.0.0.0:{}/{}",
                    config.getWebSocket().getPort(),
                    config.getWebSocket().getPath());

        LOGGER.info("  TCP       : 0.0.0.0:{}", config.getTcp().getPort());

        if (config.getHealth().isEnabled()) {
            LOGGER.info("  Health    : http://0.0.0.0:{}/health", config.getHealth().getPort());
        }

        if (config.getMetrics().isEnabled()) {
            LOGGER.info("  Metrics   : http://0.0.0.0:{}{}",
                        config.getMetrics().getPort(),
                        config.getMetrics().getPath());
        }

        if (config.getCluster().isEnabled()) {
            LOGGER.info("  Cluster   : enabled, nodeId={}", config.getCluster().getNodeId());
        }

        LOGGER.info("==========================================================");
        LOGGER.info("");
    }

    public static JwschServer getServer() {
        return server;
    }

    public static String getVersion() {
        return VERSION;
    }
}
