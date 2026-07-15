package cn.itcraft.jwsch.srv.metrics;

import cn.itcraft.jwsch.srv.config.MetricsConfig;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class MetricsServerTest {
    
    private static final AtomicInteger PORT_COUNTER = new AtomicInteger(21000);
    
    private DefaultServerMetrics serverMetrics;
    private int port;
    
    @Before
    public void setUp() {
        serverMetrics = new DefaultServerMetrics();
        port = PORT_COUNTER.getAndIncrement();
    }
    
    @After
    public void tearDown() {
        serverMetrics = null;
    }
    
    @Test
    public void testStartAndShutdown() {
        MetricsConfig config = MetricsConfig.builder()
            .enabled(true)
            .port(port)
            .path("/metrics")
            .build();
        
        MetricsServer server = new MetricsServer(config, serverMetrics);
        
        assertFalse(server.isStarted());
        
        server.start();
        assertTrue(server.isStarted());
        assertEquals(port, server.getPort());
        
        server.shutdown();
        assertFalse(server.isStarted());
    }
    
    @Test
    public void testDoubleStart() {
        MetricsConfig config = MetricsConfig.builder()
            .enabled(true)
            .port(port)
            .path("/metrics")
            .build();
        
        MetricsServer server = new MetricsServer(config, serverMetrics);
        
        server.start();
        server.start();
        
        assertTrue(server.isStarted());
        
        server.shutdown();
    }
    
    @Test
    public void testDoubleShutdown() {
        MetricsConfig config = MetricsConfig.builder()
            .enabled(true)
            .port(port)
            .path("/metrics")
            .build();
        
        MetricsServer server = new MetricsServer(config, serverMetrics);
        
        server.start();
        server.shutdown();
        server.shutdown();
        
        assertFalse(server.isStarted());
    }
    
    @Test
    public void testShutdownBeforeStart() {
        MetricsConfig config = MetricsConfig.builder()
            .enabled(true)
            .port(port)
            .path("/metrics")
            .build();
        
        MetricsServer server = new MetricsServer(config, serverMetrics);
        
        server.shutdown();
        
        assertFalse(server.isStarted());
    }
    
    @Test
    public void testWithSharedEventLoop() throws InterruptedException {
        MetricsConfig config = MetricsConfig.builder()
            .enabled(true)
            .port(port)
            .path("/metrics")
            .build();
        
        EventLoopGroup bossGroup = new NioEventLoopGroup(1, 
            new DefaultThreadFactory("shared-boss", true));
        EventLoopGroup workerGroup = new NioEventLoopGroup(1, 
            new DefaultThreadFactory("shared-worker", true));
        
        try {
            MetricsServer server = new MetricsServer(config, serverMetrics, 
                bossGroup, workerGroup);
            
            server.start();
            assertTrue(server.isStarted());
            
            server.shutdown();
            assertFalse(server.isStarted());
            
            assertTrue(bossGroup.isShuttingDown() || !bossGroup.isShutdown());
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
    
    @Test
    public void testGetPort() {
        MetricsConfig config = MetricsConfig.builder()
            .enabled(true)
            .port(port)
            .path("/metrics")
            .build();
        
        MetricsServer server = new MetricsServer(config, serverMetrics);
        
        assertEquals(port, server.getPort());
    }
}