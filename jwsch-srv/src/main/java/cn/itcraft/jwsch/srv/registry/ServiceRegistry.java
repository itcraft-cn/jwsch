package cn.itcraft.jwsch.srv.registry;

import java.util.List;

public interface ServiceRegistry {
    
    void register(ServiceInstance instance);
    
    void unregister(ServiceInstance instance);
    
    List<ServiceInstance> getInstances(String serviceName);
    
    List<ServiceInstance> getAllInstances();
    
    void subscribe(String serviceName, ServiceChangeListener listener);
    
    void unsubscribe(String serviceName, ServiceChangeListener listener);
}