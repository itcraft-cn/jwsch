package cn.itcraft.jwsch.srv.registry;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class InMemoryServiceRegistryTest {
    
    private InMemoryServiceRegistry registry;
    
    @Before
    public void setUp() {
        registry = new InMemoryServiceRegistry();
    }
    
    @Test
    public void testRegister() {
        ServiceInstance instance = new ServiceInstance("test-service", "192.168.1.1", 8080);
        
        registry.register(instance);
        
        assertEquals(1, registry.getServiceCount());
        assertEquals(1, registry.getInstanceCount("test-service"));
    }
    
    @Test
    public void testUnregister() {
        ServiceInstance instance = new ServiceInstance("test-service", "192.168.1.1", 8080);
        registry.register(instance);
        
        registry.unregister(instance);
        
        assertEquals(0, registry.getInstanceCount("test-service"));
    }
    
    @Test
    public void testGetInstances() {
        registry.register(new ServiceInstance("test-service", "192.168.1.1", 8080));
        registry.register(new ServiceInstance("test-service", "192.168.1.2", 8080));
        
        List<ServiceInstance> instances = registry.getInstances("test-service");
        
        assertEquals(2, instances.size());
    }
    
    @Test
    public void testGetInstancesReturnsOnlyAvailable() {
        ServiceInstance instance1 = new ServiceInstance("test-service", "192.168.1.1", 8080);
        ServiceInstance instance2 = new ServiceInstance("test-service", "192.168.1.2", 8080);
        instance2.setAvailable(false);
        
        registry.register(instance1);
        registry.register(instance2);
        
        List<ServiceInstance> instances = registry.getInstances("test-service");
        
        assertEquals(1, instances.size());
        assertEquals(instance1, instances.get(0));
    }
    
    @Test
    public void testGetInstancesNonExistent() {
        List<ServiceInstance> instances = registry.getInstances("non-existent");
        assertTrue(instances.isEmpty());
    }
    
    @Test
    public void testGetAllInstances() {
        registry.register(new ServiceInstance("service1", "192.168.1.1", 8080));
        registry.register(new ServiceInstance("service2", "192.168.1.2", 8080));
        
        List<ServiceInstance> all = registry.getAllInstances();
        
        assertEquals(2, all.size());
    }
    
    @Test
    public void testSubscribe() {
        ServiceInstance instance = new ServiceInstance("test-service", "192.168.1.1", 8080);
        
        final boolean[] called = {false};
        ServiceChangeListener listener = (serviceName, instances) -> called[0] = true;
        
        registry.subscribe("test-service", listener);
        registry.register(instance);
        
        assertTrue(called[0]);
    }
    
    @Test
    public void testUnsubscribe() {
        ServiceInstance instance = new ServiceInstance("test-service", "192.168.1.1", 8080);
        
        final int[] callCount = {0};
        ServiceChangeListener listener = (serviceName, instances) -> callCount[0]++;
        
        registry.subscribe("test-service", listener);
        registry.register(instance);
        registry.unsubscribe("test-service", listener);
        registry.unregister(instance);
        
        assertEquals(1, callCount[0]);
    }
}