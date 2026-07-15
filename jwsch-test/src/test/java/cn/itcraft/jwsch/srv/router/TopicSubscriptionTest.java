package cn.itcraft.jwsch.srv.router;

import cn.itcraft.jwsch.common.protocol.TopicHash;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for TopicSubscription.
 *
 * <p>TopicSubscription was refactored in Phase 2:
 * <ul>
 *   <li>Topics are stored as hashes (long) instead of Strings</li>
 *   <li>Use TopicHash.hash(topic) to compute hashes</li>
 *   <li>getTopicHashes() returns Set<Long> instead of Set<String></li>
 * </ul>
 */
public class TopicSubscriptionTest {

    private TopicSubscription subscription;

    @Before
    public void setUp() {
        subscription = new TopicSubscription();
    }

    @Test
    public void testSubscribe() {
        subscription.subscribe("/topic/news", 1L);
        subscription.subscribe("/topic/news", 2L);
        subscription.subscribe("/topic/alerts", 1L);

        assertEquals(2, subscription.getSubscriberCount("/topic/news"));
        assertEquals(1, subscription.getSubscriberCount("/topic/alerts"));
        assertEquals(2, subscription.getTopicCount());
        assertEquals(3, subscription.getTotalSubscriptions());
    }

    @Test
    public void testUnsubscribe() {
        subscription.subscribe("/topic/news", 1L);
        subscription.subscribe("/topic/news", 2L);

        subscription.unsubscribe("/topic/news", 1L);

        assertEquals(1, subscription.getSubscriberCount("/topic/news"));
    }

    @Test
    public void testUnsubscribeLastSubscriber_removesTopic() {
        subscription.subscribe("/topic/news", 1L);
        subscription.unsubscribe("/topic/news", 1L);

        assertEquals(0, subscription.getTopicCount());
        assertTrue(subscription.getSubscribers("/topic/news").isEmpty());
    }

    @Test
    public void testUnsubscribeAll() {
        subscription.subscribe("/topic/news", 1L);
        subscription.subscribe("/topic/alerts", 1L);
        subscription.subscribe("/topic/chat", 1L);
        subscription.subscribe("/topic/news", 2L);

        subscription.unsubscribeAll(1L);

        assertEquals(1, subscription.getSubscriberCount("/topic/news"));
        assertEquals(0, subscription.getSubscriberCount("/topic/alerts"));
        assertEquals(0, subscription.getSubscriberCount("/topic/chat"));
    }

    @Test
    public void testGetSubscribers() {
        subscription.subscribe("/topic/news", 1L);
        subscription.subscribe("/topic/news", 2L);

        assertEquals(2, subscription.getSubscribers("/topic/news").size());
        assertTrue(subscription.getSubscribers("/topic/news").contains(1L));
        assertTrue(subscription.getSubscribers("/topic/news").contains(2L));
    }

    @Test
    public void testGetSubscribers_unknownTopic() {
        assertTrue(subscription.getSubscribers("/unknown").isEmpty());
    }

    @Test
    public void testGetTopicHashes() {
        subscription.subscribe("/topic/news", 1L);
        subscription.subscribe("/topic/alerts", 1L);

        long newsHash = TopicHash.hash("/topic/news");
        long alertsHash = TopicHash.hash("/topic/alerts");

        assertEquals(2, subscription.getTopicHashes().size());
        assertTrue(subscription.getTopicHashes().contains(newsHash));
        assertTrue(subscription.getTopicHashes().contains(alertsHash));
    }

    @Test
    public void testGetTopicHashesForConnection() {
        subscription.subscribe("/topic/news", 1L);
        subscription.subscribe("/topic/alerts", 1L);
        subscription.subscribe("/topic/chat", 2L);

        long newsHash = TopicHash.hash("/topic/news");
        long alertsHash = TopicHash.hash("/topic/alerts");
        long chatHash = TopicHash.hash("/topic/chat");

        assertEquals(2, subscription.getTopicHashesForConnection(1L).size());
        assertTrue(subscription.getTopicHashesForConnection(1L).contains(newsHash));
        assertTrue(subscription.getTopicHashesForConnection(1L).contains(alertsHash));

        assertEquals(1, subscription.getTopicHashesForConnection(2L).size());
        assertTrue(subscription.getTopicHashesForConnection(2L).contains(chatHash));
    }

    @Test
    public void testHasTopicHash() {
        subscription.subscribe("/topic/news", 1L);
        long newsHash = TopicHash.hash("/topic/news");

        assertTrue(subscription.hasTopicHash(newsHash));
        assertFalse(subscription.hasTopicHash(99999L));
    }

    @Test
    public void testClear() {
        subscription.subscribe("/topic/news", 1L);
        subscription.subscribe("/topic/alerts", 1L);

        subscription.clear();

        assertEquals(0, subscription.getTopicCount());
        assertEquals(0, subscription.getTotalSubscriptions());
    }
}