package cn.itcraft.jwsch.common.config;

import org.junit.Test;

import static org.junit.Assert.*;

public class WriteBufferWaterMarkTest {
    
    @Test
    public void testNormalCase() {
        WriteBufferWaterMark waterMark = new WriteBufferWaterMark(1024, 2048);
        assertEquals(1024, waterMark.getLow());
        assertEquals(2048, waterMark.getHigh());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeLow() {
        new WriteBufferWaterMark(-1, 1024);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testHighLessThanLow() {
        new WriteBufferWaterMark(2048, 1024);
    }
}