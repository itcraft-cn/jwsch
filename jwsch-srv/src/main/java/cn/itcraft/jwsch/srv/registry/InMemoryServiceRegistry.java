package cn.itcraft.jwsch.srv.registry;

/**
 * InMemoryServiceRegistry 是 ServiceRegistry 接口的内存实现。
 * 
 * <p>使用 ConcurrentHashMap 存储服务实例，支持线程安全的注册、注销和查询。
 * 提供订阅/通知机制，当服务实例变化时通知监听器。
 * 
 * @author itcraft
 * @since 1.0
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryServiceRegistry implements ServiceRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryServiceRegistry.class);
    
    private final Map<String, CopyOnWriteArrayList<ServiceInstance>> serviceMap;
    private final Map<String, CopyOnWriteArrayList<ServiceChangeListener>> listenerMap;
    
    /**
     * 默认构造函数，初始化空的服务映射和监听器映射。
     */
    public InMemoryServiceRegistry() {
        this.serviceMap = new ConcurrentHashMap<>();
        this.listenerMap = new ConcurrentHashMap<>();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void register(ServiceInstance instance) {
        String serviceName = instance.getServiceName();
        
        serviceMap.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>())
            .add(instance);
        
        LOGGER.info("Service registered: {}", instance);
        notifyListeners(serviceName);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void unregister(ServiceInstance instance) {
        String serviceName = instance.getServiceName();
        
        CopyOnWriteArrayList<ServiceInstance> instances = serviceMap.get(serviceName);
        if (instances != null) {
            instances.remove(instance);
            
            if (instances.isEmpty()) {
                serviceMap.remove(serviceName);
            }
        }
        
        LOGGER.info("Service unregistered: {}", instance);
        notifyListeners(serviceName);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<ServiceInstance> getInstances(String serviceName) {
        CopyOnWriteArrayList<ServiceInstance> instances = serviceMap.get(serviceName);
        if (instances == null) {
            return Collections.emptyList();
        }
        
        List<ServiceInstance> available = new ArrayList<>();
        for (ServiceInstance instance : instances) {
            if (instance.isAvailable()) {
                available.add(instance);
            }
        }
        
        return available;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<ServiceInstance> getAllInstances() {
        List<ServiceInstance> all = new ArrayList<>();
        for (CopyOnWriteArrayList<ServiceInstance> instances : serviceMap.values()) {
            all.addAll(instances);
        }
        return all;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe(String serviceName, ServiceChangeListener listener) {
        listenerMap.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>())
            .add(listener);
        LOGGER.debug("Listener subscribed for service: {}", serviceName);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void unsubscribe(String serviceName, ServiceChangeListener listener) {
        CopyOnWriteArrayList<ServiceChangeListener> listeners = listenerMap.get(serviceName);
        if (listeners != null) {
            listeners.remove(listener);
        }
        LOGGER.debug("Listener unsubscribed for service: {}", serviceName);
    }
    
    private void notifyListeners(String serviceName) {
        CopyOnWriteArrayList<ServiceChangeListener> listeners = listenerMap.get(serviceName);
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        
        List<ServiceInstance> instances = getInstances(serviceName);
        for (ServiceChangeListener listener : listeners) {
            try {
                listener.onServiceChanged(serviceName, instances);
            } catch (Exception e) {
                LOGGER.error("Failed to notify listener for service: {}", serviceName, e);
            }
        }
    }
    
    /**
     * 获取当前注册的服务数量（不同服务名的数量）。
     *
     * @return 服务数量
     */
    public int getServiceCount() {
        return serviceMap.size();
    }
    
    /**
     * 获取指定服务名的实例数量。
     *
     * @param serviceName 服务名
     * @return 实例数量，如果服务不存在则返回 0
     */
    public int getInstanceCount(String serviceName) {
        CopyOnWriteArrayList<ServiceInstance> instances = serviceMap.get(serviceName);
        return instances != null ? instances.size() : 0;
    }
}