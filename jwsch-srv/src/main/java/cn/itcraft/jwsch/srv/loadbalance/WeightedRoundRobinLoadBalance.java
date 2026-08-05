package cn.itcraft.jwsch.srv.loadbalance;

import cn.itcraft.jwsch.srv.registry.ServiceInstance;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Weighted round-robin load balancer implementation.
 *
 * <p>Distributes requests based on instance weights, where higher-weighted
 * instances receive more requests proportionally.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Calculate total weight of all instances</li>
 *   <li>Select instance based on weight distribution</li>
 *   <li>Fall back to plain round-robin if weights are invalid</li>
 * </ol>
 *
 * <p>Useful for heterogeneous environments where instances have different
 * capacities or performance characteristics.
 */
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
    
    /**
     * Resets the weighted round-robin counter for a specific service.
     *
     * @param serviceName the service name
     */
    public void reset(String serviceName) {
        counterMap.remove(serviceName);
    }
    
    /**
     * Resets all weighted round-robin counters.
     */
    public void resetAll() {
        counterMap.clear();
    }
}