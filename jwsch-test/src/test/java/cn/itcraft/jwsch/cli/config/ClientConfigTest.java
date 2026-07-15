package cn.itcraft.jwsch.cli.config;

import org.junit.Test;

import static org.junit.Assert.*;

public class ClientConfigTest {
    
    @Test
    public void testDefaultValues() {
        ClientConfig config = new ClientConfig();
        assertTrue(config.isEnabled());
        assertNotNull(config.getEventLoopConfig());
        assertNotNull(config.getTcpConfig());
    }
    
    @Test
    public void testSetEnabled() {
        ClientConfig config = new ClientConfig();
        config.setEnabled(false);
        assertFalse(config.isEnabled());
    }
    
    @Test
    public void testSetEventLoopConfig() {
        ClientConfig config = new ClientConfig();
        EventLoopConfig eventLoopConfig = new EventLoopConfig();
        eventLoopConfig.setShared(false);
        eventLoopConfig.setWorkerThreads(8);
        
        config.setEventLoopConfig(eventLoopConfig);
        
        assertNotNull(config.getEventLoopConfig());
        assertFalse(config.getEventLoopConfig().isShared());
        assertEquals(8, config.getEventLoopConfig().getWorkerThreads());
    }
    
    @Test
    public void testSetTcpConfig() {
        ClientConfig config = new ClientConfig();
        TcpClientConfig tcpConfig = new TcpClientConfig();
        tcpConfig.setKeepalive(false);
        tcpConfig.setConnectTimeout(5000);
        
        config.setTcpConfig(tcpConfig);
        
        assertNotNull(config.getTcpConfig());
        assertFalse(config.getTcpConfig().isKeepalive());
        assertEquals(5000, config.getTcpConfig().getConnectTimeout());
    }
}