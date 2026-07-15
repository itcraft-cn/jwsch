package cn.itcraft.jwsch.common.config;

import org.junit.Test;

import static org.junit.Assert.*;

public class TcpConfigTest {
    
    @Test
    public void testDefaultValues() {
        TcpConfig config = new TcpConfig();
        
        assertTrue(config.isNodelay());
        assertEquals(1048576, config.getSndbuf());
        assertEquals(1048576, config.getRcvbuf());
        assertEquals(30000, config.getConnectTimeout());
        assertEquals(0, config.getReadTimeout());
        assertEquals(0, config.getWriteTimeout());
    }
    
    @Test
    public void testSetters() {
        TcpConfig config = new TcpConfig();
        
        config.setNodelay(false);
        assertFalse(config.isNodelay());
        
        config.setSndbuf(2097152);
        assertEquals(2097152, config.getSndbuf());
        
        config.setConnectTimeout(60000);
        assertEquals(60000, config.getConnectTimeout());
    }
}