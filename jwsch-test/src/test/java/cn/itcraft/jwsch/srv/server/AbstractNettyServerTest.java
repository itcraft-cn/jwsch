package cn.itcraft.jwsch.srv.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class AbstractNettyServerTest {
    
    private static final AtomicInteger PORT_COUNTER = new AtomicInteger(20000);
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private TestNettyServer server;
    private int port;
    
    @Before
    public void setUp() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(1);
        port = PORT_COUNTER.getAndIncrement();
        server = new TestNettyServer("TestServer", bossGroup, workerGroup, port);
    }
    
    @After
    public void tearDown() {
        if (server != null) {
            server.shutdown();
        }
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
    
    @Test
    public void testConstructor() {
        assertEquals("TestServer", server.getName());
        assertFalse(server.isStarted());
    }
    
    @Test
    public void testIsStartedBeforeStart() {
        assertFalse(server.isStarted());
    }
    
    @Test
    public void testGetName() {
        assertEquals("TestServer", server.getName());
    }
    
    @Test
    public void testGetPortBeforeStart() {
        assertEquals(-1, server.getPort());
    }
    
    @Test
    public void testGetPortAfterStart() {
        server.start();
        assertEquals(port, server.getPort());
    }
    
    @Test
    public void testIsActiveBeforeStart() {
        assertFalse(server.isActive());
    }
    
    @Test
    public void testIsActiveAfterStart() {
        server.start();
        assertTrue(server.isActive());
    }
    
    @Test
    public void testShutdownBeforeStart() {
        server.shutdown();
        assertFalse(server.isStarted());
    }
    
    @Test
    public void testShutdownAfterStart() {
        server.start();
        assertTrue(server.isStarted());
        
        server.shutdown();
        assertFalse(server.isStarted());
        assertFalse(server.isActive());
    }
    
    @Test
    public void testDoubleShutdown() {
        server.start();
        server.shutdown();
        server.shutdown();
        
        assertFalse(server.isStarted());
    }
    
    private static class TestNettyServer extends AbstractNettyServer {
        
        private final int port;
        
        TestNettyServer(String name, EventLoopGroup bossGroup, EventLoopGroup workerGroup, int port) {
            super(name, bossGroup, workerGroup);
            this.port = port;
        }
        
        @Override
        public void start() {
            if (started) {
                return;
            }
            
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                        }
                    });
                
                ChannelFuture future = bootstrap.bind(port).sync();
                serverChannel = future.channel();
                started = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to start server", e);
            }
        }
    }
}