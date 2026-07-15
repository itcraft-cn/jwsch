package cn.itcraft.jwsch.common.cache;

public interface Cache<K, V> {
    
    V get(K key);
    
    V get(K key, CacheLoader<K, V> loader);
    
    void put(K key, V value);
    
    void remove(K key);
    
    boolean containsKey(K key);
    
    int size();
    
    void clear();
}