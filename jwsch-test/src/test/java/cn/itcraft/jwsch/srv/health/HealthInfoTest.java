package cn.itcraft.jwsch.srv.health;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class HealthInfoTest {
    
    @Test
    public void testBuilder_allUp() {
        HealthInfo info = HealthInfo.builder()
            .withComponent("websocket", HealthStatus.UP)
            .withComponent("tcp", HealthStatus.UP)
            .build();
        
        assertEquals(HealthStatus.UP, info.getStatus());
        assertEquals(2, info.getComponents().size());
        assertEquals(HealthStatus.UP, info.getComponents().get("websocket"));
    }
    
    @Test
    public void testBuilder_oneDown() {
        HealthInfo info = HealthInfo.builder()
            .withComponent("websocket", HealthStatus.UP)
            .withComponent("tcp", HealthStatus.DOWN)
            .build();
        
        assertEquals(HealthStatus.DOWN, info.getStatus());
        assertEquals(2, info.getComponents().size());
    }
    
    @Test
    public void testBuilder_empty() {
        HealthInfo info = HealthInfo.builder().build();
        
        assertEquals(HealthStatus.UP, info.getStatus());
        assertTrue(info.getComponents().isEmpty());
    }
    
    @Test
    public void testGetComponents_unmodifiable() {
        HealthInfo info = HealthInfo.builder()
            .withComponent("test", HealthStatus.UP)
            .build();
        
        Map<String, HealthStatus> components = info.getComponents();
        
        try {
            components.put("new", HealthStatus.DOWN);
            fail("Should throw exception");
        } catch (UnsupportedOperationException e) {
        }
    }
}