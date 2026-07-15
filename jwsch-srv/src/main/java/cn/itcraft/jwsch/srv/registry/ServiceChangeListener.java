package cn.itcraft.jwsch.srv.registry;

import java.util.List;

public interface ServiceChangeListener {
    
    void onServiceChanged(String serviceName, List<ServiceInstance> instances);
}