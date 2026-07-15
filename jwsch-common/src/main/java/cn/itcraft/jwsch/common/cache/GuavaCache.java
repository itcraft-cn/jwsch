package cn.itcraft.jwsch.common.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public final class GuavaCache<K, V> implements Cache<K, V> {
    
    private final com.google.common.cache.Cache<K, V> cache;
    private final LoadingCache<K, V> loadingCache;
    
    public GuavaCache(CacheConfig config) {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder()
            .initialCapacity(config.getInitialCapacity())
            .concurrencyLevel(config.getConcurrencyLevel());
        
        if (config.getMaximumSize() < Long.MAX_VALUE) {
            builder.maximumSize(config.getMaximumSize());
        }
        
        if (config.getExpireAfterWriteMs() > 0) {
            builder.expireAfterWrite(config.getExpireAfterWriteMs(), TimeUnit.MILLISECONDS);
        }
        
        if (config.getExpireAfterAccessMs() > 0) {
            builder.expireAfterAccess(config.getExpireAfterAccessMs(), TimeUnit.MILLISECONDS);
        }
        
        if (config.getRefreshAfterWriteMs() > 0) {
            builder.refreshAfterWrite(config.getRefreshAfterWriteMs(), TimeUnit.MILLISECONDS);
        }
        
        if (config.isRecordStats()) {
            builder.recordStats();
        }
        
        this.cache = builder.build();
        this.loadingCache = null;
    }
    
    private GuavaCache(LoadingCache<K, V> loadingCache) {
        this.cache = loadingCache;
        this.loadingCache = loadingCache;
    }
    
    public static <K, V> GuavaCache<K, V> createLoadingCache(
            CacheConfig config, 
            cn.itcraft.jwsch.common.cache.CacheLoader<K, V> loader) {
        
        CacheLoader<K, V> guavaLoader = new CacheLoader<K, V>() {
            @Override
            public V load(K key) throws Exception {
                return loader.load(key);
            }
        };
        
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder()
            .initialCapacity(config.getInitialCapacity())
            .concurrencyLevel(config.getConcurrencyLevel());
        
        if (config.getMaximumSize() < Long.MAX_VALUE) {
            builder.maximumSize(config.getMaximumSize());
        }
        
        if (config.getExpireAfterWriteMs() > 0) {
            builder.expireAfterWrite(config.getExpireAfterWriteMs(), TimeUnit.MILLISECONDS);
        }
        
        if (config.getExpireAfterAccessMs() > 0) {
            builder.expireAfterAccess(config.getExpireAfterAccessMs(), TimeUnit.MILLISECONDS);
        }
        
        if (config.getRefreshAfterWriteMs() > 0) {
            builder.refreshAfterWrite(config.getRefreshAfterWriteMs(), TimeUnit.MILLISECONDS);
        }
        
        if (config.isRecordStats()) {
            builder.recordStats();
        }
        
        return new GuavaCache<>(builder.build(guavaLoader));
    }
    
    @Override
    public V get(K key) {
        return cache.getIfPresent(key);
    }
    
    @Override
    public V get(K key, cn.itcraft.jwsch.common.cache.CacheLoader<K, V> loader) {
        try {
            if (loadingCache != null) {
                return loadingCache.get(key);
            }
            
            V value = cache.getIfPresent(key);
            if (value == null && loader != null) {
                value = loader.load(key);
                if (value != null) {
                    cache.put(key, value);
                }
            }
            return value;
        } catch (ExecutionException e) {
            throw new CacheException("Failed to get value for key: " + key, e.getCause());
        } catch (Exception e) {
            throw new CacheException("Failed to load value for key: " + key, e);
        }
    }
    
    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }
    
    @Override
    public void remove(K key) {
        cache.invalidate(key);
    }
    
    @Override
    public boolean containsKey(K key) {
        return cache.getIfPresent(key) != null;
    }
    
    @Override
    public int size() {
        return (int) cache.size();
    }
    
    @Override
    public void clear() {
        cache.invalidateAll();
    }
}