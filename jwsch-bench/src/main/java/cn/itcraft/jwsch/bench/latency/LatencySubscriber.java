package cn.itcraft.jwsch.bench.latency;

import cn.itcraft.jwsch.bench.WebSocketClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

/**
 * 延迟测试订阅者。
 * 
 * <p>接收消息并计算端到端延迟。
 * 
 * <p>消息体格式：8字节nanoTime + 8字节序列号 + N字节负载。
 */
public final class LatencySubscriber {
    
    private static final byte MAGIC_0 = (byte) 0xe7;
    private static final byte MAGIC_1 = (byte) 0x34;
    private static final byte CMD_PUSH = 0x03;
    private static final byte CMD_BROADCAST = 0x04;
    private static final byte CMD_SUBSCRIBE = 0x05;
    
    private final WebSocketClient client;
    private final String topic;
    private final LatencyTracker latencyTracker;
    private final int id;
    
    public LatencySubscriber(int id, String wsUrl, String topic, LatencyTracker latencyTracker) {
        this.id = id;
        this.topic = topic;
        this.latencyTracker = latencyTracker;
        this.client = new WebSocketClient(wsUrl);
    }
    
    public void start() throws Exception {
        client.connect();
        
        client.setMessageHandler(this::handleMessage);
        
        Thread.sleep(100);
        
        if (client.getChannel() == null || !client.getChannel().isActive()) {
            throw new RuntimeException("WebSocket channel is not active for subscriber #" + id);
        }
        
        sendSubscribe();
        System.out.println("Subscriber #" + id + " connected and subscribed to " + topic);
    }
    
    private void handleMessage(ByteBuf content) {
        if (!isValidPacket(content)) {
            return;
        }
        
        recordLatency(content);
    }
    
    private void recordLatency(ByteBuf content) {
        int headerLength = content.getShort(2);
        int bodyStart = headerLength;
        
        if (content.readableBytes() < bodyStart + 16) {
            return;
        }
        
        long sendTimestamp = content.getLong(bodyStart);
        long now = System.nanoTime();
        long latencyNanos = now - sendTimestamp;
        
        if (latencyNanos > 0) {
            latencyTracker.record(latencyNanos);
        }
    }
    
    private void sendSubscribe() {
        byte[] topicBytes = topic.getBytes(StandardCharsets.US_ASCII);
        int headerLength = 27 + topicBytes.length;
        
        ByteBuf buf = Unpooled.buffer(headerLength);
        buf.writeByte(MAGIC_0);
        buf.writeByte(MAGIC_1);
        buf.writeShort(headerLength);
        buf.writeInt(0);
        buf.writeByte(CMD_SUBSCRIBE);
        buf.writeShort(0);
        buf.writeLong(0);
        buf.writeLong(0);
        buf.writeBytes(topicBytes);
        
        System.out.println("Subscriber #" + id + " sending SUBSCRIBE: topic=" + topic + ", headerLen=" + headerLength);
        client.sendBinary(buf);
    }
    
    private boolean isValidPacket(ByteBuf buf) {
        if (buf.readableBytes() < 27) {
            return false;
        }
        
        byte m0 = buf.getByte(0);
        byte m1 = buf.getByte(1);
        if (m0 != MAGIC_0 || m1 != MAGIC_1) {
            return false;
        }
        
        byte cmd = buf.getByte(8);
        return cmd == CMD_PUSH || cmd == CMD_BROADCAST;
    }
    
    public void stop() {
        client.close();
    }
}