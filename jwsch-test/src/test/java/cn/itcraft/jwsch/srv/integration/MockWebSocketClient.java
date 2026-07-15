package cn.itcraft.jwsch.srv.integration;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MockWebSocketClient {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MockWebSocketClient.class);
    
    private final String host;
    private final int port;
    private final String path;
    private EventLoopGroup group;
    private Channel channel;
    private volatile boolean connected = false;
    private MockWebSocketClientHandler handler;
    
    public MockWebSocketClient(String host, int port, String path) {
        this.host = host;
        this.port = port;
        this.path = path;
    }
    
    public boolean connect() throws Exception {
        group = new NioEventLoopGroup(1);
        
        URI uri = new URI("ws://" + host + ":" + port + path);
        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
            uri, WebSocketVersion.V13, null, false, null);
        
        AtomicReference<Boolean> success = new AtomicReference<>(false);
        CountDownLatch latch = new CountDownLatch(1);
        handler = new MockWebSocketClientHandler(handshaker, success, latch);
        
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("httpCodec", new HttpClientCodec());
                    pipeline.addLast("httpAggregator", new HttpObjectAggregator(65536));
                    pipeline.addLast("webSocketHandler", handler);
                }
            });
        
        channel = bootstrap.connect(host, port).sync().channel();
        
        if (!latch.await(5, TimeUnit.SECONDS)) {
            LOGGER.error("WebSocket handshake timeout");
            disconnect();
            return false;
        }
        
        connected = success.get();
        if (connected) {
            LOGGER.info("WebSocket client connected to {}:{}", host, port);
        }
        
        return connected;
    }
    
    public void disconnect() {
        connected = false;
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        LOGGER.info("WebSocket client disconnected");
    }
    
    public void sendText(String text) {
        if (!connected || channel == null) {
            LOGGER.warn("Client not connected");
            return;
        }
        channel.writeAndFlush(new TextWebSocketFrame(text));
        LOGGER.debug("Sent text: {}", text);
    }
    
    public void sendBinary(byte[] data) {
        if (!connected || channel == null) {
            LOGGER.warn("Client not connected");
            return;
        }
        channel.writeAndFlush(new BinaryWebSocketFrame(
            channel.alloc().buffer(data.length).writeBytes(data)));
        LOGGER.debug("Sent binary: {} bytes", data.length);
    }
    
    public boolean isConnected() {
        return connected && channel != null && channel.isActive();
    }
    
    public int getMessageCount() {
        return handler != null ? handler.getMessageCount() : 0;
    }
    
    public String getLastMessage() {
        return handler != null ? handler.getLastMessage() : null;
    }
    
    public boolean waitForMessage(long timeout, TimeUnit unit) throws InterruptedException {
        return handler != null && handler.waitForMessage(timeout, unit);
    }
}