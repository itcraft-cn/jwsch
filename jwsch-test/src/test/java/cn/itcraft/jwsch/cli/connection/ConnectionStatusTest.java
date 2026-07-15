package cn.itcraft.jwsch.cli.connection;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConnectionStatusTest {
    
    @Test
    public void testValues() {
        ConnectionStatus[] statuses = ConnectionStatus.values();
        assertEquals(3, statuses.length);
        assertEquals(ConnectionStatus.ACTIVE, statuses[0]);
        assertEquals(ConnectionStatus.IDLE, statuses[1]);
        assertEquals(ConnectionStatus.CLOSED, statuses[2]);
    }
    
    @Test
    public void testValueOf() {
        assertEquals(ConnectionStatus.ACTIVE, ConnectionStatus.valueOf("ACTIVE"));
        assertEquals(ConnectionStatus.IDLE, ConnectionStatus.valueOf("IDLE"));
        assertEquals(ConnectionStatus.CLOSED, ConnectionStatus.valueOf("CLOSED"));
    }
}