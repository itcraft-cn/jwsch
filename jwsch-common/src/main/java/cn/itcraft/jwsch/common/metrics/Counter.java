/**
 * Counter metric interface.
 * 
 * <p>A counter is a cumulative metric that can only increase or be reset to zero.
 * Common use cases:
 * <ul>
 *   <li>Total number of requests processed</li>
 *   <li>Total number of errors occurred</li>
 *   <li>Total bytes sent/received</li>
 * </ul>
 */
public interface Counter {
    
    /**
     * Increments the counter by 1.
     */
    void increment();
    
    /**
     * Increments the counter by the specified delta.
     * 
     * @param delta positive value to add
     */
    void increment(long delta);
    
    /**
     * Decrements the counter by 1.
     * 
     * <p>Note: Counters are typically monotonic increasing.
     * Decrement is provided for completeness but should be used cautiously.
     */
    void decrement();
    
    /**
     * Decrements the counter by the specified delta.
     * 
     * @param delta positive value to subtract
     */
    void decrement(long delta);
    
    /**
     * Returns the current counter value.
     */
    long get();
    
    /**
     * Resets the counter to zero.
     */
    void reset();
}