package cn.itcraft.jwsch.srv.stats;

import cn.itcraft.jwsch.common.protocol.Command;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TopicStatsManagerTest {
    
    private DefaultTopicStatsManager manager;
    
    @Before
    public void setUp() {
        manager = new DefaultTopicStatsManager(1000, 3600);
    }
    
    @After
    public void tearDown() {
        if (manager != null) {
            manager.stop();
        }
    }
    
    @Test
    public void testRecordSubscribe() {
        manager.recordSubscribe("/topic/news");
        
        List<TopicSubscriptionStats> top10 = manager.getTop10Subscriptions();
        assertEquals(1, top10.size());
        assertEquals("/topic/news", top10.get(0).getTopic());
    }
    
    @Test
    public void testRecordUnsubscribe() {
        manager.recordSubscribe("/topic/news");
        manager.recordSubscribe("/topic/news");
        manager.recordUnsubscribe("/topic/news");
        
        List<TopicSubscriptionStats> top10 = manager.getTop10Subscriptions();
        assertEquals(1, top10.size());
        assertEquals(1, top10.get(0).getSubscriptionCount());
    }
    
    @Test
    public void testRecordMessage() {
        manager.recordMessage("/api/user", Command.REQUEST, 100);
        manager.recordMessage("/api/user", Command.RESPONSE, 500);
        
        List<TopicMessageStats> messageStats = manager.getTop10MessageCount();
        assertEquals(1, messageStats.size());
        assertEquals(2, messageStats.get(0).getMessageCount());
        
        List<TopicTrafficStats> trafficStats = manager.getTop10Traffic();
        assertEquals(1, trafficStats.size());
        assertEquals(600, trafficStats.get(0).getTotalBytes());
    }
    
    @Test
    public void testGetTotalTopics() {
        manager.recordSubscribe("/topic/a");
        manager.recordMessage("/api/b", Command.REQUEST, 100);
        manager.recordMessage("/api/c", Command.REQUEST, 100);
        
        assertTrue(manager.getTotalTopics() >= 2);
    }
    
    @Test
    public void testResetStats() {
        manager.recordSubscribe("/topic/news");
        manager.recordMessage("/api/user", Command.REQUEST, 100);
        
        manager.resetStats();
        
        assertEquals(0, manager.getTop10Subscriptions().size());
        assertEquals(0, manager.getTop10MessageCount().size());
    }
    
    @Test
    public void testStartAndStop() {
        manager.start();
        manager.stop();
    }
}