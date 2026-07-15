package cn.itcraft.jwsch.srv.tracing;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    
    public static TracingContext create() {
        String traceId = generateTraceId();
        String spanId = generateSpanId();
        return new TracingContext(traceId, spanId, null, null);
    }
    
    public static TracingContext create(String traceId) {
        String spanId = generateSpanId();
        return new TracingContext(traceId, spanId, null, null);
    }
    
    public static TracingContext create(String traceId, String spanId) {
        return new TracingContext(traceId, spanId, null, null);
    }
    
    public static TracingContext create(String traceId, String spanId, String parentSpanId) {
        return new TracingContext(traceId, spanId, parentSpanId, null);
    }
    
    public TracingContext newChildSpan() {
        String childSpanId = generateSpanId();
        return new TracingContext(this.traceId, childSpanId, this.spanId, this.baggage);
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public String getSpanId() {
        return spanId;
    }
    
    public String getParentSpanId() {
        return parentSpanId;
    }
    
    public Map<String, String> getBaggage() {
        return Collections.unmodifiableMap(baggage);
    }
    
    public void setBaggage(String key, String value) {
        baggage.put(key, value);
    }
    
    public String getBaggage(String key) {
        return baggage.get(key);
    }
    
    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    private static String generateSpanId() {
        return Long.toHexString(System.nanoTime() & 0xFFFFFFFFFFL);
    }
    
    @Override
    public String toString() {
        return "TracingContext{" +
            "traceId='" + traceId + '\'' +
            ", spanId='" + spanId + '\'' +
            ", parentSpanId='" + parentSpanId + '\'' +
            '}';
    }
}