package cn.itcraft.jwsch.common.metrics;

/**
 * Gauge metric interface.
 * 
 * <p>A gauge represents a single numerical value that can go up and down.
 * Common use cases:
 * <ul>
 *   <li>Current number of active connections</li>
 *   <li>Current heap memory usage</li>
 *   <li>Current queue size</li>
 * </ul>
 */
public interface Gauge {
    
    /**
     * Sets the gauge to the specified value.
     * 
     * @param value new gauge value
     */
    void set(long value);
    
    /**
     * Returns the current gauge value.
     */
    long get();
    
    /**
     * Increments the gauge by 1.
     */
    void increment();
    
    /**
     * Decrements the gauge by 1.
     */
    void decrement();
}