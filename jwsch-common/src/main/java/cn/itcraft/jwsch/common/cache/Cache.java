package cn.itcraft.jwsch.common.cache;

/**
 * Cache interface for key-value storage.
 *
 * <p>Provides basic cache operations with optional CacheLoader support.
 * Implementations should be thread-safe.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface Cache<K, V> {
    
    /**
     * Retrieves value for key, returns null if not found.
     *
     * @param key cache key
     * @return cached value or null
     */
    V get(K key);
    
    /**
     * Retrieves value for key, loads via CacheLoader if not found.
     *
     * @param key cache key
     * @param loader CacheLoader to load missing values (can be null)
     * @return cached or loaded value
     * @throws CacheException if loader fails
     */
    V get(K key, CacheLoader<K, V> loader);
    
    /**
     * Stores key-value pair in cache.
     *
     * @param key cache key
     * @param value value to store
     */
    void put(K key, V value);
    
    /**
     * Removes key from cache.
     *
     * @param key key to remove
     */
    void remove(K key);
    
    /**
     * Checks if key exists in cache.
     *
     * @param key key to check
     * @return true if key exists
     */
    boolean containsKey(K key);
    
    /**
     * Returns number of entries in cache.
     *
     * @return cache size
     */
    int size();
    
    /**
     * Removes all entries from cache.
     */
    void clear();
}