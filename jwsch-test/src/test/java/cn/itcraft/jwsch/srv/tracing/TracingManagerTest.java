package cn.itcraft.jwsch.srv.tracing;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TracingManagerTest {
    
    private TracingManager manager;
    
    @Before
    public void setUp() {
        manager = new TracingManager(true, 1.0);
    }
    
    @Test
    public void testStartTrace() {
        TracingContext context = manager.startTrace();
        
        assertNotNull(context);
        assertNotNull(manager.currentContext());
    }
    
    @Test
    public void testEndTrace() {
        manager.startTrace();
        manager.endTrace();
        
        assertNull(manager.currentContext());
    }
    
    @Test
    public void testContinueTrace() {
        String traceId = "existing-trace-id";
        String spanId = "existing-span-id";
        
        TracingContext context = manager.continueTrace(traceId, spanId);
        
        assertEquals(traceId, context.getTraceId());
        assertEquals(spanId, context.getSpanId());
    }
    
    @Test
    public void testNewSpan() {
        TracingContext parent = manager.startTrace();
        TracingContext child = manager.newSpan();
        
        assertEquals(parent.getTraceId(), child.getTraceId());
        assertEquals(parent.getSpanId(), child.getParentSpanId());
    }
    
    @Test
    public void testAddBaggage() {
        manager.startTrace();
        manager.addBaggage("userId", "12345");
        
        assertEquals("12345", manager.currentContext().getBaggage("userId"));
    }
    
    @Test
    public void testDisabled() {
        TracingManager disabledManager = new TracingManager(false, 1.0);
        
        TracingContext context = disabledManager.startTrace();
        
        assertNull(context);
    }
    
    @Test
    public void testSampleRate() {
        TracingManager zeroRateManager = new TracingManager(true, 0.0);
        
        TracingContext context = zeroRateManager.startTrace();
        
        assertNull(context);
    }
    
    @Test
    public void testRecorder() {
        StringBuilder recorder = new StringBuilder();
        manager.addRecorder("test", ctx -> recorder.append(ctx.getTraceId()));
        
        TracingContext context = manager.startTrace();
        manager.endTrace();
        
        assertEquals(context.getTraceId(), recorder.toString());
    }
}