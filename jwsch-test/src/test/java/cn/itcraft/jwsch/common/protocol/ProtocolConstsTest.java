package cn.itcraft.jwsch.common.protocol;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProtocolConstsTest {
    
    @Test
    public void testMagic() {
        byte[] magic = ProtocolConsts.MAGIC;
        assertNotNull(magic);
        assertEquals(2, magic.length);
        assertEquals((byte) 0xe7, magic[0]);
        assertEquals((byte) 0x34, magic[1]);
    }
    
    @Test
    public void testFixedHeaderLength() {
        assertEquals(27, ProtocolConsts.FIXED_HEADER_LENGTH);
    }
    
    @Test
    public void testMaxTopicLength() {
        assertEquals(256, ProtocolConsts.MAX_TOPIC_LENGTH);
    }
    
    @Test
    public void testMaxBodyLength() {
        assertEquals(10 * 1024 * 1024, ProtocolConsts.MAX_BODY_LENGTH);
    }
    
    @Test
    public void testDefaultMaxBodyLength() {
        assertEquals(99999, ProtocolConsts.DEFAULT_MAX_BODY_LENGTH);
    }
}