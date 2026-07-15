package cn.itcraft.jwsch.srv.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TracingManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TracingManager.class);
    
    private static final ThreadLocal<TracingContext> CURRENT_CONTEXT = new ThreadLocal<>();
    
    private final Map<String, SpanRecorder> recorders = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final double sampleRate;
    
    public TracingManager() {
        this(true, 1.0);
    }
    
    public TracingManager(boolean enabled, double sampleRate) {
        this.enabled = enabled;
        this.sampleRate = Math.max(0.0, Math.min(1.0, sampleRate));
    }
    
    public TracingContext startTrace() {
        if (!enabled) {
            return null;
        }
        
        if (!shouldSample()) {
            return null;
        }
        
        TracingContext context = TracingContext.create();
        CURRENT_CONTEXT.set(context);
        LOGGER.debug("Started trace: {}", context);
        return context;
    }
    
    public TracingContext startTrace(String traceId) {
        if (!enabled) {
            return null;
        }
        
        TracingContext context = TracingContext.create(traceId);
        CURRENT_CONTEXT.set(context);
        LOGGER.debug("Started trace with traceId: {}", context);
        return context;
    }
    
    public TracingContext continueTrace(String traceId, String spanId) {
        if (!enabled) {
            return null;
        }
        
        TracingContext context = TracingContext.create(traceId, spanId);
        CURRENT_CONTEXT.set(context);
        LOGGER.debug("Continued trace: {}", context);
        return context;
    }
    
    public TracingContext currentContext() {
        return CURRENT_CONTEXT.get();
    }
    
    public void endTrace() {
        TracingContext context = CURRENT_CONTEXT.get();
        if (context != null) {
            recordSpan(context);
            CURRENT_CONTEXT.remove();
            LOGGER.debug("Ended trace: {}", context);
        }
    }
    
    public TracingContext newSpan() {
        TracingContext current = CURRENT_CONTEXT.get();
        if (current == null) {
            return startTrace();
        }
        
        TracingContext child = current.newChildSpan();
        CURRENT_CONTEXT.set(child);
        return child;
    }
    
    public void addBaggage(String key, String value) {
        TracingContext context = CURRENT_CONTEXT.get();
        if (context != null) {
            context.setBaggage(key, value);
        }
    }
    
    public void addRecorder(String name, SpanRecorder recorder) {
        recorders.put(name, recorder);
    }
    
    public void removeRecorder(String name) {
        recorders.remove(name);
    }
    
    private void recordSpan(TracingContext context) {
        for (SpanRecorder recorder : recorders.values()) {
            try {
                recorder.record(context);
            } catch (Exception e) {
                LOGGER.error("Failed to record span", e);
            }
        }
    }
    
    private boolean shouldSample() {
        if (sampleRate >= 1.0) {
            return true;
        }
        if (sampleRate <= 0.0) {
            return false;
        }
        return Math.random() < sampleRate;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public interface SpanRecorder {
        void record(TracingContext context);
    }
}