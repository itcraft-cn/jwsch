package cn.itcraft.jwsch.srv.integration;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MockWebSocketClientHandler extends ChannelInboundHandlerAdapter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MockWebSocketClientHandler.class);
    
    private final WebSocketClientHandshaker handshaker;
    private final AtomicReference<Boolean> handshakeSuccess;
    private final CountDownLatch handshakeLatch;
    
    private final AtomicInteger messageCount = new AtomicInteger(0);
    private final AtomicReference<String> lastMessage = new AtomicReference<>();
    private final CountDownLatch messageLatch = new CountDownLatch(1);
    
    MockWebSocketClientHandler(WebSocketClientHandshaker handshaker, 
                                AtomicReference<Boolean> handshakeSuccess, 
                                CountDownLatch handshakeLatch) {
        this.handshaker = handshaker;
        this.handshakeSuccess = handshakeSuccess;
        this.handshakeLatch = handshakeLatch;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!handshaker.isHandshakeComplete()) {
            try {
                handshaker.finishHandshake(ctx.channel(), (FullHttpResponse) msg);
                handshakeSuccess.set(true);
                handshakeLatch.countDown();
                LOGGER.debug("WebSocket handshake completed");
            } catch (Exception e) {
                LOGGER.error("WebSocket handshake failed", e);
                handshakeLatch.countDown();
            }
            return;
        }
        
        if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }
    
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) frame).text();
            lastMessage.set(text);
            messageCount.incrementAndGet();
            messageLatch.countDown();
            LOGGER.info("Received text: {}", text);
        } else if (frame instanceof BinaryWebSocketFrame) {
            byte[] bytes = new byte[frame.content().readableBytes()];
            frame.content().readBytes(bytes);
            messageCount.incrementAndGet();
            messageLatch.countDown();
            LOGGER.info("Received binary: {} bytes", bytes.length);
        } else if (frame instanceof PingWebSocketFrame) {
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
        } else if (frame instanceof CloseWebSocketFrame) {
            ctx.close();
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("WebSocket client error", cause);
        ctx.close();
    }
    
    int getMessageCount() {
        return messageCount.get();
    }
    
    String getLastMessage() {
        return lastMessage.get();
    }
    
    boolean waitForMessage(long timeout, TimeUnit unit) throws InterruptedException {
        return messageLatch.await(timeout, unit);
    }
    
    void resetMessageCount() {
        messageCount.set(0);
        lastMessage.set(null);
    }
}