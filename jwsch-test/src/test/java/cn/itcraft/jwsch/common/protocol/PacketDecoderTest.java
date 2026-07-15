package cn.itcraft.jwsch.common.protocol;

import cn.itcraft.jwsch.common.exception.ErrorCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;

public class PacketDecoderTest {
    
    private PacketDecoder decoder;
    private EmbeddedChannel channel;
    
    @Before
    public void setUp() {
        decoder = new PacketDecoder();
        channel = new EmbeddedChannel(decoder);
    }
    
    @After
    public void tearDown() {
        if (channel != null) {
            channel.close();
        }
        decoder = null;
    }
    
    @Test
    public void testDecode_normalPacket() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(ProtocolConsts.MAGIC);
        buf.writeShort(27);
        buf.writeInt(9);
        buf.writeByte(Command.REQUEST);
        buf.writeShort(ErrorCode.SUCCESS.getCode());
        buf.writeLong(12345L);
        buf.writeLong(67890L);
        buf.writeBytes("test body".getBytes(StandardCharsets.UTF_8));
        
        channel.writeInbound(buf);
        
        Packet packet = channel.readInbound();
        assertNotNull(packet);
        
        PacketHeader header = packet.getHeader();
        assertEquals(Command.REQUEST, header.getCommand());
        assertEquals(ErrorCode.SUCCESS.getCode(), header.getErrorCode());
        assertEquals(12345L, header.getSourceId());
        assertEquals(67890L, header.getTargetId());
        assertNull(header.getTopic());
        assertEquals(9, header.getBodyLength());
        
        packet.release();
    }
    
    @Test
    public void testDecode_packetWithTopic() {
        String topic = "/api/test";
        byte[] topicBytes = topic.getBytes(StandardCharsets.US_ASCII);
        
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(ProtocolConsts.MAGIC);
        buf.writeShort(27 + topicBytes.length);
        buf.writeInt(0);
        buf.writeByte(Command.REQUEST);
        buf.writeShort(ErrorCode.SUCCESS.getCode());
        buf.writeLong(12345L);
        buf.writeLong(67890L);
        buf.writeBytes(topicBytes);
        
        channel.writeInbound(buf);
        
        Packet packet = channel.readInbound();
        assertNotNull(packet);
        
        PacketHeader header = packet.getHeader();
        assertEquals(topic, header.getTopic());
        
        packet.release();
    }
    
    @Test
    public void testDecode_invalidMagic() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(new byte[]{0x00, 0x00});
        buf.writeShort(27);
        buf.writeInt(0);
        buf.writeByte(Command.REQUEST);
        buf.writeShort(ErrorCode.SUCCESS.getCode());
        buf.writeLong(12345L);
        buf.writeLong(67890L);
        
        channel.writeInbound(buf);
        
        Packet packet = channel.readInbound();
        assertNull(packet);
        assertFalse(channel.isActive());
    }
    
    @Test
    public void testDecode_incompletePacket() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(ProtocolConsts.MAGIC);
        buf.writeShort(27);
        buf.writeInt(9);
        buf.writeByte(Command.REQUEST);
        
        channel.writeInbound(buf);
        
        Packet packet = channel.readInbound();
        assertNull(packet);
        assertTrue(channel.isActive());
    }
    
    @Test
    public void testDecode_invalidHeaderLength() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(ProtocolConsts.MAGIC);
        buf.writeShort(10);
        buf.writeInt(0);
        buf.writeByte(Command.REQUEST);
        buf.writeShort(ErrorCode.SUCCESS.getCode());
        buf.writeLong(12345L);
        buf.writeLong(67890L);
        
        channel.writeInbound(buf);
        
        Packet packet = channel.readInbound();
        assertNull(packet);
        assertFalse(channel.isActive());
    }
}