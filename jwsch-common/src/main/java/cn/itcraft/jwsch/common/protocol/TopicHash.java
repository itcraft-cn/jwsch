package cn.itcraft.jwsch.common.protocol;

import java.nio.charset.StandardCharsets;

import net.openhft.hashing.LongHashFunction;

/**
 * Topic hash utility using xxHash64 algorithm.
 * 
 * <p>Topic strings are hashed to 64-bit long values for:
 * <ul>
 *   <li>Fast comparison in routing logic</li>
 *   <li>Compact memory storage</li>
 *   <li>Efficient BloomFilter operations</li>
 * </ul>
 * 
 * <p>xxHash64 provides excellent distribution and is the fastest
 * non-cryptographic hash algorithm available.
 */
public final class TopicHash {
    
    private static final LongHashFunction HASHER = LongHashFunction.xx();
    
    private TopicHash() {
    }
    
    /**
     * Compute xxHash64 hash of topic string.
     * 
     * @param topic the topic string to hash
     * @return 64-bit hash value
     */
    public static long hash(String topic) {
        if (topic == null || topic.isEmpty()) {
            return 0L;
        }
        byte[] bytes = topic.getBytes(StandardCharsets.UTF_8);
        return HASHER.hashBytes(bytes);
    }
}
