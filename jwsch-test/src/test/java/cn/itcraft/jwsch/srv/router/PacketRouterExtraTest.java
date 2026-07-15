package cn.itcraft.jwsch.srv.router;

import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketHeader;
import cn.itcraft.jwsch.srv.loadbalance.LoadBalance;
import cn.itcraft.jwsch.srv.registry.ServiceInstance;
import cn.itcraft.jwsch.srv.registry.ServiceRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PacketRouterExtraTest {
    
    private ServiceRegistry serviceRegistry;
    private LoadBalance loadBalance;
    private PacketRouter router;
    
    @Before
    public void setUp() {
        serviceRegistry = mock(ServiceRegistry.class);
        loadBalance = mock(LoadBalance.class);
        router = new PacketRouter(serviceRegistry, loadBalance);
    }
    
    @Test
    public void testRouteWithNullPacket() {
        ServiceInstance result = router.route(null);
        
        assertNull(result);
    }
    
    @Test
    public void testRouteWithEmptyTopic() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.REQUEST)
            .topic("")
            .build();
        Packet packet = new Packet(header, null);
        
        ServiceInstance result = router.route(packet);
        
        assertNull(result);
    }
    
    @Test
    public void testRouteWithNullTopic() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.REQUEST)
            .build();
        Packet packet = new Packet(header, null);
        
        ServiceInstance result = router.route(packet);
        
        assertNull(result);
    }
    
    @Test
    public void testRouteWithNoInstances() {
        when(serviceRegistry.getInstances("test-service")).thenReturn(Collections.emptyList());
        
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.REQUEST)
            .topic("test-service")
            .build();
        Packet packet = new Packet(header, null);
        
        ServiceInstance result = router.route(packet);
        
        assertNull(result);
    }
    
    @Test
    public void testRouteWithApiTopic() {
        ServiceInstance instance = new ServiceInstance("service1", "localhost", 8080);
        when(serviceRegistry.getInstances("users")).thenReturn(Arrays.asList(instance));
        when(loadBalance.select(any())).thenReturn(instance);
        
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.REQUEST)
            .topic("/api/users")
            .build();
        Packet packet = new Packet(header, null);
        
        ServiceInstance result = router.route(packet);
        
        assertNotNull(result);
        assertEquals("service1", result.getServiceName());
    }
    
    @Test
    public void testAddAndRemoveFrontendConnection() {
        Channel channel = mock(Channel.class);
        
        router.addFrontendConnection(1L, channel);
        assertEquals(1, router.getFrontendConnectionCount());
        assertEquals(channel, router.getFrontendConnection(1L));
        
        router.removeFrontendConnection(1L);
        assertEquals(0, router.getFrontendConnectionCount());
        assertNull(router.getFrontendConnection(1L));
    }
    
    @Test
    public void testAddAndRemoveBackendConnection() {
        Channel channel = mock(Channel.class);
        
        router.addBackendConnection("test-service", channel);
        assertEquals(channel, router.getBackendConnection("test-service"));
        
        router.removeBackendConnection("test-service");
        assertNull(router.getBackendConnection("test-service"));
    }
    
    @Test
    public void testRouteToFrontendWithActiveChannel() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        when(channel.alloc()).thenReturn(UnpooledByteBufAllocator.DEFAULT);
        
        router.addFrontendConnection(1L, channel);
        
        ByteBuf body = Unpooled.buffer(4);
        body.writeInt(1234);
        
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.REQUEST)
            .targetId(1L)
            .bodyLength(4)
            .build();
        Packet packet = new Packet(header, body);
        
        router.routeToFrontend(packet);
        
        verify(channel).writeAndFlush(any());
    }
    
    @Test
    public void testRouteToFrontendWithInactiveChannel() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(false);
        
        router.addFrontendConnection(1L, channel);
        
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.REQUEST)
            .targetId(1L)
            .bodyLength(0)
            .build();
        Packet packet = new Packet(header, null);
        
        router.routeToFrontend(packet);
        
        verify(channel, never()).writeAndFlush(any());
    }
    
    @Test
    public void testRouteToFrontendWithNonExistentChannel() {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.REQUEST)
            .targetId(999L)
            .bodyLength(0)
            .build();
        Packet packet = new Packet(header, null);
        
        router.routeToFrontend(packet);
    }
    
    @Test
    public void testBroadcast() {
        Channel channel1 = mock(Channel.class);
        when(channel1.isActive()).thenReturn(true);
        when(channel1.alloc()).thenReturn(UnpooledByteBufAllocator.DEFAULT);
        
        Channel channel2 = mock(Channel.class);
        when(channel2.isActive()).thenReturn(true);
        when(channel2.alloc()).thenReturn(UnpooledByteBufAllocator.DEFAULT);
        
        router.addFrontendConnection(1L, channel1);
        router.addFrontendConnection(2L, channel2);
        
        ByteBuf body = Unpooled.buffer(4);
        body.writeInt(1234);
        
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.BROADCAST)
            .bodyLength(4)
            .build();
        Packet packet = new Packet(header, body);
        
        router.broadcast(packet);
        
        verify(channel1).writeAndFlush(any());
        verify(channel2).writeAndFlush(any());
    }
    
    @Test
    public void testBroadcastWithInactiveChannel() {
        Channel activeChannel = mock(Channel.class);
        when(activeChannel.isActive()).thenReturn(true);
        when(activeChannel.alloc()).thenReturn(UnpooledByteBufAllocator.DEFAULT);
        
        Channel inactiveChannel = mock(Channel.class);
        when(inactiveChannel.isActive()).thenReturn(false);
        
        router.addFrontendConnection(1L, activeChannel);
        router.addFrontendConnection(2L, inactiveChannel);
        
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.BROADCAST)
            .bodyLength(0)
            .build();
        Packet packet = new Packet(header, null);
        
        router.broadcast(packet);
        
        verify(activeChannel).writeAndFlush(any());
        verify(inactiveChannel, never()).writeAndFlush(any());
    }
    
    @Test
    public void testBroadcastToTopicWithSubscribers() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        when(channel.alloc()).thenReturn(UnpooledByteBufAllocator.DEFAULT);
        
        router.addFrontendConnection(1L, channel);
        router.handleSubscribe("test-topic", 1L);
        
        ByteBuf body = Unpooled.buffer(4);
        body.writeInt(1234);
        
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.PUSH)
            .bodyLength(4)
            .build();
        Packet packet = new Packet(header, body);
        
        router.broadcastToTopic("test-topic", packet);
        
        verify(channel).writeAndFlush(any());
    }
    
    @Test
    public void testBroadcastToTopicNoSubscribers() {
        ByteBuf body = Unpooled.buffer(4);
        body.writeInt(1234);
        
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.PUSH)
            .bodyLength(4)
            .build();
        Packet packet = new Packet(header, body);
        
        router.broadcastToTopic("nonexistent-topic", packet);
    }
    
    @Test
    public void testHandleUnsubscribe() {
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        
        router.addFrontendConnection(1L, channel);
        router.handleSubscribe("test-topic", 1L);
        router.handleUnsubscribe("test-topic", 1L);
        
        assertEquals(0, router.getTopicSubscription().getSubscriberCount("test-topic"));
    }
    
    @Test
    public void testGetResponseMapping() {
        assertNotNull(router.getResponseMapping());
    }
    
    @Test
    public void testShutdown() {
        Channel channel = mock(Channel.class);
        router.addFrontendConnection(1L, channel);
        
        router.shutdown();
        
        assertEquals(0, router.getFrontendConnectionCount());
    }
}