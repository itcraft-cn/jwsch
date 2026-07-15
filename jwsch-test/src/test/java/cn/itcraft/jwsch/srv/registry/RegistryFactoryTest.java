package cn.itcraft.jwsch.srv.registry;

import org.junit.Test;

import static org.junit.Assert.*;

public class RegistryFactoryTest {
    
    @Test
    public void testCreateMemoryRegistry() {
        ServiceRegistry registry = RegistryFactory.createRegistry(RegistryType.MEMORY);
        
        assertNotNull(registry);
        assertTrue(registry instanceof InMemoryServiceRegistry);
    }
    
    @Test
    public void testCreateNullRegistry() {
        ServiceRegistry registry = RegistryFactory.createRegistry(null);
        
        assertNotNull(registry);
        assertTrue(registry instanceof InMemoryServiceRegistry);
    }
    
    @Test
    public void testCreateNacosRegistry_fallsBackToMemory() {
        ServiceRegistry registry = RegistryFactory.createRegistry(RegistryType.NACOS);
        
        assertNotNull(registry);
        assertTrue(registry instanceof InMemoryServiceRegistry);
    }
    
    @Test
    public void testCreateZookeeperRegistry_fallsBackToMemory() {
        ServiceRegistry registry = RegistryFactory.createRegistry(RegistryType.ZOOKEEPER);
        
        assertNotNull(registry);
        assertTrue(registry instanceof InMemoryServiceRegistry);
    }
    
    @Test
    public void testCreateDefault() {
        ServiceRegistry registry = RegistryFactory.createDefault();
        
        assertNotNull(registry);
        assertTrue(registry instanceof InMemoryServiceRegistry);
    }
    
    @Test
    public void testCreateRegistryWithConfig() {
        ServiceRegistry registry = RegistryFactory.createRegistry(RegistryType.MEMORY, new java.util.Properties());
        
        assertNotNull(registry);
    }
}