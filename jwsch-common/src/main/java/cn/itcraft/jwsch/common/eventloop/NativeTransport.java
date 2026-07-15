package cn.itcraft.jwsch.common.eventloop;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 原生传输工具类。
 * 
 * <p>自动检测平台是否支持 Epoll，优先使用原生传输以提升性能。
 * 
 * <p>Linux 上 Epoll 相比 NIO 的优势：
 * <ul>
 *   <li>更少系统调用，减少上下文切换</li>
 *   <li>边缘触发模式，更高的并发效率</li>
 *   <li>零拷贝支持更完善</li>
 * </ul>
 * 
 * <p>实现优化：
 * <ul>
 *   <li>静态初始化时确定工厂实现，避免每次调用的分支判断</li>
 *   <li>函数式接口封装，零运行时开销</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * EventLoopGroup bossGroup = NativeTransport.createEventLoopGroup(1, "boss");
 * EventLoopGroup workerGroup = NativeTransport.createEventLoopGroup(4, "worker");
 * Class&lt;? extends ServerChannel&gt; channelClass = NativeTransport.getServerChannelClass();
 * 
 * ServerBootstrap bootstrap = new ServerBootstrap()
 *     .group(bossGroup, workerGroup)
 *     .channel(channelClass);
 * </pre>
 */
public final class NativeTransport {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NativeTransport.class);
    
    private static final boolean EPOLL_AVAILABLE;
    private static final EventLoopGroupFactory EVENT_LOOP_GROUP_FACTORY;
    private static final Class<? extends ServerChannel> SERVER_CHANNEL_CLASS;
    private static final Class<? extends SocketChannel> CLIENT_CHANNEL_CLASS;
    private static final String TRANSPORT_TYPE;
    
    static {
        boolean epollAvailable = false;
        try {
            epollAvailable = Epoll.isAvailable();
            if (epollAvailable) {
                LOGGER.info("Epoll native transport is available");
            } else {
                LOGGER.info("Epoll not available, using NIO. Reason: {}", 
                    Epoll.unavailabilityCause());
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to check Epoll availability, falling back to NIO", e);
        }
        EPOLL_AVAILABLE = epollAvailable;
        
        if (EPOLL_AVAILABLE) {
            EVENT_LOOP_GROUP_FACTORY = (threads, threadName) -> 
                new EpollEventLoopGroup(threads, new DefaultThreadFactory(threadName, true));
            SERVER_CHANNEL_CLASS = EpollServerSocketChannel.class;
            CLIENT_CHANNEL_CLASS = EpollSocketChannel.class;
            TRANSPORT_TYPE = "epoll";
        } else {
            EVENT_LOOP_GROUP_FACTORY = (threads, threadName) -> 
                new NioEventLoopGroup(threads, new DefaultThreadFactory(threadName, true));
            SERVER_CHANNEL_CLASS = NioServerSocketChannel.class;
            CLIENT_CHANNEL_CLASS = NioSocketChannel.class;
            TRANSPORT_TYPE = "nio";
        }
    }
    
    private NativeTransport() {
    }
    
    /**
     * 检查 Epoll 是否可用。
     * 
     * @return true 表示 Epoll 可用
     */
    public static boolean isEpollAvailable() {
        return EPOLL_AVAILABLE;
    }
    
    /**
     * 创建 EventLoopGroup。
     * 
     * <p>优先使用 Epoll，不可用时回退到 NIO。
     * 
     * @param threads 线程数
     * @param threadName 线程名前缀
     * @return EventLoopGroup 实例
     */
    public static EventLoopGroup createEventLoopGroup(int threads, String threadName) {
        return EVENT_LOOP_GROUP_FACTORY.create(threads, threadName);
    }
    
    /**
     * 获取服务端 Channel 类。
     * 
     * @return EpollServerSocketChannel 或 NioServerSocketChannel
     */
    public static Class<? extends ServerChannel> getServerChannelClass() {
        return SERVER_CHANNEL_CLASS;
    }
    
    /**
     * 获取客户端 Channel 类。
     * 
     * @return EpollSocketChannel 或 NioSocketChannel
     */
    public static Class<? extends SocketChannel> getClientChannelClass() {
        return CLIENT_CHANNEL_CLASS;
    }
    
    /**
     * 获取传输类型名称。
     * 
     * @return "epoll" 或 "nio"
     */
    public static String getTransportType() {
        return TRANSPORT_TYPE;
    }
    
    /**
     * EventLoopGroup 工厂函数式接口。
     */
    @FunctionalInterface
    private interface EventLoopGroupFactory {
        EventLoopGroup create(int threads, String threadName);
    }
}
