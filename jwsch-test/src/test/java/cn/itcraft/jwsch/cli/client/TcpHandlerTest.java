package cn.itcraft.jwsch.cli.client;

import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketHeader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.timeout.IdleStateEvent;
import org.junit.Test;

import static org.junit.Assert.*;

public class TcpHandlerTest {
    
    @Test
    public void testHandlerReceivesPacket() {
        TcpHandler handler = new TcpHandler();
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.HEARTBEAT)
            .sourceId(1L)
            .targetId(2L)
            .build();
        
        ByteBuf body = Unpooled.EMPTY_BUFFER;
        Packet packet = new Packet(header, body);
        
        channel.writeInbound(packet);
        
        assertFalse(channel.finish());
    }
    
    @Test
    public void testUserEventTriggered_IdleStateEvent() {
        TcpHandler handler = new TcpHandler();
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        
        channel.pipeline().fireUserEventTriggered(IdleStateEvent.ALL_IDLE_STATE_EVENT);
        
        assertFalse(channel.isOpen());
        channel.finish();
    }
    
    @Test
    public void testExceptionCaught() {
        TcpHandler handler = new TcpHandler();
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        
        assertTrue(channel.isOpen());
        
        channel.pipeline().fireExceptionCaught(new RuntimeException("Test exception"));
        
        assertFalse(channel.isOpen());
        channel.finish();
    }
}