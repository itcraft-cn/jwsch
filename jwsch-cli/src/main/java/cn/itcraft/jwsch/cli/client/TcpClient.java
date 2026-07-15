package cn.itcraft.jwsch.cli.client;

import cn.itcraft.jwsch.cli.config.ClientConfig;
import cn.itcraft.jwsch.cli.config.TcpClientConfig;
import cn.itcraft.jwsch.cli.selector.NodeSelector;
import cn.itcraft.jwsch.cli.selector.PrioritySelector;
import cn.itcraft.jwsch.cli.selector.RandomSelector;
import cn.itcraft.jwsch.cli.selector.RoundRobinSelector;
import cn.itcraft.jwsch.cli.selector.SingleSelector;
import cn.itcraft.jwsch.common.eventloop.NativeTransport;
import cn.itcraft.jwsch.common.eventloop.SharedEventLoopManager;
import cn.itcraft.jwsch.common.config.WriteBufferWaterMark;
import cn.itcraft.jwsch.common.ssl.SslConfig;
import cn.itcraft.jwsch.common.ssl.SslContextFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * TCP client with cluster support.
 *
 * <p>Supports two EventLoop modes:
 * <ul>
 *   <li>Shared mode: multiple clients share one EventLoopGroup</li>
 *   <li>Dedicated mode: each client creates its own EventLoopGroup</li>
 * </ul>
 *
 * <p>Cluster mode: when nodes are configured in TcpClientConfig,
 * the client expands nodes x basePort..(basePort+portRange-1) into
 * a list of addresses, then uses a NodeSelector to pick one.
 * On disconnect, automatic reconnect is scheduled.
 *
 * <p>Usage (single node):
 * <pre>
 * ClientConfig config = new ClientConfig();
 * TcpClient client = new TcpClient(config);
 * client.start();
 * Channel ch = client.connect("localhost", 9090);
 * </pre>
 *
 * <p>Usage (cluster):
 * <pre>
 * TcpClientConfig tcpConfig = config.getTcpConfig();
 * tcpConfig.setNodes(Arrays.asList("server1", "server2"));
 * tcpConfig.setBasePort(9090);
 * tcpConfig.setPortRange(2);
 * tcpConfig.setSelectorType("round-robin");
 * TcpClient client = new TcpClient(config);
 * client.start();
 * Channel ch = client.connectCluster();
 * </pre>
 */
public final class TcpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(TcpClient.class);

    private static final String DEDICATED_THREAD_PREFIX = "jwsch-tcp-client-worker";
    private static final int SHUTDOWN_QUIET_PERIOD_MS = 100;
    private static final int SHUTDOWN_TIMEOUT_MS = 300;
    private static final int CONNECT_TIMEOUT_SECONDS = 30;

    private final ClientConfig config;
    private final boolean sharedEventLoop;
    private final SslContext sslContext;
    private EventLoopGroup workerGroup;
    private Bootstrap bootstrap;

    private List<InetSocketAddress> addresses = new ArrayList<>();
    private NodeSelector nodeSelector = new SingleSelector();
    private volatile Channel activeChannel;
    private volatile boolean reconnectEnabled;
    private ScheduledFuture<?> reconnectTask;

    public TcpClient(ClientConfig config) {
        this.config = config;
        this.sharedEventLoop = config.getEventLoopConfig().isShared();
        this.sslContext = initSslContext(config.getTcpConfig().getSslConfig());
    }

    private SslContext initSslContext(SslConfig sslConfig) {
        if (sslConfig == null || !sslConfig.isEnabled()) {
            return null;
        }

        try {
            SslContext context = SslContextFactory.createClientContext(sslConfig);
            LOGGER.info("SSL enabled for TcpClient");
            return context;
        } catch (Exception e) {
            LOGGER.error("Failed to initialize SSL context", e);
            throw new RuntimeException("Failed to initialize SSL context", e);
        }
    }

    /**
     * Start the client.
     *
     * <p>Initializes EventLoop and Bootstrap. If cluster mode is configured,
     * resolves node addresses and creates the NodeSelector.
     */
    public void start() {
        if (sharedEventLoop) {
            workerGroup = SharedEventLoopManager.getClientInstance().acquireWorkerGroup();
            LOGGER.info("Using shared EventLoopGroup");
        } else {
            int threads = config.getEventLoopConfig().getWorkerThreads();
            workerGroup = NativeTransport.createEventLoopGroup(threads, DEDICATED_THREAD_PREFIX);
            LOGGER.info("Created dedicated EventLoopGroup with {} threads, prefix={}, transport={}",
                threads, DEDICATED_THREAD_PREFIX, NativeTransport.getTransportType());
        }

        TcpClientConfig tcpConfig = config.getTcpConfig();

        Class<? extends SocketChannel> channelClass = NativeTransport.getClientChannelClass();

        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
            .channel(channelClass)
            .option(ChannelOption.TCP_NODELAY, tcpConfig.isNodelay())
            .option(ChannelOption.SO_KEEPALIVE, tcpConfig.isKeepalive())
            .option(ChannelOption.SO_SNDBUF, tcpConfig.getSndbuf())
            .option(ChannelOption.SO_RCVBUF, tcpConfig.getRcvbuf())
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, tcpConfig.getConnectTimeout())
            .option(ChannelOption.ALLOCATOR,
                new io.netty.buffer.PooledByteBufAllocator(true));

        WriteBufferWaterMark waterMark = tcpConfig.getWriteBufferWaterMark();
        bootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK,
            new io.netty.channel.WriteBufferWaterMark(waterMark.getLow(), waterMark.getHigh()));

        bootstrap.handler(new TcpClientInitializer(sslContext, config.getTcpConfig()));

        if (tcpConfig.isClusterEnabled()) {
            this.addresses = resolveAddresses(tcpConfig);
            this.nodeSelector = createNodeSelector(tcpConfig.getSelectorType());
            LOGGER.info("Cluster mode enabled: {} addresses, selector={}",
                addresses.size(), tcpConfig.getSelectorType());
        }

        String protocol = sslContext != null ? "TLS" : "TCP";
        LOGGER.info("TcpClient started ({})", protocol);
    }

    /**
     * Resolve cluster node addresses from config.
     *
     * <p>Expands nodes x [basePort .. basePort+portRange-1] into
     * a flat list of InetSocketAddress. Base-port addresses come first
     * to support PrioritySelector's preference.
     */
    List<InetSocketAddress> resolveAddresses(TcpClientConfig tcpConfig) {
        List<String> nodes = tcpConfig.getNodes();
        int basePort = tcpConfig.getBasePort();
        int portRange = tcpConfig.getPortRange();

        List<InetSocketAddress> result = new ArrayList<>(nodes.size() * portRange);

        for (String node : nodes) {
            String host = node.trim();
            for (int offset = 0; offset < portRange; offset++) {
                result.add(new InetSocketAddress(host, basePort + offset));
            }
        }

        return result;
    }

    /**
     * Create NodeSelector based on selector type string.
     */
    NodeSelector createNodeSelector(String selectorType) {
        if (selectorType == null) {
            return new SingleSelector();
        }

        switch (selectorType.toLowerCase()) {
            case "random":
                return new RandomSelector();
            case "round-robin":
                return new RoundRobinSelector();
            case "priority":
                return new PrioritySelector();
            case "single":
            default:
                return new SingleSelector();
        }
    }

    /**
     * Connect to a specific server (single-node mode).
     *
     * @param host server address
     * @param port server port
     * @return connected Channel
     * @throws InterruptedException if connection is interrupted
     * @throws RuntimeException if connection fails
     */
    public Channel connect(String host, int port) throws InterruptedException {
        ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));
        future.await(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!future.isSuccess()) {
            throw new RuntimeException("Failed to connect to " + host + ":" + port, future.cause());
        }

        String protocol = sslContext != null ? "TLS" : "TCP";
        LOGGER.info("Connected to {}:{} ({})", host, port, protocol);
        return future.channel();
    }

    /**
     * Connect to cluster using NodeSelector (cluster mode).
     *
     * <p>Selects an address via NodeSelector, connects, and sets up
     * automatic reconnect on channel close. Reconnect is retried
     * with the configured delay until shutdown.
     *
     * @return connected Channel
     * @throws InterruptedException if connection is interrupted
     * @throws RuntimeException if all addresses fail
     */
    public Channel connectCluster() throws InterruptedException {
        if (addresses.isEmpty()) {
            throw new IllegalStateException("No cluster addresses configured. Set nodes in TcpClientConfig.");
        }

        this.reconnectEnabled = true;
        return doConnectCluster();
    }

    private Channel doConnectCluster() throws InterruptedException {
        InetSocketAddress selected = nodeSelector.select(addresses);
        if (selected == null) {
            throw new RuntimeException("NodeSelector returned null, no available address");
        }

        try {
            ChannelFuture future = bootstrap.connect(selected);
            future.await(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (future.isSuccess()) {
                Channel ch = future.channel();
                this.activeChannel = ch;
                nodeSelector.onConnectSuccess(selected);

                ch.closeFuture().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (reconnectEnabled) {
                            nodeSelector.onConnectFailed(selected);
                            scheduleReconnect();
                        }
                    }
                });

                String protocol = sslContext != null ? "TLS" : "TCP";
                LOGGER.info("Cluster connected to {}:{} ({})", selected.getHostString(), selected.getPort(), protocol);
                return ch;
            }

            nodeSelector.onConnectFailed(selected);
            LOGGER.warn("Failed to connect to {}:{}, trying next node", selected.getHostString(), selected.getPort());

            InetSocketAddress next = nodeSelector.select(addresses);
            if (next != null && !next.equals(selected)) {
                return doConnectCluster();
            }

            throw new RuntimeException("Failed to connect to any cluster node", future.cause());
        } catch (InterruptedException e) {
            throw e;
        }
    }

    /**
     * Schedule a reconnect attempt after the configured delay.
     */
    private void scheduleReconnect() {
        if (!reconnectEnabled || workerGroup == null || workerGroup.isShuttingDown()) {
            return;
        }

        int delaySeconds = config.getTcpConfig().getReconnectDelaySeconds();
        LOGGER.info("Scheduling reconnect in {} seconds", delaySeconds);

        reconnectTask = workerGroup.schedule(new Runnable() {
            @Override
            public void run() {
                if (!reconnectEnabled) {
                    return;
                }

                try {
                    doConnectCluster();
                } catch (Exception e) {
                    LOGGER.warn("Reconnect failed, will retry: {}", e.getMessage());
                    scheduleReconnect();
                }
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * Get the active cluster channel.
     *
     * @return active Channel, or null if not connected
     */
    public Channel getActiveChannel() {
        return activeChannel;
    }

    /**
     * Get the resolved cluster addresses.
     *
     * @return unmodifiable list of addresses
     */
    public List<InetSocketAddress> getAddresses() {
        return addresses;
    }

    /**
     * Get the NodeSelector.
     */
    public NodeSelector getNodeSelector() {
        return nodeSelector;
    }

    /**
     * Shutdown the client.
     *
     * <p>Disables reconnect, cancels scheduled tasks, closes the active channel,
     * then releases or shuts down the EventLoopGroup.
     */
    public void shutdown() {
        this.reconnectEnabled = false;

        if (reconnectTask != null) {
            reconnectTask.cancel(false);
            reconnectTask = null;
        }

        if (activeChannel != null && activeChannel.isActive()) {
            activeChannel.close();
            activeChannel = null;
        }

        if (sharedEventLoop) {
            SharedEventLoopManager.getClientInstance().release();
            LOGGER.info("Released shared EventLoopGroup");
        } else if (workerGroup != null) {
            workerGroup.shutdownGracefully(SHUTDOWN_QUIET_PERIOD_MS,
                SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            LOGGER.info("Shutdown dedicated EventLoopGroup");
        }
    }

    public boolean isSslEnabled() {
        return sslContext != null;
    }
}