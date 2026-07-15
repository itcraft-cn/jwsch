package cn.itcraft.jwsch.srv.loadbalance;

import cn.itcraft.jwsch.srv.registry.ServiceInstance;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class RoundRobinLoadBalanceTest {
    
    private RoundRobinLoadBalance loadBalance;
    
    @Before
    public void setUp() {
        loadBalance = new RoundRobinLoadBalance();
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
    public void testSelectRoundRobin() {
        List<ServiceInstance> instances = new ArrayList<>();
        instances.add(new ServiceInstance("test", "host1", 8080));
        instances.add(new ServiceInstance("test", "host2", 8080));
        instances.add(new ServiceInstance("test", "host3", 8080));
        
        ServiceInstance s1 = loadBalance.select(instances);
        ServiceInstance s2 = loadBalance.select(instances);
        ServiceInstance s3 = loadBalance.select(instances);
        ServiceInstance s4 = loadBalance.select(instances);
        
        assertNotSame(s1, s2);
        assertNotSame(s2, s3);
        assertEquals(s1, s4);
    }
    
    @Test
    public void testGetName() {
        assertEquals("roundRobin", loadBalance.getName());
    }
    
    @Test
    public void testReset() {
        List<ServiceInstance> instances = new ArrayList<>();
        instances.add(new ServiceInstance("test", "host1", 8080));
        instances.add(new ServiceInstance("test", "host2", 8080));
        
        loadBalance.select(instances);
        loadBalance.reset("test");
        loadBalance.select(instances);
    }
    
    @Test
    public void testResetAll() {
        List<ServiceInstance> instances = new ArrayList<>();
        instances.add(new ServiceInstance("test", "host1", 8080));
        
        loadBalance.select(instances);
        loadBalance.resetAll();
        loadBalance.select(instances);
    }
}