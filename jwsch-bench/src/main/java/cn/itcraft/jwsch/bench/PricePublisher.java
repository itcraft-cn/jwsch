package cn.itcraft.jwsch.bench;

import cn.itcraft.jwsch.cli.client.TcpClient;
import cn.itcraft.jwsch.cli.config.ClientConfig;
import cn.itcraft.jwsch.cli.config.EventLoopConfig;
import cn.itcraft.jwsch.cli.config.TcpClientConfig;
import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketHeader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * EURUSD价格发布者。
 * 
 * 发送外汇价格数据到服务器，用于浏览器实时展示。
 */
public final class PricePublisher {
    
    private static final String TOPIC = "/topic/price";
    
    private final TcpClient client;
    private final Channel channel;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final PooledByteBufAllocator allocator;
    private final Random random = new Random();
    
    private double baseBid = 1.08500;
    private double spread = 0.00002;
    
    public PricePublisher(String host, int port) throws Exception {
        this.allocator = PooledByteBufAllocator.DEFAULT;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "price-publisher");
            t.setDaemon(true);
            return t;
        });
        
        ClientConfig config = new ClientConfig();
        config.setEnabled(true);
        
        EventLoopConfig eventLoopConfig = config.getEventLoopConfig();
        eventLoopConfig.setShared(false);
        eventLoopConfig.setWorkerThreads(1);
        
        TcpClientConfig tcpConfig = config.getTcpConfig();
        tcpConfig.setConnectTimeout(5000);
        tcpConfig.setNodelay(true);
        tcpConfig.setKeepalive(true);
        
        this.client = new TcpClient(config);
        this.client.start();
        
        this.channel = client.connect(host, port);
    }
    
    public void start(int intervalMs) {
        scheduler.scheduleAtFixedRate(
            this::sendPrice,
            0, intervalMs, TimeUnit.MILLISECONDS
        );
        System.out.println("[PRICE] Publisher started, interval=" + intervalMs + "ms, topic=" + TOPIC);
    }
    
    private void sendPrice() {
        if (!running.get() || !channel.isActive()) {
            return;
        }
        
        if (!channel.isWritable()) {
            return;
        }
        
        try {
            double bid = generateBid();
            double ask = bid + spread;
            String json = String.format("{\"s\":\"EURUSD\",\"b\":%.5f,\"a\":%.5f}", bid, ask);
            
            byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
            ByteBuf body = allocator.directBuffer(jsonBytes.length);
            body.writeBytes(jsonBytes);
            
            PacketHeader header = new PacketHeader.Builder()
                .command(Command.PUSH)
                .topic(TOPIC)
                .bodyLength(body.readableBytes())
                .build();
            
            Packet packet = new Packet(header, body);
            channel.writeAndFlush(packet);
            
        } catch (Exception e) {
            System.err.println("[PRICE] Send error: " + e.getMessage());
        }
    }
    
    private double generateBid() {
        double delta = (random.nextDouble() - 0.5) * 0.00010;
        baseBid += delta;
        if (baseBid < 1.07000) baseBid = 1.07000;
        if (baseBid > 1.10000) baseBid = 1.10000;
        return Math.round(baseBid * 100000.0) / 100000.0;
    }
    
    public void stop() {
        running.set(false);
        scheduler.shutdownNow();
        client.shutdown();
    }
}
