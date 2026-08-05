package cn.itcraft.jwsch.srv.health;

/**
 * Health indicator interface for monitoring component health.
 *
 * <p>Implementations of this interface provide health status for specific
 * components or subsystems. The {@link HealthAggregator} collects status
 * from multiple indicators to provide overall system health.
 */
public interface HealthIndicator {
    
    /**
     * Returns the name of this health indicator.
     *
     * @return the indicator name (e.g., "database", "memory", "connections")
     */
    String getName();
    
    /**
     * Checks the health status of the component.
     *
     * @return the health status (UP, DOWN, or custom status)
     * @throws Exception if the health check fails
     */
    HealthStatus check();
}