package cn.itcraft.jwsch.srv.tracing;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Distributed tracing context for tracking request flow across services.
 * 
 * <p>Represents a span in a distributed trace, containing:
 * <ul>
 *   <li>Trace ID - unique identifier for the entire trace</li>
 *   <li>Span ID - unique identifier for this span</li>
 *   <li>Parent Span ID - identifier of the parent span (null for root spans)</li>
 *   <li>Baggage - key-value pairs for cross-service context propagation</li>
 * </ul>
 * 
 * <p>Immutable once created, except for baggage which can be modified through setter methods.
 * Thread-safe for read operations; baggage modifications are not thread-safe.
 * 
 * <p>Follows OpenTelemetry-like tracing concepts but simplified for JWSch internal use.
 */
public class TracingContext {
    
    private final String traceId;
    private final String spanId;
    private final String parentSpanId;
    private final Map<String, String> baggage;
    
    private TracingContext(String traceId, String spanId, String parentSpanId, Map<String, String> baggage) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.baggage = baggage != null ? new HashMap<>(baggage) : new HashMap<>();
    }
    
    /**
     * Creates a new root tracing context with auto-generated trace and span IDs.
     * 
     * @return new TracingContext as root span
     */
    public static TracingContext create() {
        String traceId = generateTraceId();
        String spanId = generateSpanId();
        return new TracingContext(traceId, spanId, null, null);
    }
    
    /**
     * Creates a new tracing context with the given trace ID and auto-generated span ID.
     * 
     * @param traceId the trace ID to use
     * @return new TracingContext as child span within the given trace
     */
    public static TracingContext create(String traceId) {
        String spanId = generateSpanId();
        return new TracingContext(traceId, spanId, null, null);
    }
    
    /**
     * Creates a new tracing context with the given trace ID and span ID.
     * 
     * @param traceId the trace ID to use
     * @param spanId the span ID to use
     * @return new TracingContext with specified IDs
     */
    public static TracingContext create(String traceId, String spanId) {
        return new TracingContext(traceId, spanId, null, null);
    }
    
    /**
     * Creates a new tracing context with the given trace ID, span ID, and parent span ID.
     * 
     * @param traceId the trace ID to use
     * @param spanId the span ID to use
     * @param parentSpanId the parent span ID
     * @return new TracingContext with specified IDs and parent relationship
     */
    public static TracingContext create(String traceId, String spanId, String parentSpanId) {
        return new TracingContext(traceId, spanId, parentSpanId, null);
    }
    
    /**
     * Creates a new child span context from this context.
     * <p>The child inherits the trace ID and baggage from this context.
     * The child's parent span ID is set to this span's span ID.
     * 
     * @return new TracingContext as child span
     */
    public TracingContext newChildSpan() {
        String childSpanId = generateSpanId();
        return new TracingContext(this.traceId, childSpanId, this.spanId, this.baggage);
    }
    
    /**
     * Returns the trace ID.
     * 
     * @return trace ID
     */
    public String getTraceId() {
        return traceId;
    }
    
    /**
     * Returns the span ID.
     * 
     * @return span ID
     */
    public String getSpanId() {
        return spanId;
    }
    
    /**
     * Returns the parent span ID, or null if this is a root span.
     * 
     * @return parent span ID, or null
     */
    public String getParentSpanId() {
        return parentSpanId;
    }
    
    /**
     * Returns an unmodifiable view of the baggage map.
     * 
     * @return baggage map (immutable)
     */
    public Map<String, String> getBaggage() {
        return Collections.unmodifiableMap(baggage);
    }
    
    /**
     * Sets a baggage key-value pair for cross-service context propagation.
     * 
     * @param key baggage key
     * @param value baggage value
     */
    public void setBaggage(String key, String value) {
        baggage.put(key, value);
    }
    
    /**
     * Gets a baggage value by key.
     * 
     * @param key baggage key
     * @return baggage value, or null if key not found
     */
    public String getBaggage(String key) {
        return baggage.get(key);
    }
    
    /**
     * Generates a trace ID using UUID without hyphens.
     * 
     * @return trace ID string
     */
    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Generates a span ID using current nano-time masked to 40 bits.
     * 
     * @return span ID as hexadecimal string
     */
    private static String generateSpanId() {
        return Long.toHexString(System.nanoTime() & 0xFFFFFFFFFFL);
    }
    
    /**
     * Returns a string representation for debugging.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        return "TracingContext{" +
            "traceId='" + traceId + '\'' +
            ", spanId='" + spanId + '\'' +
            ", parentSpanId='" + parentSpanId + '\'' +
            '}';
    }
}