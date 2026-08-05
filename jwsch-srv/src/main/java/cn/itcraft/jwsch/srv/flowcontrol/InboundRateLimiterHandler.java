package cn.itcraft.jwsch.srv.flowcontrol;

import cn.itcraft.jwsch.common.flowcontrol.FlowControlConfig;
import cn.itcraft.jwsch.common.flowcontrol.RateLimiter;
import cn.itcraft.jwsch.common.protocol.Packet;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.LongAdder;

/**
 * Inbound rate limiter handler (L1).
 *
 * <p>Limits message rate at TCP inbound to protect the server from burst traffic.
 * 
 * <p>Uses token bucket algorithm:
 * <ul>
 *   <li>Independent rate limiting per connection</li>
 *   <li>Supports burst traffic (burstSize)</li>
 *   <li>Excess messages are dropped and metrics recorded</li>
 * </ul>
 *
 * <p>This is the first layer (L1) in the three-layer flow control system.
 * When enabled, it checks every incoming packet against the rate limiter.
 */
public final class InboundRateLimiterHandler extends ChannelDuplexHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundRateLimiterHandler.class);
    
    private final RateLimiter rateLimiter;
    private final LongAdder droppedCount;
    private volatile boolean enabled;
    
    /**
     * Creates an InboundRateLimiterHandler with the specified configuration.
     *
     * @param config the flow control configuration
     */
    public InboundRateLimiterHandler(FlowControlConfig config) {
        this.enabled = config.isInboundEnabled();
        this.rateLimiter = RateLimiter.create(
            config.getMaxTokensPerSecond(), 
            config.getBurstSize()
        );
        this.droppedCount = new LongAdder();
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!enabled) {
            super.channelRead(ctx, msg);
            return;
        }
        
        if (msg instanceof Packet) {
            if (rateLimiter.tryAcquire()) {
                super.channelRead(ctx, msg);
            } else {
                droppedCount.increment();
                Packet packet = (Packet) msg;
                LOGGER.info("L1 rate limit exceeded, packet dropped: cmd={}, topic={}, totalDropped={}", 
                    packet.getCommand(), packet.getTopic(), droppedCount.sum());
                ctx.fireExceptionCaught(new RateLimitExceededException("Inbound rate limit exceeded"));
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }
    
    /**
     * Enables or disables the rate limiter.
     *
     * @param enabled true to enable rate limiting, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Returns whether the rate limiter is currently enabled.
     *
     * @return true if rate limiting is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Returns the total count of dropped packets.
     *
     * @return the number of packets dropped due to rate limiting
     */
    public long getDroppedCount() {
        return droppedCount.sum();
    }
    
    /**
     * Resets the dropped packet counter to zero.
     */
    public void resetDroppedCount() {
        droppedCount.reset();
    }
    
    /**
     * Returns the current rate limit in tokens per second.
     *
     * @return the current rate limit (tokens/sec)
     */
    public double getCurrentRate() {
        return rateLimiter.getCurrentRate();
    }
}
