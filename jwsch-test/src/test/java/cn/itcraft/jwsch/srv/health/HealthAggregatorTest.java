package cn.itcraft.jwsch.srv.health;

import org.junit.Test;

import static org.junit.Assert.*;

public class HealthAggregatorTest {
    
    @Test
    public void testCheckHealth_allUp() {
        HealthAggregator aggregator = new HealthAggregator();
        aggregator.addIndicator(new TestIndicator("comp1", HealthStatus.UP));
        aggregator.addIndicator(new TestIndicator("comp2", HealthStatus.UP));
        
        HealthInfo info = aggregator.checkHealth();
        
        assertEquals(HealthStatus.UP, info.getStatus());
        assertEquals(2, info.getComponents().size());
    }
    
    @Test
    public void testCheckHealth_oneDown() {
        HealthAggregator aggregator = new HealthAggregator();
        aggregator.addIndicator(new TestIndicator("comp1", HealthStatus.UP));
        aggregator.addIndicator(new TestIndicator("comp2", HealthStatus.DOWN));
        
        HealthInfo info = aggregator.checkHealth();
        
        assertEquals(HealthStatus.DOWN, info.getStatus());
    }
    
    @Test
    public void testCheckHealth_empty() {
        HealthAggregator aggregator = new HealthAggregator();
        
        HealthInfo info = aggregator.checkHealth();
        
        assertEquals(HealthStatus.UP, info.getStatus());
    }
    
    @Test
    public void testRemoveIndicator() {
        HealthAggregator aggregator = new HealthAggregator();
        TestIndicator indicator = new TestIndicator("comp1", HealthStatus.DOWN);
        aggregator.addIndicator(indicator);
        
        aggregator.removeIndicator(indicator);
        
        HealthInfo info = aggregator.checkHealth();
        assertEquals(HealthStatus.UP, info.getStatus());
    }
    
    @Test
    public void testCheckHealth_exceptionReturnsDown() {
        HealthAggregator aggregator = new HealthAggregator();
        aggregator.addIndicator(new HealthIndicator() {
            @Override
            public String getName() {
                return "failing";
            }
            
            @Override
            public HealthStatus check() {
                throw new RuntimeException("test error");
            }
        });
        
        HealthInfo info = aggregator.checkHealth();
        
        assertEquals(HealthStatus.DOWN, info.getStatus());
        assertEquals(HealthStatus.DOWN, info.getComponents().get("failing"));
    }
    
    private static class TestIndicator implements HealthIndicator {
        private final String name;
        private final HealthStatus status;
        
        TestIndicator(String name, HealthStatus status) {
            this.name = name;
            this.status = status;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public HealthStatus check() {
            return status;
        }
    }
}