/**
 * 消息推送器服务类。
 * 
 * <p>负责将消息封装为 jwsch 协议包并推送到指定的主题。
 * 支持 JSON 格式消息，包含序列号、主题、内容和时间戳。
 * 
 * <p>线程安全设计：
 * <ul>
 *   <li>使用 volatile 确保 channel 引用的可见性</li>
 *   <li>push() 方法从局部变量读取 channel，避免多线程下的空指针异常</li>
 *   <li>消息计数器使用 int 类型，仅在单线程中递增</li>
 * </ul>
 * 
 * @author itcraft
 * @since 1.0
 */
package cn.itcraft.jwsch.sample.pusher.service;

import cn.itcraft.jwsch.common.id.IdGenerator;
import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketHeader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MessagePusher {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagePusher.class);
    
    private final String topic;
    private final String message;
    private final ScheduledExecutorService scheduler;
    private volatile Channel channel;
    private volatile boolean running = true;
    private int messageCount;
    
    /**
     * 创建消息推送器实例。
     * 
     * @param channel    TCP 通道（可后续设置）
     * @param topic      推送主题
     * @param message    消息内容
     * @param scheduler  调度器
     */
    public MessagePusher(Channel channel, String topic, String message, ScheduledExecutorService scheduler) {
        this.channel = channel;
        this.topic = topic;
        this.message = message;
        this.scheduler = scheduler;
        this.messageCount = 0;
    }
    
    /**
     * 推送消息到服务器。
     * 
     * <p>将消息封装为 JSON 格式并创建 PUSH 命令包发送。
     * 如果通道不活跃则跳过发送。
     */
    public void push() {
        if (!running) {
            return;
        }
        
        Channel ch = this.channel;
        if (ch == null || !ch.isActive()) {
            LOGGER.warn("Channel is not active, waiting for reconnect...");
            return;
        }
        
        messageCount++;
        
        String jsonMessage = String.format(
            "{\"seq\":%d,\"topic\":\"%s\",\"content\":\"%s\",\"timestamp\":%d}",
            messageCount, topic, message, System.currentTimeMillis()
        );
        
        ByteBuf bodyBuf = Unpooled.copiedBuffer(jsonMessage, StandardCharsets.UTF_8);
        
        long sourceId = IdGenerator.nextId();
        
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.PUSH)
            .errorCode((short) 0)
            .sourceId(sourceId)
            .targetId(0L)
            .topic(topic)
            .bodyLength(bodyBuf.readableBytes())
            .build();
        
        Packet packet = new Packet(header, bodyBuf);
        
        ChannelFuture future = ch.writeAndFlush(packet);
        future.addListener(f -> {
            if (f.isSuccess()) {
                LOGGER.info("Pushed message #{} to topic: {}, seq={}", 
                    messageCount, topic, sourceId);
            } else {
                LOGGER.error("Failed to push message: {}", f.cause().getMessage());
            }
        });
    }
    
    /**
     * 设置 TCP 通道。
     * 
     * @param channel TCP 通道
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }
    
    /**
     * 获取当前 TCP 通道。
     * 
     * @return TCP 通道，可能为 null
     */
    public Channel getChannel() {
        return channel;
    }
    
    /**
     * 停止推送器。
     */
    public void stop() {
        running = false;
    }
    
    /**
     * 获取已推送消息计数。
     * 
     * @return 消息计数
     */
    public int getMessageCount() {
        return messageCount;
    }
}