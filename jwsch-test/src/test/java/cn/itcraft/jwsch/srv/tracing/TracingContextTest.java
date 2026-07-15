package cn.itcraft.jwsch.srv.tracing;

import org.junit.Test;

import static org.junit.Assert.*;

public class TracingContextTest {
    
    @Test
    public void testCreate() {
        TracingContext context = TracingContext.create();
        
        assertNotNull(context.getTraceId());
        assertNotNull(context.getSpanId());
        assertNull(context.getParentSpanId());
        assertTrue(context.getBaggage().isEmpty());
    }
    
    @Test
    public void testCreateWithTraceId() {
        String traceId = "abc123";
        TracingContext context = TracingContext.create(traceId);
        
        assertEquals(traceId, context.getTraceId());
        assertNotNull(context.getSpanId());
    }
    
    @Test
    public void testCreateWithTraceIdAndSpanId() {
        String traceId = "abc123";
        String spanId = "def456";
        TracingContext context = TracingContext.create(traceId, spanId);
        
        assertEquals(traceId, context.getTraceId());
        assertEquals(spanId, context.getSpanId());
    }
    
    @Test
    public void testCreateWithParent() {
        String traceId = "abc123";
        String spanId = "def456";
        String parentSpanId = "parent789";
        TracingContext context = TracingContext.create(traceId, spanId, parentSpanId);
        
        assertEquals(traceId, context.getTraceId());
        assertEquals(spanId, context.getSpanId());
        assertEquals(parentSpanId, context.getParentSpanId());
    }
    
    @Test
    public void testNewChildSpan() {
        TracingContext parent = TracingContext.create();
        TracingContext child = parent.newChildSpan();
        
        assertEquals(parent.getTraceId(), child.getTraceId());
        assertNotEquals(parent.getSpanId(), child.getSpanId());
        assertEquals(parent.getSpanId(), child.getParentSpanId());
    }
    
    @Test
    public void testBaggage() {
        TracingContext context = TracingContext.create();
        
        context.setBaggage("key1", "value1");
        context.setBaggage("key2", "value2");
        
        assertEquals("value1", context.getBaggage("key1"));
        assertEquals("value2", context.getBaggage("key2"));
        assertNull(context.getBaggage("nonexistent"));
        assertEquals(2, context.getBaggage().size());
    }
    
    @Test
    public void testBaggageInheritedByChild() {
        TracingContext parent = TracingContext.create();
        parent.setBaggage("key1", "value1");
        
        TracingContext child = parent.newChildSpan();
        
        assertEquals("value1", child.getBaggage("key1"));
    }
}