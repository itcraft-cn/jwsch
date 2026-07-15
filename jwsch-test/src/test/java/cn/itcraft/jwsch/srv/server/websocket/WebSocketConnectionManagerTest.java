package cn.itcraft.jwsch.srv.server.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WebSocketConnectionManagerTest {
    
    private WebSocketConnectionManager manager;
    
    @Before
    public void setUp() {
        manager = WebSocketConnectionManager.getInstance();
        manager.clear();
    }
    
    @Test
    public void testGetInstance() {
        WebSocketConnectionManager instance1 = WebSocketConnectionManager.getInstance();
        WebSocketConnectionManager instance2 = WebSocketConnectionManager.getInstance();
        
        assertSame(instance1, instance2);
    }
    
    @Test
    public void testAddChannel() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        
        long connectionId = manager.addChannel(channel);
        
        assertTrue(connectionId > 0);
        assertEquals(1, manager.getConnectionCount());
    }
    
    @Test
    public void testAddChannelWithNull() {
        long connectionId = manager.addChannel(null);
        
        assertEquals(-1, connectionId);
        assertEquals(0, manager.getConnectionCount());
    }
    
    @Test
    public void testAddChannelWithConnectionId() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        
        manager.addChannel(100L, channel);
        
        assertEquals(1, manager.getConnectionCount());
    }
    
    @Test
    public void testAddChannelWithConnectionIdNull() {
        manager.addChannel(100L, null);
        
        assertEquals(0, manager.getConnectionCount());
    }
    
    @Test
    public void testRemoveChannel() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        
        long connectionId = manager.addChannel(channel);
        manager.removeChannel(connectionId);
        
        assertEquals(0, manager.getConnectionCount());
    }
    
    @Test
    public void testRemoveChannelNotFound() {
        manager.removeChannel(999L);
        
        assertEquals(0, manager.getConnectionCount());
    }
    
    @Test
    public void testSubscribeTopic() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        
        long connectionId = manager.addChannel(channel);
        manager.subscribeTopic(connectionId, "test-topic");
        
        manager.unsubscribeTopic(connectionId, "test-topic");
    }
    
    @Test
    public void testSubscribeTopicWithNullChannel() {
        manager.subscribeTopic(999L, "test-topic");
        
    }
    
    @Test
    public void testSubscribeTopicWithNullTopic() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        
        long connectionId = manager.addChannel(channel);
        manager.subscribeTopic(connectionId, null);
    }
    
    @Test
    public void testBroadcastString() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        
        manager.addChannel(channel);
        manager.broadcast("test message");
        
        verify(channel).writeAndFlush(any());
    }
    
    @Test
    public void testBroadcastBytes() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        when(channel.alloc()).thenReturn(io.netty.buffer.UnpooledByteBufAllocator.DEFAULT);
        
        manager.addChannel(channel);
        manager.broadcast(new byte[]{1, 2, 3});
        
        verify(channel).writeAndFlush(any());
    }
    
    @Test
    public void testBroadcastToTopic() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        
        long connectionId = manager.addChannel(channel);
        manager.subscribeTopic(connectionId, "test-topic");
        manager.broadcastToTopic("test-topic", "test message");
        
        verify(channel).writeAndFlush(any());
    }
    
    @Test
    public void testBroadcastToTopicNoSubscribers() {
        manager.broadcastToTopic("nonexistent-topic", "test message");
    }
    
    @Test
    public void testUpdateActiveTime() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        
        long connectionId = manager.addChannel(channel);
        manager.updateActiveTime(connectionId);
        
    }
    
    @Test
    public void testUpdateActiveTimeWithNull() {
        manager.updateActiveTime(null);
    }
    
    @Test
    public void testGetConnectionStats() {
        Channel activeChannel = mock(Channel.class);
        when(activeChannel.isActive()).thenReturn(true);
        
        Channel inactiveChannel = mock(Channel.class);
        when(inactiveChannel.isActive()).thenReturn(false);
        
        manager.addChannel(activeChannel);
        manager.addChannel(inactiveChannel);
        
        WebSocketConnectionManager.ConnectionStats stats = manager.getConnectionStats();
        
        assertEquals(2, stats.getTotal());
        assertEquals(1, stats.getActive());
        assertEquals(1, stats.getInactive());
    }
    
    @Test
    public void testGetAllChannels() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        
        manager.addChannel(channel);
        
        List<Channel> channels = manager.getAllChannels();
        
        assertEquals(1, channels.size());
    }
    
    @Test
    public void testSetInactiveCheckIntervalMs() {
        manager.setInactiveCheckIntervalMs(30000);
    }
    
    @Test
    public void testSetMaxInactiveTimeMs() {
        manager.setMaxInactiveTimeMs(60000);
    }
    
    @Test
    public void testStartAndStopInactiveCheck() {
        manager.startInactiveCheck();
        manager.stopInactiveCheck();
    }
    
    @Test
    public void testDoubleStartInactiveCheck() {
        manager.startInactiveCheck();
        manager.startInactiveCheck();
        manager.stopInactiveCheck();
    }
    
    @Test
    public void testClear() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        
        manager.addChannel(channel);
        manager.clear();
        
        assertEquals(0, manager.getConnectionCount());
    }
}