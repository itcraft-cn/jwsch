package cn.itcraft.jwsch.srv.cluster;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClusterConnectionRegistryTest {
    
    private ClusterConnectionRegistry registry;
    
    @Before
    public void setUp() {
        registry = new ClusterConnectionRegistry("local-node");
    }
    
    @Test
    public void testAddLocalConnection() {
        ConnectionMeta meta = new ConnectionMeta(1L, "WEBSOCKET", "127.0.0.1:12345");
        registry.addLocalConnection(1L, meta);
        
        assertTrue(registry.isLocal(1L));
        assertEquals(1, registry.getLocalConnectionCount());
        assertNull(registry.getNodeAddress(1L));
    }
    
    @Test
    public void testRemoveLocalConnection() {
        ConnectionMeta meta = new ConnectionMeta(1L, "WEBSOCKET", "127.0.0.1:12345");
        registry.addLocalConnection(1L, meta);
        registry.removeLocalConnection(1L);
        
        assertFalse(registry.isLocal(1L));
        assertEquals(0, registry.getLocalConnectionCount());
    }
    
    @Test
    public void testAddRemoteConnection() {
        ConnectionMeta meta = new ConnectionMeta(2L, "WEBSOCKET", "127.0.0.1:12346");
        RemoteConnection remote = new RemoteConnection(2L, "node-02", "192.168.1.2:9090", meta);
        registry.addRemoteConnection(2L, remote);
        
        assertFalse(registry.isLocal(2L));
        assertEquals("node-02", registry.getNodeId(2L));
        assertEquals("192.168.1.2:9090", registry.getNodeAddress(2L));
        assertEquals(1, registry.getRemoteConnectionCount());
    }
    
    @Test
    public void testRemoveRemoteConnection() {
        ConnectionMeta meta = new ConnectionMeta(2L, "WEBSOCKET", "127.0.0.1:12346");
        RemoteConnection remote = new RemoteConnection(2L, "node-02", "192.168.1.2:9090", meta);
        registry.addRemoteConnection(2L, remote);
        registry.removeRemoteConnection(2L);
        
        assertNull(registry.getNodeId(2L));
        assertEquals(0, registry.getRemoteConnectionCount());
    }
    
    @Test
    public void testRemoveNodeConnections() {
        ConnectionMeta meta1 = new ConnectionMeta(2L, "WEBSOCKET", "127.0.0.1:12346");
        ConnectionMeta meta2 = new ConnectionMeta(3L, "WEBSOCKET", "127.0.0.1:12347");
        registry.addRemoteConnection(2L, new RemoteConnection(2L, "node-02", "192.168.1.2:9090", meta1));
        registry.addRemoteConnection(3L, new RemoteConnection(3L, "node-02", "192.168.1.2:9090", meta2));
        
        registry.removeNodeConnections("node-02");
        
        assertEquals(0, registry.getRemoteConnectionCount());
    }
    
    @Test
    public void testGetTotalConnectionCount() {
        ConnectionMeta localMeta = new ConnectionMeta(1L, "WEBSOCKET", "127.0.0.1:12345");
        ConnectionMeta remoteMeta = new ConnectionMeta(2L, "WEBSOCKET", "127.0.0.1:12346");
        
        registry.addLocalConnection(1L, localMeta);
        registry.addRemoteConnection(2L, new RemoteConnection(2L, "node-02", "192.168.1.2:9090", remoteMeta));
        
        assertEquals(2, registry.getTotalConnectionCount());
    }
    
    @Test
    public void testClear() {
        ConnectionMeta meta = new ConnectionMeta(1L, "WEBSOCKET", "127.0.0.1:12345");
        registry.addLocalConnection(1L, meta);
        registry.clear();
        
        assertEquals(0, registry.getTotalConnectionCount());
    }
}