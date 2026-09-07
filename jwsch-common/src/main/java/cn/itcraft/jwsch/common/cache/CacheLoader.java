package cn.itcraft.jwsch.common.cache;

/**
 * Functional interface for loading cache entries.
 *
 * <p>Used by Cache.get(key, loader) to load missing entries.
 * Implementations should be thread-safe.
 *
 * @param <K> key type
 * @param <V> value type
 */
@FunctionalInterface
public interface CacheLoader<K, V> {
    
    /**
     * Loads value for given key.
     *
     * @param key cache key
     * @return loaded value
     * @throws Exception if loading fails
     */
    V load(K key) throws Exception;
}