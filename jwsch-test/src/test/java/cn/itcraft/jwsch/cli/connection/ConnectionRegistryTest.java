package cn.itcraft.jwsch.cli.connection;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ConnectionRegistryTest {
    
    private ConnectionRegistry registry;
    
    @Before
    public void setUp() {
        registry = new ConnectionRegistry();
    }
    
    @Test
    public void testRegister() {
        ConnectionInfo info = createConnectionInfo(1L, "192.168.1.1:8080", ConnectionType.FRONTEND);
        
        registry.register(info);
        
        assertEquals(1, registry.getConnectionCount());
        assertTrue(registry.contains(1L));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNull() {
        registry.register(null);
    }
    
    @Test
    public void testUnregister() {
        ConnectionInfo info = createConnectionInfo(1L, "192.168.1.1:8080", ConnectionType.FRONTEND);
        registry.register(info);
        
        ConnectionInfo removed = registry.unregister(1L);
        
        assertEquals(info, removed);
        assertEquals(0, registry.getConnectionCount());
        assertFalse(registry.contains(1L));
    }
    
    @Test
    public void testUnregisterNonExistent() {
        ConnectionInfo removed = registry.unregister(999L);
        assertNull(removed);
    }
    
    @Test
    public void testLookup() {
        ConnectionInfo info = createConnectionInfo(1L, "192.168.1.1:8080", ConnectionType.FRONTEND);
        registry.register(info);
        
        ConnectionInfo found = registry.lookup(1L);
        
        assertEquals(info, found);
    }
    
    @Test
    public void testLookupNonExistent() {
        ConnectionInfo found = registry.lookup(999L);
        assertNull(found);
    }
    
    @Test
    public void testLookupAll() {
        registry.register(createConnectionInfo(1L, "192.168.1.1:8080", ConnectionType.FRONTEND));
        registry.register(createConnectionInfo(2L, "192.168.1.2:8080", ConnectionType.BACKEND));
        
        List<ConnectionInfo> all = registry.lookupAll();
        
        assertEquals(2, all.size());
    }
    
    @Test
    public void testLookupByType() {
        registry.register(createConnectionInfo(1L, "192.168.1.1:8080", ConnectionType.FRONTEND));
        registry.register(createConnectionInfo(2L, "192.168.1.2:8080", ConnectionType.BACKEND));
        registry.register(createConnectionInfo(3L, "192.168.1.3:8080", ConnectionType.FRONTEND));
        
        List<ConnectionInfo> frontend = registry.lookupByType(ConnectionType.FRONTEND);
        List<ConnectionInfo> backend = registry.lookupByType(ConnectionType.BACKEND);
        
        assertEquals(2, frontend.size());
        assertEquals(1, backend.size());
    }
    
    @Test
    public void testLookupByRemoteAddress() {
        registry.register(createConnectionInfo(1L, "192.168.1.1:8080", ConnectionType.FRONTEND));
        registry.register(createConnectionInfo(2L, "192.168.1.1:8080", ConnectionType.FRONTEND));
        registry.register(createConnectionInfo(3L, "192.168.1.2:8080", ConnectionType.FRONTEND));
        
        List<ConnectionInfo> fromAddr1 = registry.lookupByRemoteAddress("192.168.1.1:8080");
        List<ConnectionInfo> fromAddr2 = registry.lookupByRemoteAddress("192.168.1.2:8080");
        List<ConnectionInfo> fromAddr3 = registry.lookupByRemoteAddress("192.168.1.3:8080");
        
        assertEquals(2, fromAddr1.size());
        assertEquals(1, fromAddr2.size());
        assertTrue(fromAddr3.isEmpty());
    }
    
    @Test
    public void testLookupByStatus() {
        ConnectionInfo active = createConnectionInfo(1L, "192.168.1.1:8080", ConnectionType.FRONTEND);
        active.setStatus(ConnectionStatus.ACTIVE);
        
        ConnectionInfo idle = createConnectionInfo(2L, "192.168.1.2:8080", ConnectionType.FRONTEND);
        idle.setStatus(ConnectionStatus.IDLE);
        
        registry.register(active);
        registry.register(idle);
        
        List<ConnectionInfo> activeList = registry.lookupByStatus(ConnectionStatus.ACTIVE);
        List<ConnectionInfo> idleList = registry.lookupByStatus(ConnectionStatus.IDLE);
        
        assertEquals(1, activeList.size());
        assertEquals(1, idleList.size());
    }
    
    @Test
    public void testGetConnectionCountByType() {
        registry.register(createConnectionInfo(1L, "192.168.1.1:8080", ConnectionType.FRONTEND));
        registry.register(createConnectionInfo(2L, "192.168.1.2:8080", ConnectionType.BACKEND));
        registry.register(createConnectionInfo(3L, "192.168.1.3:8080", ConnectionType.FRONTEND));
        
        assertEquals(2, registry.getConnectionCount(ConnectionType.FRONTEND));
        assertEquals(1, registry.getConnectionCount(ConnectionType.BACKEND));
    }
    
    @Test
    public void testClear() {
        registry.register(createConnectionInfo(1L, "192.168.1.1:8080", ConnectionType.FRONTEND));
        registry.register(createConnectionInfo(2L, "192.168.1.2:8080", ConnectionType.FRONTEND));
        
        registry.clear();
        
        assertEquals(0, registry.getConnectionCount());
    }
    
    @Test
    public void testAddressIndexCleanup() {
        registry.register(createConnectionInfo(1L, "192.168.1.1:8080", ConnectionType.FRONTEND));
        registry.register(createConnectionInfo(2L, "192.168.1.1:8080", ConnectionType.FRONTEND));
        
        registry.unregister(1L);
        List<ConnectionInfo> remaining = registry.lookupByRemoteAddress("192.168.1.1:8080");
        assertEquals(1, remaining.size());
        
        registry.unregister(2L);
        List<ConnectionInfo> empty = registry.lookupByRemoteAddress("192.168.1.1:8080");
        assertTrue(empty.isEmpty());
    }
    
    private ConnectionInfo createConnectionInfo(long id, String remoteAddress, ConnectionType type) {
        return new ConnectionInfo.Builder()
            .connectionId(id)
            .remoteAddress(remoteAddress)
            .connectionType(type)
            .status(ConnectionStatus.ACTIVE)
            .build();
    }
}