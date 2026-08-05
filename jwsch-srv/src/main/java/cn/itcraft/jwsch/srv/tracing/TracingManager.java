package cn.itcraft.jwsch.srv.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for distributed tracing within the JWSch server.
 * 
 * <p>Provides thread-local storage of the current tracing context and methods to
 * start, continue, and end traces. Supports sampling rate configuration to reduce
 * overhead in high-throughput environments.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Thread-local context management using {@link ThreadLocal}</li>
 *   <li>Configurable sampling rate (0.0 to 1.0)</li>
 *   <li>Span recorder registry for exporting trace data</li>
 *   <li>Baggage manipulation for cross-service context propagation</li>
 *   <li>Automatic child span creation</li>
 * </ul>
 * 
 * <p>Thread-safe for concurrent access; uses {@link ConcurrentHashMap} for recorder storage.
 */
public class TracingManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TracingManager.class);
    
    private static final ThreadLocal<TracingContext> CURRENT_CONTEXT = new ThreadLocal<>();
    
    private final Map<String, SpanRecorder> recorders = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final double sampleRate;
    
    /**
     * Creates a new TracingManager with tracing enabled and 100% sampling rate.
     */
    public TracingManager() {
        this(true, 1.0);
    }
    
    /**
     * Creates a new TracingManager with the specified configuration.
     * 
     * @param enabled whether tracing is enabled
     * @param sampleRate sampling rate between 0.0 (no sampling) and 1.0 (sample all)
     */
    public TracingManager(boolean enabled, double sampleRate) {
        this.enabled = enabled;
        this.sampleRate = Math.max(0.0, Math.min(1.0, sampleRate));
    }
    
    /**
     * Starts a new trace with auto-generated trace ID.
     * <p>If tracing is disabled or sampling decides not to sample, returns null.
     * 
     * @return new TracingContext, or null if not sampled or disabled
     */
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
    
    /**
     * Starts a new trace with the given trace ID.
     * <p>If tracing is disabled, returns null.
     * 
     * @param traceId the trace ID to use
     * @return new TracingContext, or null if tracing disabled
     */
    public TracingContext startTrace(String traceId) {
        if (!enabled) {
            return null;
        }
        
        TracingContext context = TracingContext.create(traceId);
        CURRENT_CONTEXT.set(context);
        LOGGER.debug("Started trace with traceId: {}", context);
        return context;
    }
    
    /**
     * Continues an existing trace with the given trace ID and span ID.
     * <p>Creates a new span within the existing trace, typically used when receiving
     * trace context from another service.
     * 
     * @param traceId the trace ID to continue
     * @param spanId the span ID for the new span
     * @return new TracingContext, or null if tracing disabled
     */
    public TracingContext continueTrace(String traceId, String spanId) {
        if (!enabled) {
            return null;
        }
        
        TracingContext context = TracingContext.create(traceId, spanId);
        CURRENT_CONTEXT.set(context);
        LOGGER.debug("Continued trace: {}", context);
        return context;
    }
    
    /**
     * Returns the current tracing context for this thread.
     * 
     * @return current TracingContext, or null if none set
     */
    public TracingContext currentContext() {
        return CURRENT_CONTEXT.get();
    }
    
    /**
     * Ends the current trace and records the span with all registered recorders.
     * <p>Removes the context from thread-local storage after recording.
     */
    public void endTrace() {
        TracingContext context = CURRENT_CONTEXT.get();
        if (context != null) {
            recordSpan(context);
            CURRENT_CONTEXT.remove();
            LOGGER.debug("Ended trace: {}", context);
        }
    }
    
    /**
     * Creates a new child span from the current context.
     * <p>If no current context exists, starts a new trace.
     * 
     * @return new child TracingContext
     */
    public TracingContext newSpan() {
        TracingContext current = CURRENT_CONTEXT.get();
        if (current == null) {
            return startTrace();
        }
        
        TracingContext child = current.newChildSpan();
        CURRENT_CONTEXT.set(child);
        return child;
    }
    
    /**
     * Adds baggage to the current tracing context.
     * 
     * @param key baggage key
     * @param value baggage value
     */
    public void addBaggage(String key, String value) {
        TracingContext context = CURRENT_CONTEXT.get();
        if (context != null) {
            context.setBaggage(key, value);
        }
    }
    
    /**
     * Adds a span recorder for exporting trace data.
     * 
     * @param name recorder name
     * @param recorder span recorder implementation
     */
    public void addRecorder(String name, SpanRecorder recorder) {
        recorders.put(name, recorder);
    }
    
    /**
     * Removes a span recorder by name.
     * 
     * @param name recorder name
     */
    public void removeRecorder(String name) {
        recorders.remove(name);
    }
    
    /**
     * Records a span with all registered recorders.
     * 
     * @param context the tracing context to record
     */
    private void recordSpan(TracingContext context) {
        for (SpanRecorder recorder : recorders.values()) {
            try {
                recorder.record(context);
            } catch (Exception e) {
                LOGGER.error("Failed to record span", e);
            }
        }
    }
    
    /**
     * Determines whether the current operation should be sampled based on sampling rate.
     * 
     * @return true if should sample, false otherwise
     */
    private boolean shouldSample() {
        if (sampleRate >= 1.0) {
            return true;
        }
        if (sampleRate <= 0.0) {
            return false;
        }
        return Math.random() < sampleRate;
    }
    
    /**
     * Checks if tracing is enabled.
     * 
     * @return true if tracing enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Interface for span recorders that export trace data.
     */
    public interface SpanRecorder {
        /**
         * Records a span.
         * 
         * @param context the tracing context to record
         */
        void record(TracingContext context);
    }
}