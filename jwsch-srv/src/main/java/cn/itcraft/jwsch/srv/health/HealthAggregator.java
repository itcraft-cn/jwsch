package cn.itcraft.jwsch.srv.health;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HealthAggregator {
    
    private final List<HealthIndicator> indicators = new CopyOnWriteArrayList<>();
    
    public void addIndicator(HealthIndicator indicator) {
        indicators.add(indicator);
    }
    
    public void removeIndicator(HealthIndicator indicator) {
        indicators.remove(indicator);
    }
    
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