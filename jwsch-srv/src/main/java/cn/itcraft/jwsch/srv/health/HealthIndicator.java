package cn.itcraft.jwsch.srv.health;

public interface HealthIndicator {
    
    String getName();
    
    HealthStatus check();
}