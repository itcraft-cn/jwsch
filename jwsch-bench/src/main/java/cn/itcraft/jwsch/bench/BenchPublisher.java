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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Benchmark 发布者。
 * 
 * <p>使用 TCP 连接到服务器，按指定间隔发送 PUSH 消息。
 * 消息体格式：8字节序列号 + N字节固定负载。
 * 
 * <p>优化策略：
 * <ul>
 *   <li>使用池化 Direct Buffer，避免堆内存到直接内存的拷贝</li>
 *   <li>检查通道可写状态，避免写入队列无限增长</li>
 * </ul>
 */
public final class BenchPublisher {
    
    private static final byte PAYLOAD_BYTE = (byte) 0xAB;
    
    private final TcpClient client;
    private final Channel channel;
    private final String topic;
    private final long sendIntervalMicros;
    private final int payloadSize;
    private final TpsTracker tpsTracker;
    private final ScheduledExecutorService scheduler;
    private final AtomicLong sequence = new AtomicLong(0);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final byte[] payloadTemplate;
    private final PooledByteBufAllocator allocator;
    
    public BenchPublisher(String host, int port, String topic, 
                          long sendIntervalMicros, int payloadSize, TpsTracker tpsTracker) throws Exception {
        this.topic = topic;
        this.sendIntervalMicros = sendIntervalMicros;
        this.payloadSize = payloadSize;
        this.tpsTracker = tpsTracker;
        this.payloadTemplate = new byte[payloadSize];
        for (int i = 0; i < payloadSize; i++) {
            payloadTemplate[i] = PAYLOAD_BYTE;
        }
        this.allocator = PooledByteBufAllocator.DEFAULT;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "bench-publisher-" + topic);
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
    
    public void start() {
        scheduler.scheduleAtFixedRate(
            this::sendMessage,
            0, sendIntervalMicros, TimeUnit.MICROSECONDS
        );
        System.out.println("Publisher started, sending every " + sendIntervalMicros + "μs, payload=" + payloadSize + " bytes");
    }
    
    private void sendMessage() {
        if (!running.get() || !channel.isActive()) {
            return;
        }
        
        if (!channel.isWritable()) {
            return;
        }
        
        try {
            long seq = sequence.getAndIncrement();
            ByteBuf body = createMessageBody(seq);
            
            PacketHeader header = new PacketHeader.Builder()
                .command(Command.PUSH)
                .topic(topic)
                .bodyLength(body.readableBytes())
                .build();
            
            Packet packet = new Packet(header, body);
            channel.writeAndFlush(packet);
            
            tpsTracker.increment();
        } catch (Exception e) {
            System.err.println("Send error: " + e.getMessage());
            running.set(false);
        }
    }
    
    private ByteBuf createMessageBody(long seq) {
        ByteBuf buf = allocator.directBuffer(8 + payloadSize);
        buf.writeLong(seq);
        buf.writeBytes(payloadTemplate);
        return buf;
    }
    
    public void stop() {
        running.set(false);
        
        scheduler.shutdownNow();
        try {
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                System.err.println("Publisher scheduler did not terminate in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        client.shutdown();
    }
    
    public boolean isRunning() {
        return running.get() && channel.isActive();
    }
}