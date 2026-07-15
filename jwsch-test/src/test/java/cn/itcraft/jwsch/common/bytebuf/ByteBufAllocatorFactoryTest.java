package cn.itcraft.jwsch.common.bytebuf;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.Test;

import static org.junit.Assert.*;

public class ByteBufAllocatorFactoryTest {
    
    @Test
    public void testCreatePooledDirectAllocator() {
        ByteBufConfig config = new ByteBufConfig.Builder()
            .pooled(true)
            .direct(true)
            .build();
        
        ByteBufAllocatorFactory factory = new ByteBufAllocatorFactory(config);
        ByteBufAllocator allocator = factory.create();
        
        assertNotNull(allocator);
        assertTrue(allocator instanceof PooledByteBufAllocator);
    }
    
    @Test
    public void testCreatePooledHeapAllocator() {
        ByteBufConfig config = new ByteBufConfig.Builder()
            .pooled(true)
            .direct(false)
            .build();
        
        ByteBufAllocatorFactory factory = new ByteBufAllocatorFactory(config);
        ByteBufAllocator allocator = factory.create();
        
        assertNotNull(allocator);
        assertTrue(allocator instanceof PooledByteBufAllocator);
    }
    
    @Test
    public void testCreateUnpooledDirectAllocator() {
        ByteBufConfig config = new ByteBufConfig.Builder()
            .pooled(false)
            .direct(true)
            .build();
        
        ByteBufAllocatorFactory factory = new ByteBufAllocatorFactory(config);
        ByteBufAllocator allocator = factory.create();
        
        assertNotNull(allocator);
        assertTrue(allocator instanceof UnpooledByteBufAllocator);
    }
    
    @Test
    public void testCreateUnpooledHeapAllocator() {
        ByteBufConfig config = new ByteBufConfig.Builder()
            .pooled(false)
            .direct(false)
            .build();
        
        ByteBufAllocatorFactory factory = new ByteBufAllocatorFactory(config);
        ByteBufAllocator allocator = factory.create();
        
        assertNotNull(allocator);
        assertTrue(allocator instanceof UnpooledByteBufAllocator);
    }
    
    @Test(expected = NullPointerException.class)
    public void testCreateWithNullConfig() {
        new ByteBufAllocatorFactory(null);
    }
}