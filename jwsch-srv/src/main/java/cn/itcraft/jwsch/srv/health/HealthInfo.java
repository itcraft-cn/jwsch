package cn.itcraft.jwsch.srv.health;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Health information container with overall status and component details.
 *
 * <p>Immutable class that represents the aggregated health status of the system.
 * Contains overall status (UP/DOWN) and detailed status for each component.
 */
public class HealthInfo {
    
    private final HealthStatus status;
    private final Map<String, HealthStatus> components;
    
    private HealthInfo(HealthStatus status, Map<String, HealthStatus> components) {
        this.status = status;
        this.components = Collections.unmodifiableMap(new LinkedHashMap<>(components));
    }
    
    /**
     * Returns the overall health status.
     *
     * @return the aggregated status (DOWN if any component is DOWN, otherwise UP)
     */
    public HealthStatus getStatus() {
        return status;
    }
    
    /**
     * Returns the detailed component health statuses.
     *
     * @return unmodifiable map of component names to their health status
     */
    public Map<String, HealthStatus> getComponents() {
        return components;
    }
    
    /**
     * Creates a new Builder instance.
     *
     * @return a new Builder for constructing HealthInfo
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for constructing HealthInfo instances.
     *
     * <p>Thread-safe: uses ConcurrentHashMap for component storage.
     */
    public static class Builder {
        private final ConcurrentMap<String, HealthStatus> components = new ConcurrentHashMap<>();
        
        /**
         * Adds a component health status.
         *
         * @param name the component name
         * @param status the component health status
         * @return this builder for method chaining
         */
        public Builder withComponent(String name, HealthStatus status) {
            components.put(name, status);
            return this;
        }
        
        /**
         * Builds the HealthInfo instance.
         *
         * @return the constructed HealthInfo
         */
        public HealthInfo build() {
            HealthStatus overall = calculateOverallStatus();
            return new HealthInfo(overall, components);
        }
        
        private HealthStatus calculateOverallStatus() {
            for (HealthStatus status : components.values()) {
                if (status == HealthStatus.DOWN) {
                    return HealthStatus.DOWN;
                }
            }
            return HealthStatus.UP;
        }
    }
}