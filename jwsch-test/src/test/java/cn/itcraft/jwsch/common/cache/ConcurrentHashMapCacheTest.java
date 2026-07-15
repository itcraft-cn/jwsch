package cn.itcraft.jwsch.common.cache;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConcurrentHashMapCacheTest {
    
    private ConcurrentHashMapCache<String, String> cache;
    
    @Before
    public void setUp() {
        CacheConfig config = new CacheConfig.Builder()
            .initialCapacity(16)
            .concurrencyLevel(8)
            .build();
        cache = new ConcurrentHashMapCache<>(config);
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
    public void testConcurrentAccess() throws InterruptedException {
        Thread[] threads = new Thread[10];
        
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    cache.put("key-" + threadId + "-" + j, "value-" + j);
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        assertEquals(1000, cache.size());
    }
}