package cn.itcraft.jwsch.srv.config;

import org.junit.Test;

import static org.junit.Assert.*;

public class WebSocketServerConfigTest {
    
    @Test
    public void testDefaultValues() {
        WebSocketServerConfig config = new WebSocketServerConfig();
        assertEquals(8080, config.getPort());
        assertEquals("/ws", config.getPath());
        assertEquals(1, config.getBossThreads());
        assertEquals(0, config.getWorkerThreads());
        assertEquals(65536, config.getMaxFrameSize());
        assertTrue(config.isTcpNoDelay());
        assertTrue(config.isKeepAlive());
        assertEquals(1024, config.getSoBacklog());
    }
    
    @Test
    public void testSetPort() {
        WebSocketServerConfig config = new WebSocketServerConfig();
        config.setPort(9090);
        assertEquals(9090, config.getPort());
    }
    
    @Test
    public void testSetPath() {
        WebSocketServerConfig config = new WebSocketServerConfig();
        config.setPath("/websocket");
        assertEquals("/websocket", config.getPath());
    }
}