package cn.itcraft.jwsch.srv.health;

/**
 * Health status enumeration.
 *
 * <p>Simple two-state health status system:
 * <ul>
 *   <li>UP: Component is healthy and functioning normally</li>
 *   <li>DOWN: Component is unhealthy or unavailable</li>
 * </ul>
 *
 * <p>Used by {@link HealthIndicator} implementations and aggregated by
 * {@link HealthAggregator}.
 */
public enum HealthStatus {
    /**
     * Component is healthy and functioning normally.
     */
    UP,
    
    /**
     * Component is unhealthy or unavailable.
     */
    DOWN
}