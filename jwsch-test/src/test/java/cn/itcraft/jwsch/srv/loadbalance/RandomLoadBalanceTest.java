package cn.itcraft.jwsch.srv.loadbalance;

import cn.itcraft.jwsch.srv.registry.ServiceInstance;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class RandomLoadBalanceTest {
    
    private RandomLoadBalance loadBalance;
    
    @Before
    public void setUp() {
        loadBalance = new RandomLoadBalance();
    }
    
    @Test
    public void testSelectFromEmptyList() {
        assertNull(loadBalance.select(new ArrayList<>()));
    }
    
    @Test
    public void testSelectFromNullList() {
        assertNull(loadBalance.select(null));
    }
    
    @Test
    public void testSelectSingleInstance() {
        List<ServiceInstance> instances = new ArrayList<>();
        instances.add(new ServiceInstance("test", "host", 8080));
        
        ServiceInstance selected = loadBalance.select(instances);
        
        assertNotNull(selected);
        assertEquals("test", selected.getServiceName());
    }
    
    @Test
    public void testSelectMultipleInstances() {
        List<ServiceInstance> instances = new ArrayList<>();
        instances.add(new ServiceInstance("test", "host1", 8080));
        instances.add(new ServiceInstance("test", "host2", 8080));
        instances.add(new ServiceInstance("test", "host3", 8080));
        
        ServiceInstance selected = loadBalance.select(instances);
        
        assertNotNull(selected);
        assertTrue(instances.contains(selected));
    }
    
    @Test
    public void testGetName() {
        assertEquals("random", loadBalance.getName());
    }
}