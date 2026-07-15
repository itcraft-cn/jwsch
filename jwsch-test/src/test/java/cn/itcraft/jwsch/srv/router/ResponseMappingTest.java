package cn.itcraft.jwsch.srv.router;

import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketHeader;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

public class ResponseMappingTest {
    
    private ResponseMapping responseMapping;
    
    @Before
    public void setUp() {
        responseMapping = new ResponseMapping(1000);
    }
    
    @Test
    public void testGenerateRequestId() {
        int id1 = responseMapping.generateRequestId();
        int id2 = responseMapping.generateRequestId();
        
        assertNotEquals(id1, id2);
        assertTrue(id1 > 0);
    }
    
    @Test
    public void testCreateFuture() {
        int requestId = responseMapping.generateRequestId();
        CompletableFuture<Packet> future = responseMapping.createFuture(requestId);
        
        assertNotNull(future);
        assertFalse(future.isDone());
        assertEquals(1, responseMapping.getPendingCount());
    }
    
    @Test
    public void testCompleteResponse() {
        int requestId = responseMapping.generateRequestId();
        CompletableFuture<Packet> future = responseMapping.createFuture(requestId);
        
        Packet response = createTestPacket();
        boolean completed = responseMapping.completeResponse(requestId, response);
        
        assertTrue(completed);
        assertTrue(future.isDone());
        assertEquals(response, future.join());
    }
    
    @Test
    public void testCompleteResponse_unknownRequestId() {
        boolean completed = responseMapping.completeResponse(99999, createTestPacket());
        
        assertFalse(completed);
    }
    
    @Test
    public void testTimeout() throws Exception {
        int requestId = responseMapping.generateRequestId();
        CompletableFuture<Packet> future = responseMapping.createFuture(requestId);
        
        try {
            future.get(2, TimeUnit.SECONDS);
            fail("Should throw exception");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof java.util.concurrent.TimeoutException);
        }
        
        Thread.sleep(100);
        assertEquals(0, responseMapping.getPendingCount());
    }
    
    @Test
    public void testRemoveFuture() {
        int requestId = responseMapping.generateRequestId();
        responseMapping.createFuture(requestId);
        
        responseMapping.removeFuture(requestId);
        
        assertEquals(0, responseMapping.getPendingCount());
    }
    
    @Test
    public void testShutdown() {
        responseMapping.createFuture(responseMapping.generateRequestId());
        responseMapping.createFuture(responseMapping.generateRequestId());
        
        responseMapping.shutdown();
        
        assertEquals(0, responseMapping.getPendingCount());
    }
    
    private Packet createTestPacket() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.RESPONSE)
            .sourceId(1L)
            .targetId(2L)
            .build();
        return new Packet(header, null);
    }
}