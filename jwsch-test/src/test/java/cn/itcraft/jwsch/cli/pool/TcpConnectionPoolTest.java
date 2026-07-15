package cn.itcraft.jwsch.cli.pool;

import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TcpConnectionPoolTest {
    
    private TcpConnectionPool pool;
    
    @Before
    public void setUp() {
        pool = new TcpConnectionPool(5);
    }
    
    @Test
    public void testAddChannel() {
        EmbeddedChannel channel = new EmbeddedChannel();
        
        pool.addChannel("test-service", channel);
        
        assertEquals(1, pool.getConnectionCount("test-service"));
        channel.finish();
    }
    
    @Test(expected = NullPointerException.class)
    public void testAddChannelNullServiceName() {
        EmbeddedChannel channel = new EmbeddedChannel();
        pool.addChannel(null, channel);
    }
    
    @Test(expected = NullPointerException.class)
    public void testAddChannelNullChannel() {
        pool.addChannel("test-service", null);
    }
    
    @Test
    public void testGetChannel() {
        EmbeddedChannel channel = new EmbeddedChannel();
        pool.addChannel("test-service", channel);
        
        Channel retrieved = pool.getChannel("test-service");
        
        assertNotNull(retrieved);
        assertEquals(channel, retrieved);
        channel.finish();
    }
    
    @Test
    public void testGetChannelNonExistentService() {
        Channel channel = pool.getChannel("non-existent");
        assertNull(channel);
    }
    
    @Test
    public void testGetChannelRoundRobin() {
        EmbeddedChannel channel1 = new EmbeddedChannel();
        EmbeddedChannel channel2 = new EmbeddedChannel();
        EmbeddedChannel channel3 = new EmbeddedChannel();
        
        pool.addChannel("test-service", channel1);
        pool.addChannel("test-service", channel2);
        pool.addChannel("test-service", channel3);
        
        Channel c1 = pool.getChannel("test-service");
        Channel c2 = pool.getChannel("test-service");
        Channel c3 = pool.getChannel("test-service");
        
        assertNotSame(c1, c2);
        assertNotSame(c2, c3);
        
        channel1.finish();
        channel2.finish();
        channel3.finish();
    }
    
    @Test
    public void testRemoveChannel() {
        EmbeddedChannel channel = new EmbeddedChannel();
        pool.addChannel("test-service", channel);
        
        pool.removeChannel("test-service", channel);
        
        assertEquals(0, pool.getConnectionCount("test-service"));
        channel.finish();
    }
    
    @Test
    public void testGetActiveChannels() {
        EmbeddedChannel activeChannel = new EmbeddedChannel();
        EmbeddedChannel inactiveChannel = new EmbeddedChannel();
        inactiveChannel.close().awaitUninterruptibly();
        
        pool.addChannel("test-service", activeChannel);
        pool.addChannel("test-service", inactiveChannel);
        
        List<Channel> activeChannels = pool.getActiveChannels("test-service");
        
        assertEquals(1, activeChannels.size());
        assertTrue(activeChannels.contains(activeChannel));
        
        activeChannel.finish();
    }
    
    @Test
    public void testGetActiveConnectionCount() {
        EmbeddedChannel activeChannel = new EmbeddedChannel();
        EmbeddedChannel inactiveChannel = new EmbeddedChannel();
        inactiveChannel.close().awaitUninterruptibly();
        
        pool.addChannel("test-service", activeChannel);
        pool.addChannel("test-service", inactiveChannel);
        
        assertEquals(2, pool.getConnectionCount("test-service"));
        assertEquals(1, pool.getActiveConnectionCount("test-service"));
        
        activeChannel.finish();
    }
    
    @Test
    public void testClear() {
        EmbeddedChannel channel1 = new EmbeddedChannel();
        EmbeddedChannel channel2 = new EmbeddedChannel();
        
        pool.addChannel("test-service", channel1);
        pool.addChannel("test-service", channel2);
        
        pool.clear("test-service");
        
        assertEquals(0, pool.getConnectionCount("test-service"));
    }
    
    @Test
    public void testClearAll() {
        EmbeddedChannel channel1 = new EmbeddedChannel();
        EmbeddedChannel channel2 = new EmbeddedChannel();
        
        pool.addChannel("service1", channel1);
        pool.addChannel("service2", channel2);
        
        pool.clearAll();
        
        assertEquals(0, pool.getConnectionCount("service1"));
        assertEquals(0, pool.getConnectionCount("service2"));
    }
    
    @Test
    public void testMaxConnectionsPerService() {
        for (int i = 0; i < 10; i++) {
            EmbeddedChannel channel = new EmbeddedChannel();
            pool.addChannel("test-service", channel);
        }
        
        assertEquals(5, pool.getConnectionCount("test-service"));
    }
    
    @Test
    public void testGetChannelSkipsInactive() {
        EmbeddedChannel inactiveChannel = new EmbeddedChannel();
        inactiveChannel.close().awaitUninterruptibly();
        
        EmbeddedChannel activeChannel = new EmbeddedChannel();
        
        pool.addChannel("test-service", inactiveChannel);
        pool.addChannel("test-service", activeChannel);
        
        Channel retrieved = pool.getChannel("test-service");
        
        assertNull(retrieved);
        
        activeChannel.finish();
    }
}