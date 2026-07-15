package cn.itcraft.jwsch.cli.client;

import cn.itcraft.jwsch.cli.config.ClientConfig;
import cn.itcraft.jwsch.cli.config.EventLoopConfig;
import cn.itcraft.jwsch.cli.config.TcpClientConfig;
import cn.itcraft.jwsch.common.eventloop.SharedEventLoopManager;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class TcpClientTest {
    
    private TcpClient client;
    private ClientConfig config;
    
    @Before
    public void setUp() {
        config = new ClientConfig();
        EventLoopConfig eventLoopConfig = new EventLoopConfig();
        eventLoopConfig.setShared(true);
        eventLoopConfig.setWorkerThreads(2);
        config.setEventLoopConfig(eventLoopConfig);
        
        TcpClientConfig tcpConfig = new TcpClientConfig();
        tcpConfig.setConnectTimeout(5000);
        config.setTcpConfig(tcpConfig);
    }
    
    @After
    public void tearDown() {
        if (client != null) {
            client.shutdown();
        }
        while (SharedEventLoopManager.getClientInstance().isInitialized()) {
            SharedEventLoopManager.getClientInstance().release();
        }
    }
    
    @Test
    public void testStartWithSharedEventLoop() {
        client = new TcpClient(config);
        client.start();
        
        assertTrue(SharedEventLoopManager.getClientInstance().isInitialized());
    }
    
    @Test
    public void testStartWithDedicatedEventLoop() {
        config.getEventLoopConfig().setShared(false);
        config.getEventLoopConfig().setWorkerThreads(2);
        
        client = new TcpClient(config);
        client.start();
        
        assertFalse(SharedEventLoopManager.getClientInstance().isInitialized());
    }
    
    @Test
    public void testShutdownSharedEventLoop() {
        client = new TcpClient(config);
        client.start();
        
        assertTrue(SharedEventLoopManager.getClientInstance().isInitialized());
        
        client.shutdown();
        client = null;
        
        assertFalse(SharedEventLoopManager.getClientInstance().isInitialized());
    }
    
    @Test
    public void testMultipleClientsSharedEventLoop() {
        TcpClient client1 = new TcpClient(config);
        TcpClient client2 = new TcpClient(config);
        
        client1.start();
        client2.start();
        
        assertTrue(SharedEventLoopManager.getClientInstance().isInitialized());
        
        client1.shutdown();
        assertTrue(SharedEventLoopManager.getClientInstance().isInitialized());
        
        client2.shutdown();
        assertFalse(SharedEventLoopManager.getClientInstance().isInitialized());
    }
    
    @Test(expected = RuntimeException.class)
    public void testConnectToInvalidHost() throws InterruptedException {
        client = new TcpClient(config);
        client.start();
        
        client.connect("invalid.host.that.does.not.exist", 12345);
    }
    
    @Test
    public void testDoubleShutdown() {
        client = new TcpClient(config);
        client.start();
        client.shutdown();
        client.shutdown();
        client = null;
    }
}