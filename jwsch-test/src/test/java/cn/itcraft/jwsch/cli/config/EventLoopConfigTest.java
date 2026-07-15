package cn.itcraft.jwsch.cli.config;

import org.junit.Test;

import static org.junit.Assert.*;

public class EventLoopConfigTest {
    
    @Test
    public void testDefaultValues() {
        EventLoopConfig config = new EventLoopConfig();
        assertTrue(config.isShared());
        assertTrue(config.getWorkerThreads() > 0);
    }
    
    @Test
    public void testSetShared() {
        EventLoopConfig config = new EventLoopConfig();
        config.setShared(false);
        assertFalse(config.isShared());
    }
    
    @Test
    public void testSetWorkerThreads() {
        EventLoopConfig config = new EventLoopConfig();
        config.setWorkerThreads(16);
        assertEquals(16, config.getWorkerThreads());
    }
}