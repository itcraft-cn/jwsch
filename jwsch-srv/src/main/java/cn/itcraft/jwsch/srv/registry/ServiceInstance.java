package cn.itcraft.jwsch.srv.registry;

/**
 * ServiceInstance 表示一个服务实例，包含服务名、主机、端口等元数据。
 * 
 * <p>服务实例可标记为可用或不可用，用于负载均衡和服务发现。
 * 重写了 equals/hashCode/toString 方法，基于服务名、主机和端口进行相等性判断。
 * 
 * @author itcraft
 * @since 1.0
 */

import java.util.Objects;

public class ServiceInstance {
    
    private final String serviceName;
    private final String host;
    private final int port;
    private final String address;
    private final int weight;
    private volatile boolean available = true;
    
    /**
     * 构造函数（默认权重为 1）。
     *
     * @param serviceName 服务名，不可为 null
     * @param host 主机地址，不可为 null
     * @param port 端口号
     */
    public ServiceInstance(String serviceName, String host, int port) {
        this(serviceName, host, port, 1);
    }
    
    /**
     * 构造函数（指定权重）。
     *
     * @param serviceName 服务名，不可为 null
     * @param host 主机地址，不可为 null
     * @param port 端口号
     * @param weight 权重，小于等于 0 时自动调整为 1
     */
    public ServiceInstance(String serviceName, String host, int port, int weight) {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        Objects.requireNonNull(host, "host cannot be null");
        
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
        this.address = host + ":" + port;
        this.weight = weight > 0 ? weight : 1;
    }
    
    /**
     * 获取服务名。
     *
     * @return 服务名
     */
    public String getServiceName() {
        return serviceName;
    }
    
    /**
     * 获取主机地址。
     *
     * @return 主机地址
     */
    public String getHost() {
        return host;
    }
    
    /**
     * 获取端口号。
     *
     * @return 端口号
     */
    public int getPort() {
        return port;
    }
    
    /**
     * 获取完整地址（host:port 格式）。
     *
     * @return 完整地址
     */
    public String getAddress() {
        return address;
    }
    
    /**
     * 获取权重。
     *
     * @return 权重
     */
    public int getWeight() {
        return weight;
    }
    
    /**
     * 检查服务实例是否可用。
     *
     * @return 是否可用
     */
    public boolean isAvailable() {
        return available;
    }
    
    /**
     * 设置服务实例的可用状态。
     *
     * @param available 是否可用
     */
    public void setAvailable(boolean available) {
        this.available = available;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceInstance that = (ServiceInstance) o;
        return port == that.port && serviceName.equals(that.serviceName) && host.equals(that.host);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(serviceName, host, port);
    }
    
    /**
     * {@inheritDoc}
     */
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