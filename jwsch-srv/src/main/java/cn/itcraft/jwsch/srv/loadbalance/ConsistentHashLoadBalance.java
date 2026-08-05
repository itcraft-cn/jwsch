package cn.itcraft.jwsch.srv.loadbalance;

import cn.itcraft.jwsch.srv.registry.ServiceInstance;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Consistent hash load balancer implementation.
 *
 * <p>Distributes requests across service instances using consistent hashing,
 * which minimizes redistribution when instances are added or removed.
 *
 * <p>Features:
 * <ul>
 *   <li>Virtual nodes for better distribution (default: 160 per instance)</li>
 *   <li>MurmurHash128 for uniform distribution</li>
 *   <li>Key-based routing for session stickiness</li>
 *   <li>Automatic ring rebuild on instance list changes</li>
 * </ul>
 */
public class ConsistentHashLoadBalance implements LoadBalance {
    
    private static final int DEFAULT_VIRTUAL_NODES = 160;
    private static final HashFunction HASH_FUNCTION = Hashing.murmur3_128(0x1234ABCD);
    
    private final ConcurrentMap<String, ConsistentHashRing> ringMap = new ConcurrentHashMap<>();
    private final int virtualNodes;
    
    /**
     * Creates a ConsistentHashLoadBalance with default virtual nodes (160).
     */
    public ConsistentHashLoadBalance() {
        this(DEFAULT_VIRTUAL_NODES);
    }
    
    /**
     * Creates a ConsistentHashLoadBalance with specified virtual nodes.
     *
     * @param virtualNodes number of virtual nodes per instance
     */
    public ConsistentHashLoadBalance(int virtualNodes) {
        this.virtualNodes = virtualNodes > 0 ? virtualNodes : DEFAULT_VIRTUAL_NODES;
    }
    
    @Override
    public ServiceInstance select(List<ServiceInstance> instances) {
        return select(instances, null);
    }
    
    /**
     * Selects a service instance using consistent hashing.
     *
     * @param instances the list of available service instances
     * @param key the routing key for consistent hashing (null for random)
     * @return the selected service instance, or null if no instances
     */
    public ServiceInstance select(List<ServiceInstance> instances, String key) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        
        if (instances.size() == 1) {
            return instances.get(0);
        }
        
        String serviceName = instances.get(0).getServiceName();
        ConsistentHashRing ring = ringMap.computeIfAbsent(serviceName, 
            k -> new ConsistentHashRing(instances, virtualNodes));
        
        if (!ring.matches(instances)) {
            ring = new ConsistentHashRing(instances, virtualNodes);
            ringMap.put(serviceName, ring);
        }
        
        if (key != null && !key.isEmpty()) {
            return ring.select(key);
        }
        
        return ring.select(String.valueOf(System.nanoTime()));
    }
    
    @Override
    public String getName() {
        return "consistentHash";
    }
    
    /**
     * Resets the hash ring for a specific service.
     *
     * @param serviceName the service name
     */
    public void reset(String serviceName) {
        ringMap.remove(serviceName);
    }
    
    /**
     * Resets all hash rings.
     */
    public void resetAll() {
        ringMap.clear();
    }
    
    /**
     * Consistent hash ring for a specific service.
     */
    private static class ConsistentHashRing {
        private final NavigableMap<Long, ServiceInstance> ring = new TreeMap<>();
        private final int virtualNodes;
        private final int instanceCount;
        private final int instanceHashCode;
        
        /**
         * Creates a hash ring for the specified instances.
         *
         * @param instances the service instances
         * @param virtualNodes number of virtual nodes per instance
         */
        ConsistentHashRing(List<ServiceInstance> instances, int virtualNodes) {
            this.virtualNodes = virtualNodes;
            this.instanceCount = instances.size();
            this.instanceHashCode = instances.hashCode();
            
            for (ServiceInstance instance : instances) {
                addInstance(instance);
            }
        }
        
        private void addInstance(ServiceInstance instance) {
            String address = instance.getAddress();
            for (int i = 0; i < virtualNodes; i++) {
                String virtualNodeName = address + "#" + i;
                long hash = hash(virtualNodeName);
                ring.put(hash, instance);
            }
        }
        
        /**
         * Selects an instance for the given key.
         *
         * @param key the routing key
         * @return the selected instance, or null if ring is empty
         */
        ServiceInstance select(String key) {
            if (ring.isEmpty()) {
                return null;
            }
            
            long hash = hash(key);
            Map.Entry<Long, ServiceInstance> entry = ring.ceilingEntry(hash);
            
            if (entry == null) {
                entry = ring.firstEntry();
            }
            
            return entry.getValue();
        }
        
        /**
         * Checks if this ring matches the given instances.
         *
         * @param instances the instances to compare
         * @return true if the instances match this ring's instances
         */
        boolean matches(List<ServiceInstance> instances) {
            if (instances.size() != instanceCount) {
                return false;
            }
            return instances.hashCode() == instanceHashCode;
        }
        
        private static long hash(String key) {
            return HASH_FUNCTION.hashString(key, StandardCharsets.UTF_8).asLong();
        }
    }
}