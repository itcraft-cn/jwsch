package cn.itcraft.jwsch.srv.config;

import org.junit.Test;

import static org.junit.Assert.*;

public class ServerConfigTest {
    
    @Test
    public void testDefaultValues() {
        ServerConfig config = new ServerConfig();
        assertTrue(config.isEnabled());
        assertNotNull(config.getWebSocketConfig());
        assertNotNull(config.getTcpConfig());
    }
    
    @Test
    public void testSetEnabled() {
        ServerConfig config = new ServerConfig();
        config.setEnabled(false);
        assertFalse(config.isEnabled());
    }
}