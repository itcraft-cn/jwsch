package cn.itcraft.jwsch.srv.server.websocket;

import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketHeader;
import cn.itcraft.jwsch.srv.router.PacketRouter;
import cn.itcraft.jwsch.srv.router.TopicSubscription;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WebSocketHandlerExtraTest {
    
    private PacketRouter packetRouter;
    private TopicSubscription topicSubscription;
    private WebSocketHandler handler;
    private ChannelHandlerContext ctx;
    private Channel channel;
    
    @Before
    public void setUp() {
        packetRouter = mock(PacketRouter.class);
        topicSubscription = mock(TopicSubscription.class);
        when(packetRouter.getTopicSubscription()).thenReturn(topicSubscription);
        
        handler = new WebSocketHandler(packetRouter);
        ctx = mock(ChannelHandlerContext.class);
        channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("localhost", 12345));
    }
    
    @Test
    public void testChannelActive() {
        handler.channelActive(ctx);
        
        verify(packetRouter).addFrontendConnection(anyLong(), eq(channel));
    }
    
    @Test
    public void testChannelInactive() {
        handler.channelActive(ctx);
        handler.channelInactive(ctx);
        
        verify(packetRouter).removeFrontendConnection(anyLong());
    }
    
    @Test
    public void testChannelInactiveWithoutActive() {
        handler.channelInactive(ctx);
        
        verify(packetRouter, never()).removeFrontendConnection(anyLong());
    }
    
    @Test
    public void testHandlePingFrame() throws Exception {
        ByteBuf content = Unpooled.copiedBuffer("ping", StandardCharsets.UTF_8);
        PingWebSocketFrame frame = new PingWebSocketFrame(content);
        
        handler.channelRead(ctx, frame);
        
        verify(ctx).writeAndFlush(any(PongWebSocketFrame.class));
    }
    
    @Test
    public void testHandlePongFrame() throws Exception {
        PongWebSocketFrame frame = new PongWebSocketFrame();
        
        handler.channelRead(ctx, frame);
        
        verify(ctx, never()).writeAndFlush(any());
    }
    
    @Test
    public void testHandleCloseFrame() throws Exception {
        CloseWebSocketFrame frame = new CloseWebSocketFrame();
        
        handler.channelRead(ctx, frame);
        
        verify(ctx).close();
    }
    
    @Test
    public void testHandleTextFrameSubscribe() throws Exception {
        handler.channelActive(ctx);
        
        TextWebSocketFrame frame = new TextWebSocketFrame("subscribe:test-topic");
        
        handler.channelRead(ctx, frame);
        
        verify(packetRouter).handleSubscribe(eq("test-topic"), anyLong());
        verify(ctx).writeAndFlush(any(TextWebSocketFrame.class));
    }
    
    @Test
    public void testHandleTextFrameEcho() throws Exception {
        handler.channelActive(ctx);
        
        TextWebSocketFrame frame = new TextWebSocketFrame("hello world");
        
        handler.channelRead(ctx, frame);
        
        ArgumentCaptor<TextWebSocketFrame> captor = ArgumentCaptor.forClass(TextWebSocketFrame.class);
        verify(ctx).writeAndFlush(captor.capture());
        
        assertTrue(captor.getValue().text().startsWith("echo:"));
    }
    
    @Test
    public void testHandleBinaryFrameTooShort() throws Exception {
        ByteBuf content = Unpooled.buffer(10);
        
        BinaryWebSocketFrame frame = new BinaryWebSocketFrame(content);
        handler.channelRead(ctx, frame);
        
        verify(packetRouter, never()).handleSubscribe(anyString(), anyLong());
    }
    
    @Test
    public void testHandleBinaryFrameInvalidMagic() throws Exception {
        ByteBuf content = Unpooled.buffer(50);
        content.writeByte(0x00);
        content.writeByte(0x00);
        
        BinaryWebSocketFrame frame = new BinaryWebSocketFrame(content);
        handler.channelRead(ctx, frame);
        
        verify(packetRouter, never()).handleSubscribe(anyString(), anyLong());
    }
    
    @Test
    public void testHandleBinaryFrameWithSubscribeCommand() throws Exception {
        handler.channelActive(ctx);
        
        ByteBuf content = Unpooled.buffer(50);
        content.writeByte(0xe7);
        content.writeByte(0x34);
        content.writeShort(27);
        content.writeInt(0);
        content.writeByte(Command.SUBSCRIBE);
        content.writeShort(0);
        content.writeLong(12345L);
        content.writeLong(0L);
        
        BinaryWebSocketFrame frame = new BinaryWebSocketFrame(content);
        handler.channelRead(ctx, frame);
        
        verify(packetRouter, never()).handleSubscribe(anyString(), anyLong());
    }
    
    @Test
    public void testUserEventTriggeredIdleState() throws Exception {
        IdleStateEvent event = mock(IdleStateEvent.class);
        
        handler.userEventTriggered(ctx, event);
        
        verify(ctx).close();
    }
    
    @Test
    public void testExceptionCaught() throws Exception {
        Throwable cause = new RuntimeException("test exception");
        
        handler.exceptionCaught(ctx, cause);
        
        verify(ctx).close();
    }
    
    @Test
    public void testChannelReadWithNonWebSocketFrame() throws Exception {
        Object msg = new Object();
        
        handler.channelRead(ctx, msg);
        
        verify(ctx, never()).writeAndFlush(any());
    }
}