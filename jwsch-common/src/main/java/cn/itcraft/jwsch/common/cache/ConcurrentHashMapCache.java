package cn.itcraft.jwsch.common.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ConcurrentHashMapCache<K, V> implements Cache<K, V> {
    
    private final ConcurrentMap<K, V> cache;
    
    public ConcurrentHashMapCache() {
        this(new CacheConfig.Builder().build());
    }
    
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