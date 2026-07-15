package cn.itcraft.jwsch.srv.registry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServiceInstanceTest {
    
    private ServiceInstance instance;
    
    @Before
    public void setUp() {
        instance = new ServiceInstance("test-service", "192.168.1.1", 8080);
    }
    
    @Test
    public void testConstructor() {
        assertEquals("test-service", instance.getServiceName());
        assertEquals("192.168.1.1", instance.getHost());
        assertEquals(8080, instance.getPort());
        assertEquals("192.168.1.1:8080", instance.getAddress());
        assertTrue(instance.isAvailable());
    }
    
    @Test(expected = NullPointerException.class)
    public void testNullServiceName() {
        new ServiceInstance(null, "host", 8080);
    }
    
    @Test(expected = NullPointerException.class)
    public void testNullHost() {
        new ServiceInstance("service", null, 8080);
    }
    
    @Test
    public void testSetAvailable() {
        instance.setAvailable(false);
        assertFalse(instance.isAvailable());
    }
    
    @Test
    public void testEquals() {
        ServiceInstance instance1 = new ServiceInstance("service", "host", 8080);
        ServiceInstance instance2 = new ServiceInstance("service", "host", 8080);
        ServiceInstance instance3 = new ServiceInstance("service", "host", 9090);
        
        assertEquals(instance1, instance2);
        assertNotEquals(instance1, instance3);
    }
    
    @Test
    public void testHashCode() {
        ServiceInstance instance1 = new ServiceInstance("service", "host", 8080);
        ServiceInstance instance2 = new ServiceInstance("service", "host", 8080);
        
        assertEquals(instance1.hashCode(), instance2.hashCode());
    }
    
    @Test
    public void testToString() {
        String str = instance.toString();
        assertTrue(str.contains("test-service"));
        assertTrue(str.contains("192.168.1.1"));
        assertTrue(str.contains("8080"));
    }
}