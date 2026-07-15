package cn.itcraft.jwsch.srv.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public final class RegistryFactory {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryFactory.class);
    
    private RegistryFactory() {
    }
    
    public static ServiceRegistry createRegistry(RegistryType type) {
        return createRegistry(type, new Properties());
    }
    
    public static ServiceRegistry createRegistry(RegistryType type, Properties config) {
        if (type == null) {
            type = RegistryType.MEMORY;
        }
        
        switch (type) {
            case MEMORY:
                LOGGER.info("Creating InMemoryServiceRegistry");
                return new InMemoryServiceRegistry();
                
            case NACOS:
                LOGGER.info("NacosServiceRegistry not implemented, falling back to InMemoryServiceRegistry");
                return new InMemoryServiceRegistry();
                
            case ZOOKEEPER:
                LOGGER.info("ZooKeeperServiceRegistry not implemented, falling back to InMemoryServiceRegistry");
                return new InMemoryServiceRegistry();
                
            default:
                LOGGER.warn("Unknown registry type: {}, using InMemoryServiceRegistry", type);
                return new InMemoryServiceRegistry();
        }
    }
    
    public static ServiceRegistry createDefault() {
        return createRegistry(RegistryType.MEMORY);
    }
}