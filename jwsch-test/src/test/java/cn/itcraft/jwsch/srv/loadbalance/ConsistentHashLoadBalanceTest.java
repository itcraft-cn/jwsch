package cn.itcraft.jwsch.srv.loadbalance;

import cn.itcraft.jwsch.srv.registry.ServiceInstance;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ConsistentHashLoadBalanceTest {
    
    private ConsistentHashLoadBalance loadBalance;
    private ServiceInstance instance1;
    private ServiceInstance instance2;
    private ServiceInstance instance3;
    private List<ServiceInstance> instances;
    
    @Before
    public void setUp() {
        loadBalance = new ConsistentHashLoadBalance();
        instance1 = new ServiceInstance("test-service", "192.168.1.1", 8080);
        instance2 = new ServiceInstance("test-service", "192.168.1.2", 8080);
        instance3 = new ServiceInstance("test-service", "192.168.1.3", 8080);
        instances = Arrays.asList(instance1, instance2, instance3);
    }
    
    @Test
    public void testSelect_nullInstances_returnsNull() {
        assertNull(loadBalance.select(null));
    }
    
    @Test
    public void testSelect_emptyInstances_returnsNull() {
        assertNull(loadBalance.select(Collections.emptyList()));
    }
    
    @Test
    public void testSelect_singleInstance_returnsInstance() {
        List<ServiceInstance> single = Collections.singletonList(instance1);
        assertSame(instance1, loadBalance.select(single));
    }
    
    @Test
    public void testSelect_withKey_sameKeyReturnsSameInstance() {
        String key = "user-123";
        
        ServiceInstance first = loadBalance.select(instances, key);
        ServiceInstance second = loadBalance.select(instances, key);
        ServiceInstance third = loadBalance.select(instances, key);
        
        assertSame(first, second);
        assertSame(first, third);
    }
    
    @Test
    public void testSelect_withKey_differentKeysMayReturnDifferentInstances() {
        Map<ServiceInstance, Integer> distribution = new HashMap<>();
        
        for (int i = 0; i < 100; i++) {
            String key = "user-" + i;
            ServiceInstance selected = loadBalance.select(instances, key);
            distribution.merge(selected, 1, Integer::sum);
        }
        
        assertTrue(distribution.size() >= 2);
    }
    
    @Test
    public void testSelect_withoutKey_returnsInstance() {
        ServiceInstance selected = loadBalance.select(instances);
        assertNotNull(selected);
        assertTrue(instances.contains(selected));
    }
    
    @Test
    public void testSelect_distribution() {
        Map<ServiceInstance, Integer> distribution = new HashMap<>();
        
        for (int i = 0; i < 1000; i++) {
            String key = "session-" + i;
            ServiceInstance selected = loadBalance.select(instances, key);
            distribution.merge(selected, 1, Integer::sum);
        }
        
        for (ServiceInstance instance : instances) {
            int count = distribution.getOrDefault(instance, 0);
            assertTrue("Distribution should be roughly even", count > 200);
        }
    }
    
    @Test
    public void testSelect_instanceChange_rebuildsRing() {
        String key = "user-456";
        
        ServiceInstance first = loadBalance.select(instances, key);
        
        List<ServiceInstance> newInstances = Arrays.asList(instance1, instance3);
        ServiceInstance second = loadBalance.select(newInstances, key);
        
        assertTrue(newInstances.contains(second));
    }
    
    @Test
    public void testGetName() {
        assertEquals("consistentHash", loadBalance.getName());
    }
    
    @Test
    public void testReset() {
        String key = "user-789";
        ServiceInstance before = loadBalance.select(instances, key);
        
        loadBalance.reset("test-service");
        
        ServiceInstance after = loadBalance.select(instances, key);
        assertSame(before, after);
    }
    
    @Test
    public void testResetAll() {
        loadBalance.select(instances, "key1");
        
        loadBalance.resetAll();
        
        ServiceInstance selected = loadBalance.select(instances, "key1");
        assertNotNull(selected);
    }
    
    @Test
    public void testCustomVirtualNodes() {
        ConsistentHashLoadBalance customLoadBalance = new ConsistentHashLoadBalance(80);
        
        ServiceInstance selected = customLoadBalance.select(instances, "test-key");
        assertNotNull(selected);
        assertTrue(instances.contains(selected));
    }
    
    @Test
    public void testSelect_weightedInstances_usesAddressNotWeight() {
        ServiceInstance weighted1 = new ServiceInstance("weighted-service", "192.168.1.1", 8080, 5);
        ServiceInstance weighted2 = new ServiceInstance("weighted-service", "192.168.1.2", 8080, 1);
        List<ServiceInstance> weightedInstances = Arrays.asList(weighted1, weighted2);
        
        Map<ServiceInstance, Integer> distribution = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            ServiceInstance selected = loadBalance.select(weightedInstances, "key-" + i);
            distribution.merge(selected, 1, Integer::sum);
        }
        
        assertEquals(2, distribution.size());
    }
}