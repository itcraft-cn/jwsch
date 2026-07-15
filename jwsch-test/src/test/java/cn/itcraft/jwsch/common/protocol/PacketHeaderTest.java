package cn.itcraft.jwsch.common.protocol;

import cn.itcraft.jwsch.common.exception.ErrorCode;
import org.junit.Test;

import static org.junit.Assert.*;

public class PacketHeaderTest {
    
    @Test
    public void testBuilder_normalCase() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.REQUEST)
            .errorCode(ErrorCode.SUCCESS)
            .sourceId(12345L)
            .targetId(67890L)
            .topic("/api/test")
            .bodyLength(100)
            .build();
        
        assertEquals(27 + 9, header.getHeaderLength());
        assertEquals(100, header.getBodyLength());
        assertEquals(Command.REQUEST, header.getCommand());
        assertEquals(ErrorCode.SUCCESS.getCode(), header.getErrorCode());
        assertEquals(12345L, header.getSourceId());
        assertEquals(67890L, header.getTargetId());
        assertEquals("/api/test", header.getTopic());
    }
    
    @Test
    public void testBuilder_withoutTopic() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.RESPONSE)
            .errorCode(ErrorCode.SUCCESS)
            .sourceId(12345L)
            .targetId(67890L)
            .bodyLength(0)
            .build();
        
        assertEquals(27, header.getHeaderLength());
        assertNull(header.getTopic());
    }
    
    @Test
    public void testBuilder_errorCode() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.RESPONSE)
            .errorCode(ErrorCode.SERVICE_NOT_FOUND)
            .sourceId(12345L)
            .targetId(67890L)
            .bodyLength(0)
            .build();
        
        assertEquals(ErrorCode.SERVICE_NOT_FOUND.getCode(), header.getErrorCode());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_invalidCommand() {
        new PacketHeader.Builder()
            .command((byte) 0x00)
            .errorCode(ErrorCode.SUCCESS)
            .sourceId(12345L)
            .targetId(67890L)
            .build();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_topicTooLong() {
        StringBuilder longTopic = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            longTopic.append("a");
        }
        
        new PacketHeader.Builder()
            .command(Command.REQUEST)
            .errorCode(ErrorCode.SUCCESS)
            .sourceId(12345L)
            .targetId(67890L)
            .topic(longTopic.toString())
            .build();
    }
    
    @Test
    public void testIsSuccess_success() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.RESPONSE)
            .errorCode(ErrorCode.SUCCESS)
            .sourceId(12345L)
            .targetId(67890L)
            .build();
        
        assertTrue(header.isSuccess());
    }
    
    @Test
    public void testIsSuccess_failure() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.RESPONSE)
            .errorCode(ErrorCode.SERVICE_NOT_FOUND)
            .sourceId(12345L)
            .targetId(67890L)
            .build();
        
        assertFalse(header.isSuccess());
    }
}