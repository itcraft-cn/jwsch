package cn.itcraft.jwsch.common.cache;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GuavaCacheTest {
    
    private GuavaCache<String, String> cache;
    
    @Before
    public void setUp() {
        CacheConfig config = new CacheConfig.Builder()
            .initialCapacity(16)
            .concurrencyLevel(8)
            .maximumSize(100)
            .build();
        cache = new GuavaCache<>(config);
    }
    
    @Test
    public void testPutAndGet() {
        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"));
    }
    
    @Test
    public void testGet_nonExisting() {
        assertNull(cache.get("non-existing"));
    }
    
    @Test
    public void testGet_withLoader() {
        String value = cache.get("key1", key -> "loaded-" + key);
        assertEquals("loaded-key1", value);
        assertEquals("loaded-key1", cache.get("key1"));
    }
    
    @Test
    public void testRemove() {
        cache.put("key1", "value1");
        cache.remove("key1");
        assertNull(cache.get("key1"));
    }
    
    @Test
    public void testContainsKey() {
        cache.put("key1", "value1");
        assertTrue(cache.containsKey("key1"));
        assertFalse(cache.containsKey("key2"));
    }
    
    @Test
    public void testSize() {
        assertEquals(0, cache.size());
        cache.put("key1", "value1");
        assertEquals(1, cache.size());
        cache.put("key2", "value2");
        assertEquals(2, cache.size());
    }
    
    @Test
    public void testClear() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.clear();
        assertEquals(0, cache.size());
    }
    
    @Test
    public void testMaximumSize_eviction() throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            cache.put("key" + i, "value" + i);
        }
        
        Thread.sleep(100);
        
        assertTrue(cache.size() <= 100);
    }
    
    @Test
    public void testCreateLoadingCache() {
        CacheConfig config = new CacheConfig.Builder()
            .initialCapacity(16)
            .build();
        
        GuavaCache<String, String> loadingCache = GuavaCache.createLoadingCache(
            config, 
            key -> "auto-loaded-" + key
        );
        
        String value = loadingCache.get("key1", null);
        assertEquals("auto-loaded-key1", value);
    }
}