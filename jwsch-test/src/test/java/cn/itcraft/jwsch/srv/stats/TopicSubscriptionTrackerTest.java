package cn.itcraft.jwsch.srv.stats;

import cn.itcraft.jwsch.common.protocol.Command;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TopicSubscriptionTrackerTest {
    
    private TopicSubscriptionTracker tracker;
    
    @Before
    public void setUp() {
        tracker = new TopicSubscriptionTracker();
    }
    
    @Test
    public void testSubscribe_incrementsCount() {
        tracker.subscribe("/topic/news");
        
        assertEquals(1, tracker.getSubscriptionCount("/topic/news"));
    }
    
    @Test
    public void testSubscribe_sameTopicMultipleTimes() {
        tracker.subscribe("/topic/news");
        tracker.subscribe("/topic/news");
        tracker.subscribe("/topic/news");
        
        assertEquals(3, tracker.getSubscriptionCount("/topic/news"));
    }
    
    @Test
    public void testUnsubscribe_decrementsCount() {
        tracker.subscribe("/topic/news");
        tracker.subscribe("/topic/news");
        tracker.unsubscribe("/topic/news");
        
        assertEquals(1, tracker.getSubscriptionCount("/topic/news"));
    }
    
    @Test
    public void testUnsubscribe_toZero_removesTopic() {
        tracker.subscribe("/topic/news");
        tracker.unsubscribe("/topic/news");
        
        assertEquals(0, tracker.getSubscriptionCount("/topic/news"));
        assertEquals(0, tracker.getTotalTopics());
    }
    
    @Test
    public void testUnsubscribe_nonExistent_noError() {
        tracker.unsubscribe("/topic/nonexistent");
        
        assertEquals(0, tracker.getSubscriptionCount("/topic/nonexistent"));
    }
    
    @Test
    public void testGetTop10_returnsCorrectOrder() {
        tracker.subscribe("/topic/a");
        tracker.subscribe("/topic/a");
        tracker.subscribe("/topic/a");
        tracker.subscribe("/topic/b");
        tracker.subscribe("/topic/b");
        tracker.subscribe("/topic/c");
        
        List<TopicSubscriptionStats> top10 = tracker.getTop10();
        
        assertEquals(3, top10.size());
        assertEquals("/topic/a", top10.get(0).getTopic());
        assertEquals(3, top10.get(0).getSubscriptionCount());
        assertEquals("/topic/b", top10.get(1).getTopic());
        assertEquals(2, top10.get(1).getSubscriptionCount());
        assertEquals("/topic/c", top10.get(2).getTopic());
        assertEquals(1, top10.get(2).getSubscriptionCount());
    }
    
    @Test
    public void testGetTop10_moreThan10() {
        for (int i = 0; i < 15; i++) {
            tracker.subscribe("/topic/" + i);
            for (int j = 0; j < i; j++) {
                tracker.subscribe("/topic/" + i);
            }
        }
        
        List<TopicSubscriptionStats> top10 = tracker.getTop10();
        
        assertEquals(10, top10.size());
    }
    
    @Test
    public void testClear() {
        tracker.subscribe("/topic/news");
        tracker.subscribe("/topic/alerts");
        
        tracker.clear();
        
        assertEquals(0, tracker.getTotalTopics());
    }
    
    @Test
    public void testGetTotalTopics() {
        tracker.subscribe("/topic/a");
        tracker.subscribe("/topic/b");
        tracker.subscribe("/topic/c");
        
        assertEquals(3, tracker.getTotalTopics());
    }
}