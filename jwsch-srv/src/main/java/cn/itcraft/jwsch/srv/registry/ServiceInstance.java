package cn.itcraft.jwsch.srv.registry;

import java.util.Objects;

public class ServiceInstance {
    
    private final String serviceName;
    private final String host;
    private final int port;
    private final String address;
    private final int weight;
    private volatile boolean available = true;
    
    public ServiceInstance(String serviceName, String host, int port) {
        this(serviceName, host, port, 1);
    }
    
    public ServiceInstance(String serviceName, String host, int port, int weight) {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        Objects.requireNonNull(host, "host cannot be null");
        
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
        this.address = host + ":" + port;
        this.weight = weight > 0 ? weight : 1;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getAddress() {
        return address;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    public void setAvailable(boolean available) {
        this.available = available;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceInstance that = (ServiceInstance) o;
        return port == that.port && serviceName.equals(that.serviceName) && host.equals(that.host);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(serviceName, host, port);
    }
    
    @Override
    public String toString() {
        return "ServiceInstance{" +
            "serviceName='" + serviceName + '\'' +
            ", host='" + host + '\'' +
            ", port=" + port +
            ", weight=" + weight +
            ", available=" + available +
            '}';
    }
}