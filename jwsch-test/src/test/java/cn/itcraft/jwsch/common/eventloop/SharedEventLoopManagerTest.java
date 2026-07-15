package cn.itcraft.jwsch.common.eventloop;

import io.netty.channel.EventLoopGroup;
import org.junit.Test;

import static org.junit.Assert.*;

public class SharedEventLoopManagerTest {
    
    @Test
    public void testGetInstance() {
        SharedEventLoopManager manager1 = SharedEventLoopManager.getClientInstance();
        SharedEventLoopManager manager2 = SharedEventLoopManager.getClientInstance();
        assertSame("Client instance should be singleton", manager1, manager2);
    }
    
    @Test
    public void testServerMode() {
        SharedEventLoopManager server = SharedEventLoopManager.createServer(1, 2);
        
        assertNotNull("Boss group should not be null", server.getBossGroup());
        assertNotNull("Worker group should not be null", server.getWorkerGroup());
        assertTrue("Should be initialized", server.isInitialized());
        assertEquals("Boss thread count", 1, server.getBossThreadCount());
        assertEquals("Worker thread count", 2, server.getWorkerThreadCount());
        
        server.shutdown();
        assertTrue("Should be shutdown", server.isShutdown());
    }
    
    @Test
    public void testClientModeRefCount() {
        SharedEventLoopManager client = SharedEventLoopManager.getClientInstance();
        
        int initial = client.getRefCount();
        
        client.acquireWorkerGroup();
        assertEquals("After 1st acquire", initial + 1, client.getRefCount());
        
        client.acquireWorkerGroup();
        assertEquals("After 2nd acquire", initial + 2, client.getRefCount());
        
        client.release();
        assertEquals("After 1st release", initial + 1, client.getRefCount());
        
        client.release();
        assertEquals("After 2nd release", initial, client.getRefCount());
        
        if (initial == 0) {
            assertFalse("Should be uninitialized", client.isInitialized());
        }
    }
}