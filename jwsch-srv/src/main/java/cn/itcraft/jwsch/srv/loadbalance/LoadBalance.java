package cn.itcraft.jwsch.srv.loadbalance;

import cn.itcraft.jwsch.srv.registry.ServiceInstance;

import java.util.List;

/**
 * Load balancing strategy interface.
 *
 * <p>Defines the contract for load balancing algorithms that select
 * service instances from a list of available instances.
 *
 * <p>Implementations may use different strategies such as round-robin,
 * random selection, consistent hashing, or weighted algorithms.
 */
public interface LoadBalance {
    
    /**
     * Selects a service instance from the available instances.
     *
     * @param instances the list of available service instances
     * @return the selected service instance, or null if no instances available
     */
    ServiceInstance select(List<ServiceInstance> instances);
    
    /**
     * Returns the name of this load balancing strategy.
     *
     * @return the strategy name (e.g., "roundRobin", "random", "consistentHash")
     */
    String getName();
}