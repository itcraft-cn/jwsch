package cn.itcraft.jwsch.srv.health;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Health aggregator that collects health status from multiple indicators.
 *
 * <p>Manages a collection of {@link HealthIndicator} instances and aggregates
 * their health statuses into a single {@link HealthInfo} object.
 *
 * <p>Thread-safe: uses {@link CopyOnWriteArrayList} for indicator management.
 */
public class HealthAggregator {
    
    private final List<HealthIndicator> indicators = new CopyOnWriteArrayList<>();
    
    /**
     * Adds a health indicator to the aggregator.
     *
     * @param indicator the health indicator to add
     */
    public void addIndicator(HealthIndicator indicator) {
        indicators.add(indicator);
    }
    
    /**
     * Removes a health indicator from the aggregator.
     *
     * @param indicator the health indicator to remove
     */
    public void removeIndicator(HealthIndicator indicator) {
        indicators.remove(indicator);
    }
    
    /**
     * Checks health status by aggregating all registered indicators.
     *
     * @return HealthInfo containing aggregated health status
     * @throws Exception if any indicator fails
     */
    public HealthInfo checkHealth() {
        HealthInfo.Builder builder = HealthInfo.builder();
        
        for (HealthIndicator indicator : indicators) {
            try {
                HealthStatus status = indicator.check();
                builder.withComponent(indicator.getName(), status);
            } catch (Exception e) {
                builder.withComponent(indicator.getName(), HealthStatus.DOWN);
            }
        }
        
        return builder.build();
    }
}