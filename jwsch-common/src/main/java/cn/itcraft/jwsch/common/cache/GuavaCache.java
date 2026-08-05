/**
 * Guava-based cache implementation.
 *
 * <p>Uses Google Guava CacheBuilder for feature-rich caching:
 * <ul>
 *   <li>Size-based eviction</li>
 *   <li>Time-based expiration (write/access)</li>
 *   <li>Automatic refresh</li>
 *   <li>Statistics collection</li>
 *   <li>Asynchronous loading</li>
 * </ul>
 *
 * <p>Two modes:
 * <ul>
 *   <li>Simple cache: Basic get/put operations</li>
 *   <li>Loading cache: Automatic loading via CacheLoader</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 * CacheConfig config = CacheConfig.builder()
 *     .maximumSize(10000)
 *     .expireAfterWrite(10, TimeUnit.MINUTES)
 *     .recordStats(true)
 *     .build();
 * Cache<String, User> cache = new GuavaCache<>(config);
 * </pre>
 */
public final class GuavaCache<K, V> implements Cache<K, V> {
    
    private final com.google.common.cache.Cache<K, V> cache;
    private final LoadingCache<K, V> loadingCache;
    
    /**
     * Creates a simple Guava cache with configuration.
     *
     * @param config cache configuration
     */
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
    
    /**
     * Creates a loading cache with CacheLoader.
     *
     * @param config cache configuration
     * @param loader CacheLoader for loading missing entries
     * @return GuavaCache instance
     * @param <K> key type
     * @param <V> value type
     */
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