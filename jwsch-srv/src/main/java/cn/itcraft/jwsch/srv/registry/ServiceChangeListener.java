package cn.itcraft.jwsch.srv.registry;

/**
 * ServiceChangeListener 是服务变更监听器接口。
 * 
 * <p>当服务实例注册或注销时，注册中心会通知所有订阅了该服务的监听器。
 * 
 * @author itcraft
 * @since 1.0
 */

import java.util.List;

public interface ServiceChangeListener {
    
    /**
     * 当服务实例发生变化时被调用。
     *
     * @param serviceName 服务名
     * @param instances 当前可用的服务实例列表（不会为 null，但可能为空列表）
     */
    void onServiceChanged(String serviceName, List<ServiceInstance> instances);
}