package cn.itcraft.jwsch.srv;

import cn.itcraft.jwsch.srv.config.JwschConfig;
import cn.itcraft.jwsch.srv.config.TcpConfig;
import cn.itcraft.jwsch.srv.config.WebSocketConfig;
import cn.itcraft.jwsch.srv.loadbalance.LoadBalance;
import cn.itcraft.jwsch.srv.registry.ServiceRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class JwschServerTest {
    
    private JwschServer server;
    
    @Before
    public void setUp() {
        server = new JwschServer();
    }
    
    @After
    public void tearDown() {
        server.shutdown();
    }
    
    @Test
    public void testGetServiceRegistry() {
        ServiceRegistry registry = server.getServiceRegistry();
        assertNotNull(registry);
    }
    
    @Test
    public void testGetLoadBalance() {
        LoadBalance loadBalance = server.getLoadBalance();
        assertNotNull(loadBalance);
    }
    
    @Test
    public void testNotStartedInitially() {
        assertFalse(server.isStarted());
    }
    
    @Test
    public void testDisabledServer() {
        JwschConfig config = JwschConfig.builder()
            .enabled(false)
            .build();
        
        JwschServer disabledServer = new JwschServer(config);
        disabledServer.start();
        
        assertFalse(disabledServer.isStarted());
    }
    
    @Test
    public void testDoubleStart() {
        JwschConfig config = JwschConfig.builder()
            .webSocket(WebSocketConfig.builder()
                .port(18081)
                .build())
            .tcp(TcpConfig.builder()
                .port(19090)
                .build())
            .build();
        
        JwschServer server1 = new JwschServer(config);
        server1.start();
        server1.start();
        
        assertTrue(server1.isStarted());
        
        server1.shutdown();
    }
    
    @Test
    public void testDoubleShutdown() {
        server.start();
        server.shutdown();
        server.shutdown();
        
        assertFalse(server.isStarted());
    }
}