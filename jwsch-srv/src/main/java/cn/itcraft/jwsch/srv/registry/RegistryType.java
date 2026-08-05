package cn.itcraft.jwsch.srv.registry;

/**
 * RegistryType 枚举定义了支持的注册中心类型。
 * 
 * @author itcraft
 * @since 1.0
 */

public enum RegistryType {
    /**
     * 内存注册中心，基于内存存储，适用于单机或测试环境。
     */
    MEMORY,
    /**
     * Nacos 注册中心，基于阿里开源的 Nacos（暂未实现）。
     */
    NACOS,
    /**
     * ZooKeeper 注册中心，基于 Apache ZooKeeper（暂未实现）。
     */
    ZOOKEEPER
}