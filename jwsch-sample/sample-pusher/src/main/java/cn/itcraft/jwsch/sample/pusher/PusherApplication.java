package cn.itcraft.jwsch.sample.pusher;

import cn.itcraft.jwsch.cli.client.TcpClient;
import cn.itcraft.jwsch.cli.config.ClientConfig;
import cn.itcraft.jwsch.cli.config.EventLoopConfig;
import cn.itcraft.jwsch.cli.config.TcpClientConfig;
import cn.itcraft.jwsch.sample.pusher.service.MessagePusher;
import io.netty.channel.Channel;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PusherApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(PusherApplication.class);

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9090;
    private static final String DEFAULT_TOPIC = "/topic/news";
    private static final int DEFAULT_INTERVAL = 5000;

    public static void main(String[] args) throws Exception {
        Options options = createOptions();

        try {
            CommandLine cmd = new DefaultParser().parse(options, args);

            if (cmd.hasOption("help")) {
                printHelp(options);
                return;
            }

            String host = cmd.getOptionValue("host", DEFAULT_HOST);
            int port = Integer.parseInt(cmd.getOptionValue("port", String.valueOf(DEFAULT_PORT)));
            String topic = cmd.getOptionValue("topic", DEFAULT_TOPIC);
            int interval = Integer.parseInt(cmd.getOptionValue("interval", String.valueOf(DEFAULT_INTERVAL)));
            String message = cmd.getOptionValue("message", "Hello from jwsch-pusher!");

            LOGGER.info("Starting Jwsch Pusher...");
            LOGGER.info("Target: {}:{}", host, port);
            LOGGER.info("Topic: {}", topic);
            LOGGER.info("Interval: {} ms", interval);

            TcpClient client = createClient();
            client.start();

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "sample-pusher");
                t.setDaemon(true);
                return t;
            });

            MessagePusher pusher = new MessagePusher(null, topic, message, scheduler);

            final String finalHost = host;
            final int finalPort = port;

            Runnable connectTask = () -> {
                try {
                    if (pusher.getChannel() == null || !pusher.getChannel().isActive()) {
                        LOGGER.info("Connecting to {}:{}", finalHost, finalPort);
                        Channel channel = client.connect(finalHost, finalPort);
                        pusher.setChannel(channel);
                        LOGGER.info("Connected to {}:{}", finalHost, finalPort);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to connect: {}", e.getMessage());
                }
            };

            Runnable pushTask = pusher::push;

            scheduler.scheduleWithFixedDelay(connectTask, 0, 5, TimeUnit.SECONDS);
            // scheduler.scheduleAtFixedRate(pushTask, 1_000L, interval / 1000, TimeUnit.MILLISECONDS);
            scheduler.scheduleAtFixedRate(pushTask, 1_000_000L, interval / 1000, TimeUnit.MICROSECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Shutting down...");
                pusher.stop();
                scheduler.shutdown();
                client.shutdown();
            }));

            Thread.currentThread().join();

        } catch (Exception e) {
            LOGGER.error("Error running pusher", e);
            printHelp(options);
            System.exit(1);
        }
    }

    private static Options createOptions() {
        Options options = new Options();
        options.addOption("h", "host", true, "Server host (default: localhost)");
        options.addOption("p", "port", true, "Server port (default: 9090)");
        options.addOption("t", "topic", true, "Topic to push to (default: /topic/news)");
        options.addOption("i", "interval", true, "Push interval in ms (default: 5000)");
        options.addOption("m", "message", true, "Message to push (default: Hello from jwsch-pusher!)");
        options.addOption(null, "help", false, "Print help");
        return options;
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar sample-pusher.jar", options);
    }

    private static TcpClient createClient() {
        ClientConfig config = new ClientConfig();
        config.setEnabled(true);

        EventLoopConfig eventLoopConfig = config.getEventLoopConfig();
        eventLoopConfig.setShared(false);
        eventLoopConfig.setWorkerThreads(1);

        TcpClientConfig tcpConfig = config.getTcpConfig();
        tcpConfig.setConnectTimeout(5000);
        tcpConfig.setNodelay(true);
        tcpConfig.setKeepalive(true);

        return new TcpClient(config);
    }
}
