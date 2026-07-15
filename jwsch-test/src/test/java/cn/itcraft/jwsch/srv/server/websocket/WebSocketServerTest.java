package cn.itcraft.jwsch.srv.server.websocket;

import cn.itcraft.jwsch.srv.config.WebSocketConfig;
import cn.itcraft.jwsch.srv.router.PacketRouter;
import cn.itcraft.jwsch.srv.router.TopicSubscription;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WebSocketServerTest {
    
    private static final AtomicInteger PORT_COUNTER = new AtomicInteger(22000);
    
    private PacketRouter packetRouter;
    private WebSocketConfig config;
    private int port;
    
    @Before
    public void setUp() {
        packetRouter = mock(PacketRouter.class);
        TopicSubscription topicSubscription = mock(TopicSubscription.class);
        when(packetRouter.getTopicSubscription()).thenReturn(topicSubscription);
        
        port = PORT_COUNTER.getAndIncrement();
        config = WebSocketConfig.builder()
            .port(port)
            .path("/ws")
            .build();
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void testStartAndShutdown() {
        WebSocketServer server = new WebSocketServer(config, packetRouter);
        
        assertFalse(server.isStarted());
        
        server.start();
        assertTrue(server.isStarted());
        assertEquals(port, server.getPort());
        
        server.shutdown();
        assertFalse(server.isStarted());
    }
    
    @Test
    public void testDoubleStart() {
        WebSocketServer server = new WebSocketServer(config, packetRouter);
        
        server.start();
        server.start();
        
        assertTrue(server.isStarted());
        
        server.shutdown();
    }
    
    @Test
    public void testDoubleShutdown() {
        WebSocketServer server = new WebSocketServer(config, packetRouter);
        
        server.start();
        server.shutdown();
        server.shutdown();
        
        assertFalse(server.isStarted());
    }
    
    @Test
    public void testShutdownBeforeStart() {
        WebSocketServer server = new WebSocketServer(config, packetRouter);
        
        server.shutdown();
        
        assertFalse(server.isStarted());
    }
    
    @Test
    public void testWithSharedEventLoop() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1, 
            new DefaultThreadFactory("shared-ws-boss", true));
        EventLoopGroup workerGroup = new NioEventLoopGroup(1, 
            new DefaultThreadFactory("shared-ws-worker", true));
        
        try {
            WebSocketServer server = new WebSocketServer(config, packetRouter, 
                null, 0, bossGroup, workerGroup);
            
            server.start();
            assertTrue(server.isStarted());
            
            server.shutdown();
            assertFalse(server.isStarted());
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
    
    @Test
    public void testIsSslEnabled() {
        WebSocketServer server = new WebSocketServer(config, packetRouter);
        
        assertFalse(server.isSslEnabled());
        
        server.shutdown();
    }
    
    @Test
    public void testGetPort() {
        WebSocketServer server = new WebSocketServer(config, packetRouter);
        
        assertEquals(port, server.getPort());
        
        server.shutdown();
    }
}