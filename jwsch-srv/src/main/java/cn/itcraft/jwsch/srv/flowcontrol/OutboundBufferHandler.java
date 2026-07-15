package cn.itcraft.jwsch.srv.flowcontrol;

import cn.itcraft.jwsch.common.flowcontrol.FlowControlConfig;
import cn.itcraft.jwsch.common.flowcontrol.OverflowStrategy;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

/**
 * 出站缓冲Handler。
 *
 * <p>实现L3堆积溢出控制：
 * <ul>
 *   <li>当Channel不可写时，消息入队缓冲</li>
 *   <li>队列满时根据OverflowStrategy处理</li>
 *   <li>Channel恢复可写时，排空队列</li>
 * </ul>
 *
 * <p>溢出策略：
 * <ul>
 *   <li>DROP_OLDEST: 丢弃最旧消息</li>
 *   <li>DROP_NEWEST: 丢弃新消息</li>
 *   <li>DISCONNECT: 断开慢消费者</li>
 *   <li>DROP_OLDEST_THEN_DISCONNECT: 先丢弃旧，超极限后断开</li>
 * </ul>
 */
public class OutboundBufferHandler extends ChannelDuplexHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboundBufferHandler.class);
    
    private final int maxQueueSize;
    private final int disconnectThreshold;
    private final OverflowStrategy overflowStrategy;
    private final ConcurrentLinkedQueue<ByteBuf> pendingQueue;
    private final LongAdder dropCount;
    private final LongAdder enqueueCount;
    private final LongAdder drainCount;
    private final LongAdder disconnectCount;
    
    public OutboundBufferHandler(FlowControlConfig config) {
        this.maxQueueSize = config.getMaxQueueSize();
        this.disconnectThreshold = config.getDisconnectThreshold();
        this.overflowStrategy = config.getOutboundOverflowStrategy();
        this.pendingQueue = new ConcurrentLinkedQueue<>();
        this.dropCount = new LongAdder();
        this.enqueueCount = new LongAdder();
        this.drainCount = new LongAdder();
        this.disconnectCount = new LongAdder();
    }
    
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof BinaryWebSocketFrame)) {
            ctx.write(msg, promise);
            return;
        }
        
        BinaryWebSocketFrame frame = (BinaryWebSocketFrame) msg;
        ByteBuf content = frame.content();
        
        if (ctx.channel().isWritable() && pendingQueue.isEmpty()) {
            ctx.write(msg, promise);
            return;
        }
        
        if (ctx.channel().isWritable()) {
            drainQueue(ctx);
            ctx.write(msg, promise);
            return;
        }
        
        int queueSize = pendingQueue.size();
        if (queueSize >= maxQueueSize) {
            handleOverflow(ctx, content, promise);
        } else {
            enqueueBuffer(content, promise);
        }
        
        ReferenceCountUtil.release(frame);
    }
    
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isWritable()) {
            drainQueue(ctx);
        }
        ctx.fireChannelWritabilityChanged();
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ByteBuf buf;
        while ((buf = pendingQueue.poll()) != null) {
            buf.release();
        }
        ctx.fireChannelInactive();
    }
    
    private void drainQueue(ChannelHandlerContext ctx) {
        ByteBuf buf;
        int drained = 0;
        
        while (ctx.channel().isWritable() && (buf = pendingQueue.poll()) != null) {
            ctx.write(new BinaryWebSocketFrame(buf));
            drained++;
        }
        
        if (drained > 0) {
            ctx.flush();
            drainCount.add(drained);
            LOGGER.debug("Drained {} buffers from queue, remaining={}", drained, pendingQueue.size());
        }
    }
    
    private void handleOverflow(ChannelHandlerContext ctx, ByteBuf content, ChannelPromise promise) {
        dropCount.increment();
        int queueSize = pendingQueue.size();
        
        switch (overflowStrategy) {
            case DROP_OLDEST:
                dropOldestAndEnqueue(content);
                ReferenceCountUtil.release(content);
                promise.setSuccess();
                break;
                
            case DROP_NEWEST:
                ReferenceCountUtil.release(content);
                promise.setSuccess();
                break;
                
            case DISCONNECT:
                ReferenceCountUtil.release(content);
                disconnectCount.increment();
                ctx.close();
                promise.setSuccess();
                break;
                
            case DROP_OLDEST_THEN_DISCONNECT:
                if (queueSize >= disconnectThreshold) {
                    ReferenceCountUtil.release(content);
                    disconnectCount.increment();
                    ctx.close();
                } else {
                    dropOldestAndEnqueue(content);
                }
                promise.setSuccess();
                break;
        }
        
        LOGGER.warn("Outbound overflow: strategy={}, queueSize={}, dropCount={}, disconnectCount={}",
            overflowStrategy, queueSize, dropCount.sum(), disconnectCount.sum());
    }
    
    private void dropOldestAndEnqueue(ByteBuf newBuf) {
        ByteBuf oldest = pendingQueue.poll();
        if (oldest != null) {
            oldest.release();
        }
        ByteBuf retained = newBuf.retainedSlice();
        pendingQueue.offer(retained);
    }
    
    private void enqueueBuffer(ByteBuf content, ChannelPromise promise) {
        ByteBuf retained = content.retainedSlice();
        pendingQueue.offer(retained);
        enqueueCount.increment();
        promise.setSuccess();
        
        LOGGER.debug("Enqueued buffer: queueSize={}", pendingQueue.size());
    }
    
    public int getQueueSize() {
        return pendingQueue.size();
    }
    
    public long getDropCount() {
        return dropCount.sum();
    }
    
    public long getEnqueueCount() {
        return enqueueCount.sum();
    }
    
    public long getDrainCount() {
        return drainCount.sum();
    }
    
    public long getDisconnectCount() {
        return disconnectCount.sum();
    }
}