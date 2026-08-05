package cn.itcraft.jwsch.srv.cluster;

import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

/**
 * Node-level Bloom filter for fast topic subscription check.
 * 
 * <p>Used to quickly determine if this node has any subscribers for a topic,
 * avoiding unnecessary cluster forwarding when there are no local subscribers.
 * 
 * <p>Characteristics:
 * <ul>
 *   <li>False positive rate: 3%</li>
 *   <li>Memory efficient: ~91KB for 100K topics</li>
 *   <li>Thread-safe: ReadWriteLock protection</li>
 * </ul>
 * 
 * <p>Note: Bloom filter has false positives (may say "might have" when actually not),
 * but no false negatives (if says "definitely not", then definitely not).
 * For subscription matching, we use exact HashSet, not bloom filter.
 */
public class NodeBloomFilter {
    
    private static final double FALSE_POSITIVE_RATE = 0.03;
    
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private BloomFilter<Long> bloom;
    private int expectedTopics;
    
    /**
     * Creates a Bloom filter with expected number of topics.
     * 
     * @param expectedTopics expected number of unique topics for sizing the filter
     */
    public NodeBloomFilter(int expectedTopics) {
        this.expectedTopics = expectedTopics;
        this.bloom = createBloomFilter(expectedTopics);
    }
    
    private BloomFilter<Long> createBloomFilter(int expectedInsertions) {
        return BloomFilter.create(Funnels.longFunnel(), expectedInsertions, FALSE_POSITIVE_RATE);
    }
    
    /**
     * Adds a topic hash to the Bloom filter.
     * 
     * <p>Acquires write lock to ensure thread safety.
     * 
     * @param topicHash hash of the topic to add
     */
    public void addTopic(long topicHash) {
        lock.writeLock().lock();
        try {
            bloom.put(topicHash);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Checks if the Bloom filter might contain the given topic hash.
     * 
     * <p>Acquires read lock for thread safety.
     * <p>Note: Bloom filters have false positives but no false negatives.
     * A return value of {@code true} means the topic <i>might</i> be present;
     * {@code false} means the topic is <i>definitely not</i> present.
     * 
     * @param topicHash hash of the topic to check
     * @return {@code true} if the topic might be present, {@code false} if definitely not present
     */
    public boolean mightHaveTopic(long topicHash) {
        lock.readLock().lock();
        try {
            return bloom.mightContain(topicHash);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Rebuilds the Bloom filter with a new set of topic hashes.
     * 
     * <p>Acquires write lock to ensure thread safety.
     * The filter size is adjusted to the maximum of expectedTopics and the size of the new set.
     * 
     * @param topicHashes new set of topic hashes to populate the filter
     */
    public void rebuild(Set<Long> topicHashes) {
        lock.writeLock().lock();
        try {
            int newSize = Math.max(topicHashes.size(), expectedTopics);
            this.bloom = createBloomFilter(newSize);
            for (Long topicHash : topicHashes) {
                bloom.put(topicHash);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Clears the Bloom filter and resets it to empty state.
     * 
     * <p>Acquires write lock to ensure thread safety.
     * The filter retains the original expected topics size.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            this.bloom = createBloomFilter(expectedTopics);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
