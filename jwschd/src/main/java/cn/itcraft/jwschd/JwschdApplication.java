/**
 * Jwschd 应用主入口。
 * 
 * <p>独立部署的 Jwsch 服务端应用，提供 WebSocket、TCP 双协议支持。
 * 包含优雅启动、配置加载、信号处理和状态监控功能。
 * 
 * <p>启动流程：
 * <ol>
 *   <li>打印 Banner 和版本信息</li>
 *   <li>加载配置（文件/环境变量/命令行参数）</li>
 *   <li>创建并启动 JwschServer</li>
 *   <li>注册 Shutdown Hook 优雅停机</li>
 *   <li>打印服务启动信息</li>
 *   <li>进入阻塞等待状态</li>
 * </ol>
 * 
 * <p>使用示例：
 * <pre>
 *   java -jar jwschd.jar
 *   java -jar jwschd.jar --config /etc/jwsch/config.yml
 *   java -jar jwschd.jar -c /path/to/config.yml --jwsch.websocket.port=8080
 * </pre>
 */
package cn.itcraft.jwschd;

import cn.itcraft.jwsch.srv.JwschServer;
import cn.itcraft.jwsch.srv.config.JwschConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jwschd 应用主类。
 * 
 * <p>提供 Jwsch 服务的独立部署入口，支持优雅启动、配置加载和信号处理。
 * 采用单例模式管理服务器实例。
 */
public final class JwschdApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwschdApplication.class);

    private static final String VERSION = "1.0.0-SNAPSHOT";

    private static volatile JwschServer server;

    /**
     * 应用主入口。
     * 
     * <p>执行完整启动流程：
     * <ol>
     *   <li>打印 Banner 和版本信息</li>
     *   <li>通过 {@link ConfigLoader} 加载配置</li>
     *   <li>检查服务是否启用（配置中的 enabled 字段）</li>
     *   <li>创建 {@link JwschServer} 实例</li>
     *   <li>注册 Shutdown Hook 处理停机信号</li>
     *   <li>启动服务器并打印端口信息</li>
     *   <li>阻塞主线程等待停机信号</li>
     * </ol>
     * 
     * @param args 命令行参数，传递给 {@link ConfigLoader}
     * @throws Exception 启动失败时抛出异常，进程退出码为 1
     */
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

    /**
     * 获取当前运行的服务器实例。
     * 
     * @return JwschServer 实例，如果未启动则为 null
     */
    public static JwschServer getServer() {
        return server;
    }

    /**
     * 获取应用版本号。
     * 
     * @return 应用版本字符串
     */
    public static String getVersion() {
        return VERSION;
    }
}
