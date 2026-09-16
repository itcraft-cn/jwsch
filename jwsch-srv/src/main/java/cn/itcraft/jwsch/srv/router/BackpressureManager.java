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
    private final AtomicBoolean releaseCheckScheduled = new AtomicBoolean(false);
    private final AtomicInteger nonWritableCount = new AtomicInteger(0);
    private volatile long lastActivateNanos = 0;
    
    /**
     * 最近一次发布释放检查所使用的 EventLoop，仅作为重试通道的备选，
     * 调度去重由 releaseCheckScheduled 保证，此字段不作为同步状态使用。
     */
    private volatile EventLoop scheduledEventLoop = null;
    
    /**
     * 注册 TCP 通道（发布者通道）。
     *
     * @param channel TCP 通道
     */
    public void registerTcpChannel(Channel channel) {
        tcpChannels.add(channel);
        LOGGER.debug("TCP channel registered: {}", channel);
    }
    
    /**
     * 注销 TCP 通道。
     *
     * @param channel TCP 通道
     */
    public void unregisterTcpChannel(Channel channel) {
        tcpChannels.remove(channel);
        LOGGER.debug("TCP channel unregistered: {}", channel);
    }
    
    /**
     * 注册前端通道（订阅者通道）。
     *
     * <p>注册时会检查通道的可写状态并更新不可写计数。
     *
     * @param channel 前端通道
     */
    public void registerFrontendChannel(Channel channel) {
        frontendChannels.add(channel);
        if (!channel.isWritable()) {
            nonWritableCount.incrementAndGet();
        }
        LOGGER.debug("Frontend channel registered: writable={}, nonWritableCount={}", 
            channel.isWritable(), nonWritableCount.get());
        checkAndDisableAutoRead();
    }
    
    /**
     * 注销前端通道。
     *
     * <p>注销时会更新不可写计数，并安排一次背压释放检查。
     *
     * @param channel 前端通道
     */
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
            lastActivateNanos = System.nanoTime();
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
        if (!autoReadDisabled.get() || eventLoop == null) {
            return;
        }
        
        if (releaseCheckScheduled.compareAndSet(false, true)) {
            scheduledEventLoop = eventLoop;
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastActivateNanos);
            long delay = Math.max(0, RELEASE_COOLDOWN_MS - elapsedMs);
            
            eventLoop.schedule(() -> {
                tryReleaseAutoRead();
            }, delay, TimeUnit.MILLISECONDS);
            
            LOGGER.debug("Scheduled release check: delay={}ms, elapsed={}ms", delay, elapsedMs);
        }
    }
    
    private void tryReleaseAutoRead() {
        if (!autoReadDisabled.get()) {
            releaseCheckScheduled.set(false);
            return;
        }
        
        int nonWritable = nonWritableCount.get();
        int total = frontendChannels.size();
        
        if (total == 0) {
            releaseCheckScheduled.set(false);
            return;
        }
        
        double ratio = nonWritable / (double) total;
        
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastActivateNanos);
        
        if (elapsed < RELEASE_COOLDOWN_MS || ratio > RELEASE_THRESHOLD) {
            scheduleRetryReleaseCheck(total, nonWritable, ratio, elapsed);
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
        
        releaseCheckScheduled.set(false);
        scheduledEventLoop = null;
    }
    
    private void scheduleRetryReleaseCheck(int total, int nonWritable, double ratio, long elapsed) {
        LOGGER.debug("Release check deferred: nonWritable={}/{}, ratio={}, elapsed={}ms",
            nonWritable, total, ratio, elapsed);
        
        if (autoReadDisabled.get()) {
            EventLoop loop = scheduledEventLoop;
            if (loop != null && loop.isShuttingDown()) {
                scheduledEventLoop = null;
            }
            
            EventLoop retryLoop = loop != null && !loop.isShuttingDown() ? loop : null;
            
            if (retryLoop == null) {
                onRetryLoopMissing(total, nonWritable, ratio);
                return;
            }
            
            retryLoop.schedule(this::tryReleaseAutoRead, RELEASE_COOLDOWN_MS, TimeUnit.MILLISECONDS);
        }
    }
    
    private void onRetryLoopMissing(int total, int nonWritable, double ratio) {
        releaseCheckScheduled.set(false);
        LOGGER.debug("No event loop available for release retry: nonWritable={}/{} ({}%)",
            nonWritable, total, (int) (100 * ratio));
    }
    
    /**
     * 获取已注册的 TCP 通道数量。
     *
     * @return TCP 通道数量
     */
    public int getTcpChannelCount() {
        return tcpChannels.size();
    }
    
    /**
     * 获取已注册的前端通道数量。
     *
     * @return 前端通道数量
     */
    public int getFrontendChannelCount() {
        return frontendChannels.size();
    }
    
    /**
     * 获取不可写的前端通道数量。
     *
     * @return 不可写通道数量
     */
    public int getNonWritableCount() {
        return nonWritableCount.get();
    }
    
    /**
     * 检查背压是否激活（即 AUTO_READ 是否被禁用）。
     *
     * @return 背压是否激活
     */
    public boolean isAutoReadDisabled() {
        return autoReadDisabled.get();
    }
    
    /**
     * 清空所有通道并重置背压状态。
     * 
     * <p>用于服务器关闭或重置场景。
     */
    public void clear() {
        tcpChannels.clear();
        frontendChannels.clear();
        autoReadDisabled.set(false);
        nonWritableCount.set(0);
        releaseCheckScheduled.set(false);
        scheduledEventLoop = null;
        lastActivateNanos = 0;
    }
}
