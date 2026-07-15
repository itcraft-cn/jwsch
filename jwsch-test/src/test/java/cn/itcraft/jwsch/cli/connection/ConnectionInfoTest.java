package cn.itcraft.jwsch.cli.connection;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConnectionInfoTest {
    
    @Test
    public void testBuilder() {
        long now = System.currentTimeMillis();
        ConnectionInfo info = new ConnectionInfo.Builder()
            .connectionId(12345L)
            .remoteAddress("192.168.1.100:8080")
            .localAddress("192.168.1.1:12345")
            .connectionType(ConnectionType.BACKEND)
            .serviceName("test-service")
            .createTime(now)
            .lastActiveTime(now)
            .status(ConnectionStatus.ACTIVE)
            .build();
        
        assertEquals(12345L, info.getConnectionId());
        assertEquals("192.168.1.100:8080", info.getRemoteAddress());
        assertEquals("192.168.1.1:12345", info.getLocalAddress());
        assertEquals(ConnectionType.BACKEND, info.getConnectionType());
        assertEquals("test-service", info.getServiceName());
        assertEquals(now, info.getCreateTime());
        assertEquals(now, info.getLastActiveTime());
        assertEquals(ConnectionStatus.ACTIVE, info.getStatus());
    }
    
    @Test
    public void testBuilderDefaultTime() {
        ConnectionInfo info = new ConnectionInfo.Builder()
            .connectionId(1L)
            .connectionType(ConnectionType.FRONTEND)
            .status(ConnectionStatus.ACTIVE)
            .build();
        
        assertTrue(info.getCreateTime() > 0);
        assertTrue(info.getLastActiveTime() > 0);
    }
    
    @Test(expected = NullPointerException.class)
    public void testBuilderNullType() {
        new ConnectionInfo.Builder()
            .connectionId(1L)
            .status(ConnectionStatus.ACTIVE)
            .build();
    }
    
    @Test(expected = NullPointerException.class)
    public void testBuilderNullStatus() {
        new ConnectionInfo.Builder()
            .connectionId(1L)
            .connectionType(ConnectionType.FRONTEND)
            .build();
    }
    
    @Test
    public void testSetLastActiveTime() {
        ConnectionInfo info = new ConnectionInfo.Builder()
            .connectionId(1L)
            .connectionType(ConnectionType.FRONTEND)
            .status(ConnectionStatus.ACTIVE)
            .build();
        
        long newTime = System.currentTimeMillis() + 1000;
        info.setLastActiveTime(newTime);
        assertEquals(newTime, info.getLastActiveTime());
    }
    
    @Test
    public void testSetStatus() {
        ConnectionInfo info = new ConnectionInfo.Builder()
            .connectionId(1L)
            .connectionType(ConnectionType.FRONTEND)
            .status(ConnectionStatus.ACTIVE)
            .build();
        
        info.setStatus(ConnectionStatus.IDLE);
        assertEquals(ConnectionStatus.IDLE, info.getStatus());
    }
    
    @Test
    public void testUpdateActiveTime() throws InterruptedException {
        ConnectionInfo info = new ConnectionInfo.Builder()
            .connectionId(1L)
            .connectionType(ConnectionType.FRONTEND)
            .status(ConnectionStatus.ACTIVE)
            .build();
        
        long originalTime = info.getLastActiveTime();
        Thread.sleep(10);
        info.updateActiveTime();
        
        assertTrue(info.getLastActiveTime() >= originalTime);
    }
    
    @Test
    public void testEquals() {
        ConnectionInfo info1 = new ConnectionInfo.Builder()
            .connectionId(1L)
            .connectionType(ConnectionType.FRONTEND)
            .remoteAddress("addr1")
            .status(ConnectionStatus.ACTIVE)
            .build();
        
        ConnectionInfo info2 = new ConnectionInfo.Builder()
            .connectionId(1L)
            .connectionType(ConnectionType.BACKEND)
            .remoteAddress("addr2")
            .status(ConnectionStatus.IDLE)
            .build();
        
        ConnectionInfo info3 = new ConnectionInfo.Builder()
            .connectionId(2L)
            .connectionType(ConnectionType.FRONTEND)
            .status(ConnectionStatus.ACTIVE)
            .build();
        
        assertEquals(info1, info2);
        assertNotEquals(info1, info3);
    }
    
    @Test
    public void testHashCode() {
        ConnectionInfo info1 = new ConnectionInfo.Builder()
            .connectionId(1L)
            .connectionType(ConnectionType.FRONTEND)
            .status(ConnectionStatus.ACTIVE)
            .build();
        
        ConnectionInfo info2 = new ConnectionInfo.Builder()
            .connectionId(1L)
            .connectionType(ConnectionType.BACKEND)
            .status(ConnectionStatus.IDLE)
            .build();
        
        assertEquals(info1.hashCode(), info2.hashCode());
    }
    
    @Test
    public void testToString() {
        ConnectionInfo info = new ConnectionInfo.Builder()
            .connectionId(12345L)
            .connectionType(ConnectionType.BACKEND)
            .serviceName("test-service")
            .status(ConnectionStatus.ACTIVE)
            .build();
        
        String str = info.toString();
        assertTrue(str.contains("12345"));
        assertTrue(str.contains("BACKEND"));
        assertTrue(str.contains("test-service"));
        assertTrue(str.contains("ACTIVE"));
    }
}