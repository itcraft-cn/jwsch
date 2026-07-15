package cn.itcraft.jwsch.common.cache;

@FunctionalInterface
public interface CacheLoader<K, V> {
    
    V load(K key) throws Exception;
}