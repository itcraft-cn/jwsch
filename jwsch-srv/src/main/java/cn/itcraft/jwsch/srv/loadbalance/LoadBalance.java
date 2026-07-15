package cn.itcraft.jwsch.srv.loadbalance;

import cn.itcraft.jwsch.srv.registry.ServiceInstance;

import java.util.List;

public interface LoadBalance {
    
    ServiceInstance select(List<ServiceInstance> instances);
    
    String getName();
}