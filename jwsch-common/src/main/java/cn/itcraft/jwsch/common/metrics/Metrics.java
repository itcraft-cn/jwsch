/**
 * Metrics collection interface.
 * 
 * <p>Central interface for collecting and reporting application metrics.
 * Supports counters and gauges with namespaced metric names.
 * 
 * <p>Metric names follow dot-separated hierarchy convention:
 * <pre>
 *   connections.active
 *   packets.sent.total
 *   errors.decode
 * </pre>
 */
public interface Metrics {
    
    /**
     * Increments the named counter by 1.
     * 
     * @param name metric name
     */
    void increment(String name);
    
    /**
     * Increments the named counter by specified delta.
     * 
     * @param name metric name
     * @param delta positive value to add
     */
    void increment(String name, long delta);
    
    /**
     * Decrements the named counter by 1.
     * 
     * @param name metric name
     */
    void decrement(String name);
    
    /**
     * Records a value for the named gauge.
     * 
     * <p>If the gauge doesn't exist, it will be created.
     * 
     * @param name metric name
     * @param value current gauge value
     */
    void record(String name, long value);
    
    /**
     * Gets or creates a Counter with the specified name.
     * 
     * @param name metric name
     * @return existing or newly created Counter
     */
    Counter getCounter(String name);
    
    /**
     * Gets or creates a Gauge with the specified name.
     * 
     * @param name metric name
     * @return existing or newly created Gauge
     */
    Gauge getGauge(String name);
    
    /**
     * Removes the named metric.
     * 
     * @param name metric name to remove
     */
    void remove(String name);
    
    /**
     * Removes all metrics.
     */
    void clear();
}