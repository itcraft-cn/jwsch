package cn.itcraft.jwsch.srv.loadbalance;

import cn.itcraft.jwsch.srv.registry.ServiceInstance;

import java.util.List;
import java.util.Random;

public class RandomLoadBalance implements LoadBalance {
    
    private final Random random = new Random();
    
    @Override
    public ServiceInstance select(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        
        int size = instances.size();
        if (size == 1) {
            return instances.get(0);
        }
        
        int index = random.nextInt(size);
        return instances.get(index);
    }
    
    @Override
    public String getName() {
        return "random";
    }
}