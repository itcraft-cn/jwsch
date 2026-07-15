package cn.itcraft.jwsch.common.protocol;

import org.junit.Test;

import static org.junit.Assert.*;

public class CommandTest {
    
    @Test
    public void testRequest() {
        assertEquals(0x01, Command.REQUEST);
    }
    
    @Test
    public void testResponse() {
        assertEquals(0x02, Command.RESPONSE);
    }
    
    @Test
    public void testPush() {
        assertEquals(0x03, Command.PUSH);
    }
    
    @Test
    public void testBroadcast() {
        assertEquals(0x04, Command.BROADCAST);
    }
    
    @Test
    public void testSubscribe() {
        assertEquals(0x05, Command.SUBSCRIBE);
    }
    
    @Test
    public void testHeartbeat() {
        assertEquals(0x06, Command.HEARTBEAT);
    }
    
    @Test
    public void testAck() {
        assertEquals(0x07, Command.ACK);
    }
    
    @Test
    public void testClusterSync() {
        assertEquals(0x10, Command.CLUSTER_SYNC);
    }
    
    @Test
    public void testClusterForward() {
        assertEquals(0x11, Command.CLUSTER_FORWARD);
    }
    
    @Test
    public void testClusterBroadcast() {
        assertEquals(0x12, Command.CLUSTER_BROADCAST);
    }
    
    @Test
    public void testIsValid_validCommands() {
        assertTrue(Command.isValid(Command.REQUEST));
        assertTrue(Command.isValid(Command.RESPONSE));
        assertTrue(Command.isValid(Command.PUSH));
        assertTrue(Command.isValid(Command.BROADCAST));
        assertTrue(Command.isValid(Command.SUBSCRIBE));
        assertTrue(Command.isValid(Command.HEARTBEAT));
        assertTrue(Command.isValid(Command.ACK));
        assertTrue(Command.isValid(Command.CLUSTER_SYNC));
        assertTrue(Command.isValid(Command.CLUSTER_FORWARD));
        assertTrue(Command.isValid(Command.CLUSTER_BROADCAST));
    }
    
    @Test
    public void testIsValid_invalidCommands() {
        assertFalse(Command.isValid((byte) 0x00));
        assertFalse(Command.isValid((byte) 0x08));
        assertFalse(Command.isValid((byte) 0x09));
        assertFalse(Command.isValid((byte) 0x0F));
        assertFalse(Command.isValid((byte) 0x13));
    }
}