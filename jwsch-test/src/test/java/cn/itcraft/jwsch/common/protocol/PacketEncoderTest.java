package cn.itcraft.jwsch.common.protocol;

import cn.itcraft.jwsch.common.exception.ErrorCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class PacketEncoderTest {
    
    private PacketEncoder encoder;
    
    @Before
    public void setUp() {
        encoder = new PacketEncoder();
    }
    
    @After
    public void tearDown() {
        encoder = null;
    }
    
    @Test
    public void testEncode_normalPacket() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.REQUEST)
            .errorCode(ErrorCode.SUCCESS)
            .sourceId(12345L)
            .targetId(67890L)
            .bodyLength(9)
            .build();
        
        byte[] body = "test body".getBytes(StandardCharsets.UTF_8);
        ByteBuf bodyBuf = Unpooled.wrappedBuffer(body);
        Packet packet = new Packet(header, bodyBuf);
        
        ByteBuf out = Unpooled.buffer();
        encoder.encode(null, packet, out);
        
        assertEquals(27 + 9, out.readableBytes());
        
        assertEquals((byte) 0xe7, out.readByte());
        assertEquals((byte) 0x34, out.readByte());
        assertEquals(27, out.readShort());
        assertEquals(9, out.readInt());
        assertEquals(Command.REQUEST, out.readByte());
        assertEquals(ErrorCode.SUCCESS.getCode(), out.readShort());
        assertEquals(12345L, out.readLong());
        assertEquals(67890L, out.readLong());
        
        out.release();
        bodyBuf.release();
    }
    
    @Test
    public void testEncode_packetWithTopic() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.REQUEST)
            .errorCode(ErrorCode.SUCCESS)
            .sourceId(12345L)
            .targetId(67890L)
            .topic("/api/test")
            .bodyLength(0)
            .build();
        
        Packet packet = new Packet(header, null);
        
        ByteBuf out = Unpooled.buffer();
        encoder.encode(null, packet, out);
        
        assertEquals(27 + 9, out.readableBytes());
        
        out.skipBytes(2);
        assertEquals(36, out.readShort());
        
        out.release();
    }
    
    @Test
    public void testEncode_packetWithNullBody() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.RESPONSE)
            .errorCode(ErrorCode.SUCCESS)
            .sourceId(12345L)
            .targetId(67890L)
            .bodyLength(0)
            .build();
        
        Packet packet = new Packet(header, null);
        
        ByteBuf out = Unpooled.buffer();
        encoder.encode(null, packet, out);
        
        assertEquals(27, out.readableBytes());
        
        out.release();
    }
}