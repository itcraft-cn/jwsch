package cn.itcraft.jwsch.srv.registry;

/**
 * ServiceRegistry 是服务注册中心接口，定义服务注册、发现和订阅的基本操作。
 * 
 * <p>支持服务实例的注册、注销、查询，以及服务变更的监听。
 * 
 * @author itcraft
 * @since 1.0
 */

import java.util.List;

public interface ServiceRegistry {
    
    /**
     * 注册一个服务实例。
     *
     * @param instance 服务实例，不可为 null
     */
    void register(ServiceInstance instance);
    
    /**
     * 注销一个服务实例。
     *
     * @param instance 服务实例，不可为 null
     */
    void unregister(ServiceInstance instance);
    
    /**
     * 获取指定服务名的可用实例列表。
     *
     * @param serviceName 服务名
     * @return 可用实例列表，不会为 null，可能为空列表
     */
    List<ServiceInstance> getInstances(String serviceName);
    
    /**
     * 获取所有服务实例（包括不可用的实例）。
     *
     * @return 所有实例列表，不会为 null
     */
    List<ServiceInstance> getAllInstances();
    
    /**
     * 订阅指定服务的变更通知。
     *
     * @param serviceName 服务名
     * @param listener 监听器，不可为 null
     */
    void subscribe(String serviceName, ServiceChangeListener listener);
    
    /**
     * 取消订阅指定服务的变更通知。
     *
     * @param serviceName 服务名
     * @param listener 监听器，不可为 null
     */
    void unsubscribe(String serviceName, ServiceChangeListener listener);
}