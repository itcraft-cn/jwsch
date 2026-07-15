package cn.itcraft.jwsch.srv.integration;

import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketDecoder;
import cn.itcraft.jwsch.common.protocol.PacketEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MockTcpServer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MockTcpServer.class);
    
    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean started = false;
    
    private final AtomicInteger packetCount = new AtomicInteger(0);
    private final AtomicReference<Packet> lastPacket = new AtomicReference<>();
    private final CountDownLatch packetLatch = new CountDownLatch(1);
    
    public MockTcpServer(int port) {
        this.port = port;
    }
    
    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(1);
        
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 1024)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("idleState", new IdleStateHandler(180, 0, 0, TimeUnit.SECONDS));
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
                        10 * 1024 * 1024, 0, 4, 0, 4));
                    pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                    pipeline.addLast("decoder", new PacketDecoder());
                    pipeline.addLast("encoder", new PacketEncoder());
                    pipeline.addLast("handler", new MockTcpServerHandler(packetCount, lastPacket, packetLatch));
                }
            });
        
        serverChannel = bootstrap.bind(port).sync().channel();
        started = true;
        
        LOGGER.info("MockTcpServer started on port {}", port);
    }
    
    public void stop() {
        if (!started) {
            return;
        }
        
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        
        started = false;
        LOGGER.info("MockTcpServer stopped");
    }
    
    public int getPacketCount() {
        return packetCount.get();
    }
    
    public Packet getLastPacket() {
        return lastPacket.get();
    }
    
    public boolean waitForPacket(long timeout, TimeUnit unit) throws InterruptedException {
        return packetLatch.await(timeout, unit);
    }
    
    public boolean isStarted() {
        return started;
    }
    
    public int getPort() {
        return port;
    }
}

class MockTcpServerHandler extends SimpleChannelInboundHandler<Packet> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MockTcpServerHandler.class);
    
    private final AtomicInteger packetCount;
    private final AtomicReference<Packet> lastPacket;
    private final CountDownLatch packetLatch;
    
    MockTcpServerHandler(AtomicInteger packetCount, AtomicReference<Packet> lastPacket, CountDownLatch packetLatch) {
        this.packetCount = packetCount;
        this.lastPacket = lastPacket;
        this.packetLatch = packetLatch;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet msg) {
        packetCount.incrementAndGet();
        lastPacket.set(msg);
        packetLatch.countDown();
        LOGGER.debug("Received packet: cmd={}", msg.getHeader().getCommand());
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof io.netty.handler.timeout.IdleStateEvent) {
            ctx.close();
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("MockTcpServer error", cause);
        ctx.close();
    }
}