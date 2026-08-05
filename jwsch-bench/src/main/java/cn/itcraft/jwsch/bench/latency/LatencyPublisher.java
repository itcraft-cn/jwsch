package cn.itcraft.jwsch.bench.latency;

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
 * 延迟测试发布者。
 * 
 * <p>发送带时间戳的消息，用于测量端到端延迟。
 * 
 * <p>消息体格式：8字节nanoTime + 8字节序列号 + N字节负载。
 */
public final class LatencyPublisher {
    
    private static final byte PAYLOAD_BYTE = (byte) 0xAB;
    
    private final TcpClient client;
    private final Channel channel;
    private final String topic;
    private final long sendIntervalMicros;
    private final int payloadSize;
    private final ScheduledExecutorService scheduler;
    private final AtomicLong sequence = new AtomicLong(0);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final byte[] payloadTemplate;
    private final PooledByteBufAllocator allocator;
    private final AtomicLong sendCount = new AtomicLong(0);
    
    /**
     * 创建延迟测试发布者实例。
     * 
     * @param host                目标主机
     * @param port                目标端口
     * @param topic               发布主题
     * @param sendIntervalMicros  发送间隔（微秒）
     * @param payloadSize         负载大小（字节）
     * @throws Exception 如果连接失败或初始化失败
     */
    public LatencyPublisher(String host, int port, String topic, 
                            long sendIntervalMicros, int payloadSize) throws Exception {
        this.topic = topic;
        this.sendIntervalMicros = sendIntervalMicros;
        this.payloadSize = payloadSize;
        this.payloadTemplate = new byte[payloadSize];
        for (int i = 0; i < payloadSize; i++) {
            payloadTemplate[i] = PAYLOAD_BYTE;
        }
        this.allocator = PooledByteBufAllocator.DEFAULT;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "latency-publisher");
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
    
    /**
     * 启动发布者，开始按指定间隔发送消息。
     */
    public void start() {
        scheduler.scheduleAtFixedRate(
            this::sendMessage,
            0, sendIntervalMicros, TimeUnit.MICROSECONDS
        );
        System.out.println("Publisher started, sending every " + sendIntervalMicros + "μs, payload=" + payloadSize + " bytes");
    }
    
    /**
     * 发送带时间戳的消息。
     */
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
            
            sendCount.incrementAndGet();
        } catch (Exception e) {
            System.err.println("Send error: " + e.getMessage());
            running.set(false);
        }
    }
    
    /**
     * 创建消息体，包含时间戳、序列号和负载。
     * 
     * @param seq 消息序列号
     * @return 包含消息体的 ByteBuf
     */
    private ByteBuf createMessageBody(long seq) {
        ByteBuf buf = allocator.directBuffer(16 + payloadSize);
        buf.writeLong(System.nanoTime());
        buf.writeLong(seq);
        buf.writeBytes(payloadTemplate);
        return buf;
    }
    
    /**
     * 停止发布者，关闭调度器和客户端连接。
     */
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
    
    /**
     * 检查发布者是否正在运行。
     * 
     * @return true 如果正在运行且通道活跃
     */
    public boolean isRunning() {
        return running.get() && channel.isActive();
    }
    
    /**
     * 获取已发送消息总数。
     * 
     * @return 已发送消息数
     */
    public long getSendCount() {
        return sendCount.get();
    }
}