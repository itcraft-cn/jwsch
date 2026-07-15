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
 * 入口限速Handler（L1）。
 *
 * <p>在TCP入站处限制消息速率，保护服务端不被突发流量打垮。
 * 
 * <p>使用令牌桶算法：
 * <ul>
 *   <li>每个连接独立限速</li>
 *   <li>支持突发流量（burstSize）</li>
 *   <li>超限消息被丢弃并记录指标</li>
 * </ul>
 */
public final class InboundRateLimiterHandler extends ChannelDuplexHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundRateLimiterHandler.class);
    
    private final RateLimiter rateLimiter;
    private final LongAdder droppedCount;
    private volatile boolean enabled;
    
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
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public long getDroppedCount() {
        return droppedCount.sum();
    }
    
    public void resetDroppedCount() {
        droppedCount.reset();
    }
    
    public double getCurrentRate() {
        return rateLimiter.getCurrentRate();
    }
}
