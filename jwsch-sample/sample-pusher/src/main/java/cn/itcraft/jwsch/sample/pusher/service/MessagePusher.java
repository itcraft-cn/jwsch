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
    
    public MessagePusher(Channel channel, String topic, String message, ScheduledExecutorService scheduler) {
        this.channel = channel;
        this.topic = topic;
        this.message = message;
        this.scheduler = scheduler;
        this.messageCount = 0;
    }
    
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
    
    public void setChannel(Channel channel) {
        this.channel = channel;
    }
    
    public Channel getChannel() {
        return channel;
    }
    
    public void stop() {
        running = false;
    }
    
    public int getMessageCount() {
        return messageCount;
    }
}