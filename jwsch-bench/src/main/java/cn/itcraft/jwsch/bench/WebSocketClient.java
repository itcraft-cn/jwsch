package cn.itcraft.jwsch.bench;

import cn.itcraft.jwsch.common.eventloop.NativeTransport;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * WebSocket 客户端封装。
 * 
 * <p>使用 Netty 实现原生 WebSocket 客户端，支持二进制帧收发。
 */
public final class WebSocketClient {
    
    private static final int MAX_FRAME_PAYLOAD_LENGTH = 512 * 1024;
    
    private final String url;
    private final EventLoopGroup group;
    private final Class<? extends SocketChannel> channelClass;
    private Channel channel;
    private WebSocketClientHandshaker handshaker;
    private Consumer<ByteBuf> messageHandler;
    private CountDownLatch handshakeLatch;
    
    public WebSocketClient(String url) {
        this.url = url;
        this.group = NativeTransport.createEventLoopGroup(1, "ws-client");
        this.channelClass = NativeTransport.getClientChannelClass();
    }
    
    public void connect() throws Exception {
        URI uri = new URI(url);
        String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
        String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        int port = uri.getPort();
        if (port == -1) {
            port = "wss".equalsIgnoreCase(scheme) ? 443 : 80;
        }
        
        handshakeLatch = new CountDownLatch(1);
        
        handshaker = WebSocketClientHandshakerFactory.newHandshaker(
            uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders(), MAX_FRAME_PAYLOAD_LENGTH);
        
        Bootstrap b = new Bootstrap();
        b.group(group)
         .channel(channelClass)
         .handler(new ChannelInitializer<SocketChannel>() {
             @Override
             protected void initChannel(SocketChannel ch) {
                 ChannelPipeline p = ch.pipeline();
                 p.addLast(new HttpClientCodec());
                 p.addLast(new HttpObjectAggregator(65536));
                 p.addLast(new WebSocketClientHandler());
             }
         });
        
        ChannelFuture future = b.connect(host, port);
        future.await(5, TimeUnit.SECONDS);
        
        if (!future.isSuccess()) {
            throw new RuntimeException("Failed to connect to " + url, future.cause());
        }
        
        channel = future.channel();
        
        handshaker.handshake(channel);
        
        if (!handshakeLatch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("WebSocket handshake timeout");
        }
        
        if (!handshaker.isHandshakeComplete()) {
            throw new RuntimeException("WebSocket handshake failed");
        }
        
        System.out.println("WebSocket connected: " + url);
    }
    
    public void sendBinary(ByteBuf data) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new BinaryWebSocketFrame(data));
        }
    }
    
    public void setMessageHandler(Consumer<ByteBuf> handler) {
        this.messageHandler = handler;
    }
    
    public void close() {
        if (channel != null) {
            channel.close();
        }
        group.shutdownGracefully(100, 300, TimeUnit.MILLISECONDS);
    }
    
    public Channel getChannel() {
        return channel;
    }
    
    private class WebSocketClientHandler extends ChannelInboundHandlerAdapter {
        
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!handshaker.isHandshakeComplete()) {
                handshaker.finishHandshake(ctx.channel(), (io.netty.handler.codec.http.FullHttpResponse) msg);
                handshakeLatch.countDown();
                return;
            }
            
            if (msg instanceof BinaryWebSocketFrame) {
                BinaryWebSocketFrame frame = (BinaryWebSocketFrame) msg;
                try {
                    ByteBuf content = frame.content();
                    if (messageHandler != null && content.readableBytes() >= 27) {
                        messageHandler.accept(content);
                    }
                } finally {
                    frame.release();
                }
            } else if (msg instanceof WebSocketFrame) {
                ((WebSocketFrame) msg).release();
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            handshakeLatch.countDown();
            ctx.close();
        }
    }
}