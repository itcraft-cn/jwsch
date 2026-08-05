package cn.itcraft.jwsch.srv.registry;

/**
 * RegistryFactory 是 ServiceRegistry 的工厂类，支持根据类型创建不同的注册中心实现。
 * 
 * <p>目前支持 MEMORY 类型，NACOS 和 ZOOKEEPER 暂未实现，回退到内存实现。
 * 
 * @author itcraft
 * @since 1.0
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public final class RegistryFactory {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryFactory.class);
    
    private RegistryFactory() {
    }
    
    /**
     * 根据注册中心类型创建 ServiceRegistry 实例（使用默认配置）。
     *
     * @param type 注册中心类型，如果为 null 则使用 MEMORY
     * @return ServiceRegistry 实例
     */
    public static ServiceRegistry createRegistry(RegistryType type) {
        return createRegistry(type, new Properties());
    }
    
    /**
     * 根据注册中心类型和配置创建 ServiceRegistry 实例。
     *
     * @param type 注册中心类型，如果为 null 则使用 MEMORY
     * @param config 配置属性
     * @return ServiceRegistry 实例
     */
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
    
    /**
     * 创建默认的 ServiceRegistry 实例（内存实现）。
     *
     * @return 默认的 ServiceRegistry 实例
     */
    public static ServiceRegistry createDefault() {
        return createRegistry(RegistryType.MEMORY);
    }
}