package cn.itcraft.jwsch.srv.stats;

import cn.itcraft.jwsch.common.protocol.Command;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TopicMessageTrackerTest {
    
    private TopicMessageTracker tracker;
    
    @Before
    public void setUp() {
        tracker = new TopicMessageTracker();
    }
    
    @Test
    public void testRecord_request() {
        tracker.record("/api/user", Command.REQUEST);
        
        TopicMessageStats stats = tracker.getStats("/api/user");
        assertNotNull(stats);
        assertEquals(1, stats.getMessageCount());
        assertEquals(1, stats.getRequestCount());
        assertEquals(0, stats.getResponseCount());
    }
    
    @Test
    public void testRecord_response() {
        tracker.record("/api/user", Command.RESPONSE);
        
        TopicMessageStats stats = tracker.getStats("/api/user");
        assertNotNull(stats);
        assertEquals(1, stats.getMessageCount());
        assertEquals(1, stats.getResponseCount());
    }
    
    @Test
    public void testRecord_push() {
        tracker.record("/topic/news", Command.PUSH);
        
        TopicMessageStats stats = tracker.getStats("/topic/news");
        assertNotNull(stats);
        assertEquals(1, stats.getMessageCount());
        assertEquals(1, stats.getPushCount());
    }
    
    @Test
    public void testRecord_broadcast() {
        tracker.record("/topic/alerts", Command.BROADCAST);
        
        TopicMessageStats stats = tracker.getStats("/topic/alerts");
        assertNotNull(stats);
        assertEquals(1, stats.getMessageCount());
        assertEquals(1, stats.getBroadcastCount());
    }
    
    @Test
    public void testRecord_multipleMessages() {
        tracker.record("/api/user", Command.REQUEST);
        tracker.record("/api/user", Command.REQUEST);
        tracker.record("/api/user", Command.RESPONSE);
        
        TopicMessageStats stats = tracker.getStats("/api/user");
        assertNotNull(stats);
        assertEquals(3, stats.getMessageCount());
        assertEquals(2, stats.getRequestCount());
        assertEquals(1, stats.getResponseCount());
    }
    
    @Test
    public void testGetTop10_correctOrder() {
        for (int i = 0; i < 5; i++) {
            tracker.record("/api/a", Command.REQUEST);
        }
        for (int i = 0; i < 3; i++) {
            tracker.record("/api/b", Command.REQUEST);
        }
        tracker.record("/api/c", Command.REQUEST);
        
        List<TopicMessageStats> top10 = tracker.getTop10();
        
        assertEquals(3, top10.size());
        assertEquals("/api/a", top10.get(0).getTopic());
        assertEquals(5, top10.get(0).getMessageCount());
    }
    
    @Test
    public void testGetStats_nonExistent() {
        TopicMessageStats stats = tracker.getStats("/nonexistent");
        assertNull(stats);
    }
    
    @Test
    public void testClear() {
        tracker.record("/api/user", Command.REQUEST);
        
        tracker.clear();
        
        assertEquals(0, tracker.getTotalTopics());
    }
}