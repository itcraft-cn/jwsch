package cn.itcraft.jwsch.srv.router;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 背压管理器。
 * 
 * <p>管理 TCP 后端通道与 WebSocket 前端通道之间的背压：
 * <ul>
 *   <li>当一定比例的前端通道不可写时，禁用所有 TCP 通道的 AUTO_READ</li>
 *   <li>当所有前端通道可写且经过冷却时间后，重新启用 TCP 通道的 AUTO_READ</li>
 * </ul>
 * 
 * <p>这种机制确保当订阅者处理能力不足时，发布者会自然降速，
 * 避免服务端内存无限增长导致 OOM。
 * 
 * <h3>磁滞回线设计</h3>
 * 
 * <p>触发阈值（20%）与释放阈值（5%）形成磁滞回线，避免背压振荡：
 * <ul>
 *   <li>触发：非可写订阅者比例 >= 20% 时激活背压</li>
 *   <li>释放：非可写订阅者比例 <= 5% 且冷却期（500ms）后释放</li>
 * </ul>
 * 
 * <h3>工作原理</h3>
 * <pre>
 * Publisher (TCP) → Server → Subscribers (WebSocket × N)
 *                        ↑
 *                    背压控制点
 * 
 * 当非可写订阅者比例 >= 20%：
 * 1. 禁用 TCP 通道 AUTO_READ
 * 2. TCP 通道停止读取，发布者发送缓冲区填满
 * 3. 发布者自然降速
 * 
 * 释放条件：
 * 1. 非可写订阅者比例 <= 5%
 * 2. 距离激活时间超过冷却期（500ms）
 * </pre>
 */
public final class BackpressureManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BackpressureManager.class);
    
    /**
     * 非可写订阅者比例触发阈值。
     * 
     * <p>设为 0.2 表示 20% 订阅者不可写才触发背压。
     */
    private static final double TRIGGER_THRESHOLD = 0.2;
    
    /**
     * 非可写订阅者比例释放阈值。
     * 
     * <p>设为 0.05 表示 5% 订阅者不可写时才能释放背压。
     * 与触发阈值形成磁滞回线，避免背压振荡。
     */
    private static final double RELEASE_THRESHOLD = 0.05;
    
    /**
     * 背压释放冷却时间（毫秒）。
     * 
     * <p>背压激活后必须等待此时间才能释放，
     * 避免缓冲区快速排空导致的瞬时释放。
     */
    private static final long RELEASE_COOLDOWN_MS = 500;
    
    private final Set<Channel> tcpChannels = ConcurrentHashMap.newKeySet();
    private final Set<Channel> frontendChannels = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean autoReadDisabled = new AtomicBoolean(false);
    private final AtomicInteger nonWritableCount = new AtomicInteger(0);
    private volatile long lastActivateTime = 0;
    private volatile EventLoop scheduledEventLoop = null;
    
    public void registerTcpChannel(Channel channel) {
        tcpChannels.add(channel);
        LOGGER.debug("TCP channel registered: {}", channel);
    }
    
    public void unregisterTcpChannel(Channel channel) {
        tcpChannels.remove(channel);
        LOGGER.debug("TCP channel unregistered: {}", channel);
    }
    
    public void registerFrontendChannel(Channel channel) {
        frontendChannels.add(channel);
        if (!channel.isWritable()) {
            nonWritableCount.incrementAndGet();
        }
        LOGGER.debug("Frontend channel registered: writable={}, nonWritableCount={}", 
            channel.isWritable(), nonWritableCount.get());
    }
    
    public void unregisterFrontendChannel(Channel channel) {
        frontendChannels.remove(channel);
        if (!channel.isWritable()) {
            nonWritableCount.decrementAndGet();
        }
        LOGGER.debug("Frontend channel unregistered: nonWritableCount={}", nonWritableCount.get());
        
        scheduleReleaseCheck(channel.eventLoop());
    }
    
    /**
     * 当前端通道可写状态变化时调用。
     * 
     * <p>由 WebSocketHandler 的 channelWritabilityChanged 事件触发。
     * 
     * @param channel 状态变化的通道
     */
    public void onFrontendWritabilityChanged(Channel channel) {
        boolean writable = channel.isWritable();
        
        if (writable) {
            int count = nonWritableCount.decrementAndGet();
            LOGGER.debug("Frontend writable: nonWritableCount={}", count);
            scheduleReleaseCheck(channel.eventLoop());
        } else {
            int count = nonWritableCount.incrementAndGet();
            LOGGER.debug("Frontend non-writable: nonWritableCount={}", count);
            checkAndDisableAutoRead();
        }
    }
    
    private void checkAndDisableAutoRead() {
        int total = frontendChannels.size();
        if (total == 0) {
            return;
        }
        
        int nonWritable = nonWritableCount.get();
        double ratio = nonWritable / (double) total;
        
        if (ratio >= TRIGGER_THRESHOLD && !autoReadDisabled.get()) {
            disableAutoReadOnAllTcpChannels();
        }
    }
    
    private void disableAutoReadOnAllTcpChannels() {
        if (autoReadDisabled.compareAndSet(false, true)) {
            lastActivateTime = System.currentTimeMillis();
            scheduledEventLoop = null;
            
            int disabledCount = 0;
            for (Channel tcpChannel : tcpChannels) {
                if (tcpChannel.isActive() && tcpChannel.config().isAutoRead()) {
                    tcpChannel.config().setAutoRead(false);
                    disabledCount++;
                }
            }
            
            int nonWritable = nonWritableCount.get();
            int total = frontendChannels.size();
            LOGGER.info("Backpressure activated: disabled AUTO_READ on {} TCP channels, " +
                "nonWritable={}/{} ({}%)", disabledCount, nonWritable, total, 
                (int)(100 * nonWritable / (double) total));
        }
    }
    
    private void scheduleReleaseCheck(EventLoop eventLoop) {
        if (!autoReadDisabled.get()) {
            return;
        }
        
        if (scheduledEventLoop != null && scheduledEventLoop == eventLoop) {
            return;
        }
        
        scheduledEventLoop = eventLoop;
        long elapsed = System.currentTimeMillis() - lastActivateTime;
        long delay = Math.max(0, RELEASE_COOLDOWN_MS - elapsed);
        
        eventLoop.schedule(() -> {
            tryReleaseAutoRead();
        }, delay, TimeUnit.MILLISECONDS);
        
        LOGGER.debug("Scheduled release check: delay={}ms, elapsed={}ms", delay, elapsed);
    }
    
    private void tryReleaseAutoRead() {
        if (!autoReadDisabled.get()) {
            return;
        }
        
        int nonWritable = nonWritableCount.get();
        int total = frontendChannels.size();
        
        if (total == 0) {
            return;
        }
        
        double ratio = nonWritable / (double) total;
        
        if (ratio > RELEASE_THRESHOLD) {
            LOGGER.debug("Cannot release: ratio={} > threshold={}", ratio, RELEASE_THRESHOLD);
            scheduledEventLoop = null;
            return;
        }
        
        long elapsed = System.currentTimeMillis() - lastActivateTime;
        if (elapsed < RELEASE_COOLDOWN_MS) {
            LOGGER.debug("Cannot release: cooldown not elapsed ({}ms < {}ms)", 
                elapsed, RELEASE_COOLDOWN_MS);
            return;
        }
        
        if (autoReadDisabled.compareAndSet(true, false)) {
            int enabledCount = 0;
            for (Channel tcpChannel : tcpChannels) {
                if (tcpChannel.isActive() && !tcpChannel.config().isAutoRead()) {
                    tcpChannel.config().setAutoRead(true);
                    enabledCount++;
                }
            }
            
            LOGGER.info("Backpressure released: enabled AUTO_READ on {} TCP channels, " +
                "nonWritable={}/{}, cooldown={}ms", enabledCount, nonWritable, total, elapsed);
        }
        
        scheduledEventLoop = null;
    }
    
    public int getTcpChannelCount() {
        return tcpChannels.size();
    }
    
    public int getFrontendChannelCount() {
        return frontendChannels.size();
    }
    
    public int getNonWritableCount() {
        return nonWritableCount.get();
    }
    
    public boolean isAutoReadDisabled() {
        return autoReadDisabled.get();
    }
    
    public void clear() {
        tcpChannels.clear();
        frontendChannels.clear();
        autoReadDisabled.set(false);
        nonWritableCount.set(0);
        lastActivateTime = 0;
        scheduledEventLoop = null;
    }
}
