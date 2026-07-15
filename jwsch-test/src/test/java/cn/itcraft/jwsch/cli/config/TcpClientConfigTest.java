package cn.itcraft.jwsch.cli.config;

import cn.itcraft.jwsch.common.config.WriteBufferWaterMark;
import org.junit.Test;

import static org.junit.Assert.*;

public class TcpClientConfigTest {
    
    @Test
    public void testDefaultValues() {
        TcpClientConfig config = new TcpClientConfig();
        assertTrue(config.isKeepalive());
        assertTrue(config.isNodelay());
        assertTrue(config.getConnectTimeout() > 0);
    }
    
    @Test
    public void testSetKeepalive() {
        TcpClientConfig config = new TcpClientConfig();
        config.setKeepalive(false);
        assertFalse(config.isKeepalive());
    }
    
    @Test
    public void testInheritedProperties() {
        TcpClientConfig config = new TcpClientConfig();
        config.setConnectTimeout(5000);
        config.setNodelay(true);
        config.setSndbuf(65536);
        config.setRcvbuf(65536);
        
        assertEquals(5000, config.getConnectTimeout());
        assertTrue(config.isNodelay());
        assertEquals(65536, config.getSndbuf());
        assertEquals(65536, config.getRcvbuf());
    }
    
    @Test
    public void testWriteBufferWaterMark() {
        TcpClientConfig config = new TcpClientConfig();
        WriteBufferWaterMark waterMark = new WriteBufferWaterMark(32768, 65536);
        config.setWriteBufferWaterMark(waterMark);
        
        assertNotNull(config.getWriteBufferWaterMark());
        assertEquals(32768, config.getWriteBufferWaterMark().getLow());
        assertEquals(65536, config.getWriteBufferWaterMark().getHigh());
    }
}