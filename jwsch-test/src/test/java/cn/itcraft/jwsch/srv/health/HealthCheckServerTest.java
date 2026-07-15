package cn.itcraft.jwsch.srv.health;

import cn.itcraft.jwsch.srv.config.HealthConfig;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class HealthCheckServerTest {
    
    private static final AtomicInteger PORT_COUNTER = new AtomicInteger(50000);
    
    private HealthCheckServer server;
    
    @After
    public void tearDown() {
        if (server != null) {
            server.shutdown();
        }
    }
    
    @Test
    public void testStart_shutdown() throws InterruptedException {
        HealthConfig config = HealthConfig.builder()
            .enabled(true)
            .port(PORT_COUNTER.getAndIncrement())
            .build();
        server = new HealthCheckServer(config);
        server.start();
        
        assertTrue(server.getPort() > 0);
        
        server.shutdown();
    }
    
    @Test
    public void testAddIndicator() {
        HealthConfig config = HealthConfig.builder()
            .enabled(true)
            .port(PORT_COUNTER.getAndIncrement())
            .build();
        server = new HealthCheckServer(config);
        
        HealthIndicator indicator = new HealthIndicator() {
            @Override
            public String getName() {
                return "test";
            }
            
            @Override
            public HealthStatus check() {
                return HealthStatus.UP;
            }
        };
        
        server.addIndicator(indicator);
        
        HealthInfo info = server.getHealthAggregator().checkHealth();
        assertEquals(HealthStatus.UP, info.getStatus());
    }
    
    @Test
    public void testGetHealthAggregator() {
        HealthConfig config = HealthConfig.builder()
            .enabled(true)
            .port(PORT_COUNTER.getAndIncrement())
            .build();
        server = new HealthCheckServer(config);
        
        assertNotNull(server.getHealthAggregator());
    }
}