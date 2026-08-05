package cn.itcraft.jwsch.srv.loadbalance;

import cn.itcraft.jwsch.srv.registry.ServiceInstance;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round-robin load balancer implementation.
 *
 * <p>Distributes requests sequentially across service instances,
 * rotating through the list in order.
 *
 * <p>Features:
 * <ul>
 *   <li>Per-service counters for independent round-robin</li>
 *   <li>Thread-safe counter management</li>
 *   <li>Simple and predictable distribution</li>
 * </ul>
 */
public class RoundRobinLoadBalance implements LoadBalance {
    
    private final ConcurrentMap<String, AtomicInteger> counterMap = new ConcurrentHashMap<>();
    
    @Override
    public ServiceInstance select(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        
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
        return "roundRobin";
    }
    
    /**
     * Resets the round-robin counter for a specific service.
     *
     * @param serviceName the service name
     */
    public void reset(String serviceName) {
        counterMap.remove(serviceName);
    }
    
    /**
     * Resets all round-robin counters.
     */
    public void resetAll() {
        counterMap.clear();
    }
}