package cn.itcraft.jwsch.common.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ConcurrentHashMap-based cache implementation.
 *
 * <p>Simple in-memory cache using Java's ConcurrentHashMap.
 * Suitable for small to medium-sized caches with high concurrency.
 *
 * <p>Features:
 * <ul>
 *   <li>Thread-safe operations</li>
 *   <li>Configurable initial capacity and concurrency level</li>
 *   <li>CacheLoader support for loading missing entries</li>
 *   <li>No eviction policy (caller must manage size)</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 * CacheConfig config = CacheConfig.builder()
 *     .initialCapacity(1000)
 *     .concurrencyLevel(16)
 *     .build();
 * Cache<String, User> cache = new ConcurrentHashMapCache<>(config);
 * cache.put("user1", user);
 * User user = cache.get("user1");
 * </pre>
 */
public final class ConcurrentHashMapCache<K, V> implements Cache<K, V> {

    private final ConcurrentMap<K, V> cache;

    /**
     * Creates a cache with default configuration.
     */
    public ConcurrentHashMapCache() {
        this(new CacheConfig.Builder().build());
    }

    /**
     * Creates a cache with specified configuration.
     *
     * @param config cache configuration
     */
    public ConcurrentHashMapCache(CacheConfig config) {
        this.cache = new ConcurrentHashMap<>(
                config.getInitialCapacity(),
                0.75f,
                config.getConcurrencyLevel()
        );
    }

    @Override
    public V get(K key) {
        return cache.get(key);
    }

    @Override
    public V get(K key, CacheLoader<K, V> loader) {
        V value = cache.get(key);
        if (value == null && loader != null) {
            try {
                value = loader.load(key);
                if (value != null) {
                    V existing = cache.putIfAbsent(key, value);
                    if (existing != null) {
                        value = existing;
                    }
                }
            } catch (Exception e) {
                throw new CacheException("Failed to load value for key: " + key, e);
            }
        }
        return value;
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void remove(K key) {
        cache.remove(key);
    }

    @Override
    public boolean containsKey(K key) {
        return cache.containsKey(key);
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
