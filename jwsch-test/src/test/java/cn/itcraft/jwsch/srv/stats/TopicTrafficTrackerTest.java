package cn.itcraft.jwsch.srv.stats;

import cn.itcraft.jwsch.common.protocol.Command;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TopicTrafficTrackerTest {
    
    private TopicTrafficTracker tracker;
    
    @Before
    public void setUp() {
        tracker = new TopicTrafficTracker();
    }
    
    @Test
    public void testRecord_request() {
        tracker.record("/api/user", Command.REQUEST, 100);
        
        TopicTrafficStats stats = tracker.getStats("/api/user");
        assertNotNull(stats);
        assertEquals(100, stats.getTotalBytes());
        assertEquals(100, stats.getRequestBytes());
        assertEquals(0, stats.getResponseBytes());
    }
    
    @Test
    public void testRecord_response() {
        tracker.record("/api/user", Command.RESPONSE, 500);
        
        TopicTrafficStats stats = tracker.getStats("/api/user");
        assertNotNull(stats);
        assertEquals(500, stats.getTotalBytes());
        assertEquals(500, stats.getResponseBytes());
    }
    
    @Test
    public void testRecord_multipleRecords() {
        tracker.record("/api/user", Command.REQUEST, 100);
        tracker.record("/api/user", Command.RESPONSE, 500);
        tracker.record("/api/user", Command.PUSH, 200);
        
        TopicTrafficStats stats = tracker.getStats("/api/user");
        assertNotNull(stats);
        assertEquals(800, stats.getTotalBytes());
        assertEquals(100, stats.getRequestBytes());
        assertEquals(500, stats.getResponseBytes());
        assertEquals(200, stats.getPushBytes());
    }
    
    @Test
    public void testGetTop10_correctOrder() {
        tracker.record("/api/a", Command.RESPONSE, 1000);
        tracker.record("/api/b", Command.RESPONSE, 500);
        tracker.record("/api/c", Command.RESPONSE, 100);
        
        List<TopicTrafficStats> top10 = tracker.getTop10();
        
        assertEquals(3, top10.size());
        assertEquals("/api/a", top10.get(0).getTopic());
        assertEquals(1000, top10.get(0).getTotalBytes());
    }
    
    @Test
    public void testGetStats_nonExistent() {
        TopicTrafficStats stats = tracker.getStats("/nonexistent");
        assertNull(stats);
    }
    
    @Test
    public void testClear() {
        tracker.record("/api/user", Command.REQUEST, 100);
        
        tracker.clear();
        
        assertEquals(0, tracker.getTotalTopics());
    }
}