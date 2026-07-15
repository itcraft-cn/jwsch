package cn.itcraft.jwsch.cli.pool;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * TCP 连接池。
 * 
 * <p>管理多个服务的 TCP 连接，支持轮询负载均衡。
 * 每个 serviceName 对应一个连接数组，使用 LongAdder 实现高性能轮询计数。
 * 
 * <p>数据结构：
 * <pre>
 * channelArrays = {
 *   "user-service": [channel1, channel2, ...],
 *   "order-service": [channel3, ...],
 *   ...
 * }
 * </pre>
 * 
 * <p>线程安全：使用 ConcurrentHashMap 和 AtomicReference，避免 CopyOnWriteArrayList 的写时复制开销。
 */
public class TcpConnectionPool {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TcpConnectionPool.class);
    
    private static final Channel[] EMPTY_CHANNELS = new Channel[0];
    
    /** 服务名 → 连接数组（原子引用，避免复制） */
    private final ConcurrentMap<String, AtomicReference<Channel[]>> channelArrays;
    
    /** 服务名 → 轮询计数器 */
    private final ConcurrentMap<String, LongAdder> counters;
    
    /** 每个服务的最大连接数 */
    private final int maxConnectionsPerService;
    
    public TcpConnectionPool() {
        this(10);
    }
    
    public TcpConnectionPool(int maxConnectionsPerService) {
        this.channelArrays = new ConcurrentHashMap<>();
        this.counters = new ConcurrentHashMap<>();
        this.maxConnectionsPerService = maxConnectionsPerService;
    }
    
    /**
     * 添加连接到池中。
     * 
     * <p>使用 CAS 更新数组，避免写时复制。
     * 
     * @param serviceName 服务名
     * @param channel     连接通道
     */
    public void addChannel(String serviceName, Channel channel) {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        Objects.requireNonNull(channel, "channel cannot be null");
        
        AtomicReference<Channel[]> ref = channelArrays.computeIfAbsent(
            serviceName, k -> new AtomicReference<>(EMPTY_CHANNELS));
        counters.computeIfAbsent(serviceName, k -> new LongAdder());
        
        while (true) {
            Channel[] oldArray = ref.get();
            if (oldArray.length >= maxConnectionsPerService) {
                LOGGER.warn("Connection pool for service {} is full, size={}", 
                    serviceName, oldArray.length);
                return;
            }
            
            Channel[] newArray = Arrays.copyOf(oldArray, oldArray.length + 1);
            newArray[oldArray.length] = channel;
            
            if (ref.compareAndSet(oldArray, newArray)) {
                LOGGER.debug("Added channel to pool: service={}, size={}", 
                    serviceName, newArray.length);
                return;
            }
        }
    }
    
    /**
     * 获取服务的连接（轮询策略）。
     * 
     * <p>使用 LongAdder 实现高性能轮询计数。
     * 如果获取的连接不活跃，会移除无效连接并返回 null。
     * 
     * @param serviceName 服务名
     * @return 活跃的 Channel，如果没有可用连接则返回 null
     */
    public Channel getChannel(String serviceName) {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        
        AtomicReference<Channel[]> ref = channelArrays.get(serviceName);
        if (ref == null) {
            return null;
        }
        
        Channel[] channels = ref.get();
        if (channels == null || channels.length == 0) {
            return null;
        }
        
        LongAdder counter = counters.get(serviceName);
        if (counter == null) {
            return null;
        }
        
        counter.increment();
        int index = (int) (counter.sum() % channels.length);
        Channel channel = channels[index];
        
        if (channel != null && channel.isActive()) {
            LOGGER.debug("Got channel from pool: service={}, index={}", serviceName, index);
            return channel;
        }
        
        removeInactiveChannels(serviceName, ref);
        return null;
    }
    
    /**
     * 从池中移除连接。
     * 
     * <p>使用 CAS 更新数组。
     * 
     * @param serviceName 服务名
     * @param channel     连接通道
     */
    public void removeChannel(String serviceName, Channel channel) {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        Objects.requireNonNull(channel, "channel cannot be null");
        
        AtomicReference<Channel[]> ref = channelArrays.get(serviceName);
        if (ref == null) {
            return;
        }
        
        while (true) {
            Channel[] oldArray = ref.get();
            if (oldArray == null || oldArray.length == 0) {
                return;
            }
            
            List<Channel> newList = new ArrayList<>(oldArray.length - 1);
            boolean found = false;
            for (Channel c : oldArray) {
                if (c == channel) {
                    found = true;
                } else {
                    newList.add(c);
                }
            }
            
            if (!found) {
                return;
            }
            
            Channel[] newArray = newList.toArray(new Channel[0]);
            if (ref.compareAndSet(oldArray, newArray)) {
                LOGGER.debug("Removed channel from pool: service={}", serviceName);
                
                if (newArray.length == 0) {
                    channelArrays.remove(serviceName);
                    counters.remove(serviceName);
                }
                return;
            }
        }
    }
    
    /**
     * 获取服务的所有活跃连接。
     * 
     * @param serviceName 服务名
     * @return 活跃连接列表
     */
    public List<Channel> getActiveChannels(String serviceName) {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        
        AtomicReference<Channel[]> ref = channelArrays.get(serviceName);
        Channel[] channels = ref != null ? ref.get() : null;
        if (channels == null || channels.length == 0) {
            return new ArrayList<>();
        }
        
        List<Channel> activeChannels = new ArrayList<>(channels.length);
        for (Channel channel : channels) {
            if (channel.isActive()) {
                activeChannels.add(channel);
            }
        }
        
        return activeChannels;
    }
    
    /**
     * 获取服务的连接数量。
     * 
     * @param serviceName 服务名
     * @return 连接数量
     */
    public int getConnectionCount(String serviceName) {
        AtomicReference<Channel[]> ref = channelArrays.get(serviceName);
        Channel[] channels = ref != null ? ref.get() : null;
        return channels != null ? channels.length : 0;
    }
    
    /**
     * 获取服务的活跃连接数量。
     * 
     * @param serviceName 服务名
     * @return 活跃连接数量
     */
    public int getActiveConnectionCount(String serviceName) {
        AtomicReference<Channel[]> ref = channelArrays.get(serviceName);
        Channel[] channels = ref != null ? ref.get() : null;
        if (channels == null) {
            return 0;
        }
        
        int count = 0;
        for (Channel channel : channels) {
            if (channel.isActive()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 清空指定服务的连接池。
     * 
     * @param serviceName 服务名
     */
    public void clear(String serviceName) {
        AtomicReference<Channel[]> ref = channelArrays.remove(serviceName);
        counters.remove(serviceName);
        
        if (ref != null) {
            Channel[] channels = ref.get();
            if (channels != null) {
                for (Channel channel : channels) {
                    if (channel != null && channel.isActive()) {
                        channel.close();
                    }
                }
            }
            LOGGER.info("Cleared connection pool for service: {}", serviceName);
        }
    }
    
    /**
     * 清空所有服务的连接池。
     */
    public void clearAll() {
        for (String serviceName : channelArrays.keySet()) {
            clear(serviceName);
        }
    }
    
    /**
     * 移除指定服务的不活跃连接。
     * 
     * @param serviceName 服务名
     * @param ref         连接数组引用
     */
    private void removeInactiveChannels(String serviceName, AtomicReference<Channel[]> ref) {
        while (true) {
            Channel[] oldArray = ref.get();
            if (oldArray == null || oldArray.length == 0) {
                return;
            }
            
            List<Channel> activeList = new ArrayList<>(oldArray.length);
            for (Channel channel : oldArray) {
                if (channel != null && channel.isActive()) {
                    activeList.add(channel);
                }
            }
            
            Channel[] newArray = activeList.toArray(new Channel[0]);
            if (ref.compareAndSet(oldArray, newArray)) {
                if (newArray.length == 0) {
                    channelArrays.remove(serviceName);
                    counters.remove(serviceName);
                }
                LOGGER.debug("Removed inactive channels for service: {}", serviceName);
                return;
            }
        }
    }
}