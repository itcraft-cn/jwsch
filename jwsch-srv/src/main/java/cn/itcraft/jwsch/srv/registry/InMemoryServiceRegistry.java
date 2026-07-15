package cn.itcraft.jwsch.srv.registry;

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
    
    public InMemoryServiceRegistry() {
        this.serviceMap = new ConcurrentHashMap<>();
        this.listenerMap = new ConcurrentHashMap<>();
    }
    
    @Override
    public void register(ServiceInstance instance) {
        String serviceName = instance.getServiceName();
        
        serviceMap.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>())
            .add(instance);
        
        LOGGER.info("Service registered: {}", instance);
        notifyListeners(serviceName);
    }
    
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
    
    @Override
    public List<ServiceInstance> getAllInstances() {
        List<ServiceInstance> all = new ArrayList<>();
        for (CopyOnWriteArrayList<ServiceInstance> instances : serviceMap.values()) {
            all.addAll(instances);
        }
        return all;
    }
    
    @Override
    public void subscribe(String serviceName, ServiceChangeListener listener) {
        listenerMap.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>())
            .add(listener);
        LOGGER.debug("Listener subscribed for service: {}", serviceName);
    }
    
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
    
    public int getServiceCount() {
        return serviceMap.size();
    }
    
    public int getInstanceCount(String serviceName) {
        CopyOnWriteArrayList<ServiceInstance> instances = serviceMap.get(serviceName);
        return instances != null ? instances.size() : 0;
    }
}