/**
 * Default thread-safe implementation of {@link Metrics}.
 * 
 * <p>Uses {@link ConcurrentHashMap} for metric storage and {@link AtomicLong}
 * for atomic operations. All methods are thread-safe.
 * 
 * <p>Metrics are lazily created on first access.
 */
public final class DefaultMetrics implements Metrics {
    
    private final Map<String, AtomicCounter> counters;
    private final Map<String, AtomicGauge> gauges;
    
    /**
     * Creates a new DefaultMetrics instance with empty metric maps.
     */
    public DefaultMetrics() {
        this.counters = new ConcurrentHashMap<>();
        this.gauges = new ConcurrentHashMap<>();
    }
    
    @Override
    public void increment(String name) {
        getOrCreateCounter(name).increment();
    }
    
    @Override
    public void increment(String name, long delta) {
        getOrCreateCounter(name).increment(delta);
    }
    
    @Override
    public void decrement(String name) {
        getOrCreateCounter(name).decrement();
    }
    
    @Override
    public void record(String name, long value) {
        getOrCreateGauge(name).set(value);
    }
    
    @Override
    public Counter getCounter(String name) {
        return getOrCreateCounter(name);
    }
    
    @Override
    public Gauge getGauge(String name) {
        return getOrCreateGauge(name);
    }
    
    @Override
    public void remove(String name) {
        counters.remove(name);
        gauges.remove(name);
    }
    
    @Override
    public void clear() {
        counters.clear();
        gauges.clear();
    }
    
    private AtomicCounter getOrCreateCounter(String name) {
        return counters.computeIfAbsent(name, k -> new AtomicCounter());
    }
    
    private AtomicGauge getOrCreateGauge(String name) {
        return gauges.computeIfAbsent(name, k -> new AtomicGauge());
    }
    
    /**
     * Thread-safe counter implementation using AtomicLong.
     */
    private static final class AtomicCounter implements Counter {
        private final AtomicLong value = new AtomicLong(0);
        
        @Override
        public void increment() {
            value.incrementAndGet();
        }
        
        @Override
        public void increment(long delta) {
            value.addAndGet(delta);
        }
        
        @Override
        public void decrement() {
            value.decrementAndGet();
        }
        
        @Override
        public void decrement(long delta) {
            value.addAndGet(-delta);
        }
        
        @Override
        public long get() {
            return value.get();
        }
        
        @Override
        public void reset() {
            value.set(0);
        }
    }
    
    /**
     * Thread-safe gauge implementation using AtomicLong.
     */
    private static final class AtomicGauge implements Gauge {
        private final AtomicLong value = new AtomicLong(0);
        
        @Override
        public void set(long value) {
            this.value.set(value);
        }
        
        @Override
        public long get() {
            return value.get();
        }
        
        @Override
        public void increment() {
            value.incrementAndGet();
        }
        
        @Override
        public void decrement() {
            value.decrementAndGet();
        }
    }
}