package cn.itcraft.jwsch.common.bytebuf;

import org.junit.Test;

import static org.junit.Assert.*;

public class ByteBufConfigTest {
    
    @Test
    public void testDefaultValues() {
        ByteBufConfig config = new ByteBufConfig.Builder().build();
        
        assertTrue(config.isPooled());
        assertTrue(config.isDirect());
        assertEquals("SIMPLE", config.getLeakDetection());
    }
    
    @Test
    public void testBuilderWithCustomValues() {
        ByteBufConfig config = new ByteBufConfig.Builder()
            .pooled(false)
            .direct(false)
            .leakDetection("ADVANCED")
            .build();
        
        assertFalse(config.isPooled());
        assertFalse(config.isDirect());
        assertEquals("ADVANCED", config.getLeakDetection());
    }
    
    @Test
    public void testBuilderChaining() {
        ByteBufConfig.Builder builder = new ByteBufConfig.Builder();
        
        assertSame(builder, builder.pooled(true));
        assertSame(builder, builder.direct(true));
        assertSame(builder, builder.leakDetection("SIMPLE"));
        
        ByteBufConfig config = builder.build();
        assertNotNull(config);
    }
}