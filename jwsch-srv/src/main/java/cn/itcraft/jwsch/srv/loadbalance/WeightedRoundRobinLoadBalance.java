package cn.itcraft.jwsch.srv.loadbalance;

import cn.itcraft.jwsch.srv.registry.ServiceInstance;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WeightedRoundRobinLoadBalance implements LoadBalance {
    
    private final ConcurrentMap<String, AtomicInteger> counterMap = new ConcurrentHashMap<>();
    
    @Override
    public ServiceInstance select(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        
        int totalWeight = 0;
        for (ServiceInstance instance : instances) {
            totalWeight += instance.getWeight();
        }
        
        if (totalWeight <= 0) {
            return selectRoundRobin(instances);
        }
        
        String serviceName = instances.get(0).getServiceName();
        AtomicInteger counter = counterMap.computeIfAbsent(serviceName, k -> new AtomicInteger(0));
        
        int currentWeight = Math.abs(counter.getAndIncrement() % totalWeight);
        
        for (ServiceInstance instance : instances) {
            currentWeight -= instance.getWeight();
            if (currentWeight < 0) {
                return instance;
            }
        }
        
        return instances.get(0);
    }
    
    private ServiceInstance selectRoundRobin(List<ServiceInstance> instances) {
        int size = instances.size();
        if (size == 1) {
            return instances.get(0);
        }
        
        String serviceName = instances.get(0).getServiceName();
        AtomicInteger counter = counterMap.computeIfAbsent(serviceName, k -> new AtomicInteger(0));
        
        int index = Math.abs(counter.getAndIncrement() % size);
        return instances.get(index);
    }
    
    @Override
    public String getName() {
        return "weightedRoundRobin";
    }
    
    public void reset(String serviceName) {
        counterMap.remove(serviceName);
    }
    
    public void resetAll() {
        counterMap.clear();
    }
}