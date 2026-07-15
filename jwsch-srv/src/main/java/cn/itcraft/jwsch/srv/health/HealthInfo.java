package cn.itcraft.jwsch.srv.health;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HealthInfo {
    
    private final HealthStatus status;
    private final Map<String, HealthStatus> components;
    
    private HealthInfo(HealthStatus status, Map<String, HealthStatus> components) {
        this.status = status;
        this.components = Collections.unmodifiableMap(new LinkedHashMap<>(components));
    }
    
    public HealthStatus getStatus() {
        return status;
    }
    
    public Map<String, HealthStatus> getComponents() {
        return components;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final ConcurrentMap<String, HealthStatus> components = new ConcurrentHashMap<>();
        
        public Builder withComponent(String name, HealthStatus status) {
            components.put(name, status);
            return this;
        }
        
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