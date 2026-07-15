package cn.itcraft.jwsch.cli.connection;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConnectionTypeTest {
    
    @Test
    public void testValues() {
        ConnectionType[] types = ConnectionType.values();
        assertEquals(2, types.length);
        assertEquals(ConnectionType.FRONTEND, types[0]);
        assertEquals(ConnectionType.BACKEND, types[1]);
    }
    
    @Test
    public void testValueOf() {
        assertEquals(ConnectionType.FRONTEND, ConnectionType.valueOf("FRONTEND"));
        assertEquals(ConnectionType.BACKEND, ConnectionType.valueOf("BACKEND"));
    }
}