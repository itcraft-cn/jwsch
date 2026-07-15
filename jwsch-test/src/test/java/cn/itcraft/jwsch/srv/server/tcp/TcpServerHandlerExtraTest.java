package cn.itcraft.jwsch.srv.server.tcp;

import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketHeader;
import cn.itcraft.jwsch.srv.router.PacketRouter;
import cn.itcraft.jwsch.srv.router.TopicSubscription;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;

import static org.mockito.Mockito.*;

public class TcpServerHandlerExtraTest {
    
    private PacketRouter packetRouter;
    private TopicSubscription topicSubscription;
    private TcpServerHandler handler;
    private ChannelHandlerContext ctx;
    private Channel channel;
    
    @Before
    public void setUp() {
        packetRouter = mock(PacketRouter.class);
        topicSubscription = mock(TopicSubscription.class);
        when(packetRouter.getTopicSubscription()).thenReturn(topicSubscription);
        when(packetRouter.getFrontendConnectionCount()).thenReturn(10);
        
        handler = new TcpServerHandler(packetRouter);
        ctx = mock(ChannelHandlerContext.class);
        channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("localhost", 12345));
    }
    
    @Test
    public void testChannelActive() {
        handler.channelActive(ctx);
        
        verify(ctx).channel();
    }
    
    @Test
    public void testChannelInactive() {
        handler.channelInactive(ctx);
        
        verify(ctx).channel();
    }
    
    @Test
    public void testChannelReadWithNonPacket() {
        Object msg = new Object();
        
        handler.channelRead(ctx, msg);
        
        verify(packetRouter, never()).broadcast(any());
    }
    
    @Test
    public void testHandlePush() {
        when(topicSubscription.hasTopic("test-topic")).thenReturn(true);
        when(topicSubscription.getSubscriberCount("test-topic")).thenReturn(5);
        
        ByteBuf body = Unpooled.buffer(4);
        body.writeInt(1234);
        
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.PUSH)
            .topic("test-topic")
            .bodyLength(4)
            .build();
        
        Packet packet = new Packet(header, body);
        
        handler.channelRead(ctx, packet);
        
        verify(packetRouter).broadcastToTopic(eq("test-topic"), any(Packet.class));
    }
    
    @Test
    public void testHandlePushNoTopic() {
        when(topicSubscription.hasTopic("nonexistent")).thenReturn(false);
        
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.PUSH)
            .topic("nonexistent")
            .bodyLength(0)
            .build();
        
        Packet packet = new Packet(header, null);
        
        handler.channelRead(ctx, packet);
        
        verify(packetRouter, never()).broadcastToTopic(anyString(), any(Packet.class));
    }
    
    @Test
    public void testHandleBroadcast() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.BROADCAST)
            .bodyLength(0)
            .build();
        
        Packet packet = new Packet(header, null);
        
        handler.channelRead(ctx, packet);
        
        verify(packetRouter).broadcast(packet);
    }
    
    @Test
    public void testHandleRequestWithTargetId() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.REQUEST)
            .targetId(12345L)
            .bodyLength(0)
            .build();
        
        Packet packet = new Packet(header, null);
        
        handler.channelRead(ctx, packet);
        
        verify(packetRouter).routeToFrontend(packet);
    }
    
    @Test
    public void testHandleRequestWithoutTargetId() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.REQUEST)
            .targetId(0L)
            .bodyLength(0)
            .build();
        
        Packet packet = new Packet(header, null);
        
        handler.channelRead(ctx, packet);
        
        verify(packetRouter, never()).routeToFrontend(any());
    }
    
    @Test
    public void testHandleHeartbeat() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.HEARTBEAT)
            .bodyLength(0)
            .build();
        
        Packet packet = new Packet(header, null);
        
        handler.channelRead(ctx, packet);
        
        verify(ctx).writeAndFlush(packet);
    }
    
    @Test
    public void testHandleUnhandledCommand() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.RESPONSE)
            .bodyLength(0)
            .build();
        
        Packet packet = new Packet(header, null);
        
        handler.channelRead(ctx, packet);
        
        verify(packetRouter, never()).broadcast(any());
    }
    
    @Test
    public void testUserEventTriggeredReaderIdle() {
        IdleStateEvent event = mock(IdleStateEvent.class);
        when(event.state()).thenReturn(IdleState.READER_IDLE);
        
        handler.userEventTriggered(ctx, event);
        
        verify(ctx).close();
    }
    
    @Test
    public void testExceptionCaught() {
        Throwable cause = new RuntimeException("test exception");
        
        handler.exceptionCaught(ctx, cause);
        
        verify(ctx).close();
    }
}