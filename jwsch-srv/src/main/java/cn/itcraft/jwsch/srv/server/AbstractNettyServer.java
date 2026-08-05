package cn.itcraft.jwsch.srv.server;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Netty 服务器抽象基类。
 * 
 * <p>封装 Netty 服务器的通用逻辑，包括：
 * <ul>
 *   <li>EventLoopGroup 管理（bossGroup 和 workerGroup）</li>
 *   <li>服务器 Channel 的生命周期管理</li>
 *   <li>启动/停止状态跟踪</li>
 *   <li>端口和活跃状态查询</li>
 * </ul>
 * 
 * <p>子类需要实现 {@link #start()} 方法来完成具体的服务器启动逻辑。
 */
public abstract class AbstractNettyServer {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected final EventLoopGroup bossGroup;
    protected final EventLoopGroup workerGroup;
    protected final String name;
    protected Channel serverChannel;
    protected volatile boolean started;
    
    /**
     * 创建抽象 Netty 服务器。
     *
     * @param name 服务器名称（用于日志）
     * @param bossGroup boss EventLoopGroup
     * @param workerGroup worker EventLoopGroup
     * @throws NullPointerException 如果 name、bossGroup 或 workerGroup 为 null
     */
    protected AbstractNettyServer(String name, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this.name = name;
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.started = false;
    }
    
    /**
     * 启动服务器。
     * 
     * <p>子类必须实现此方法，完成服务器绑定和 Channel 初始化。
     * 实现应设置 {@link #serverChannel} 和 {@link #started} 状态。
     */
    public abstract void start();
    
    /**
     * 关闭服务器。
     * 
     * <p>关闭服务器 Channel 并停止 EventLoopGroup（如果由本类管理）。
     * 子类可覆盖此方法以添加额外的清理逻辑。
     */
    public void shutdown() {
        if (!started) {
            return;
        }
        
        if (serverChannel != null) {
            serverChannel.close();
            serverChannel = null;
        }
        
        started = false;
        logger.info("{} stopped", name);
    }
    
    /**
     * 检查服务器是否已启动。
     *
     * @return true 如果服务器已启动，否则 false
     */
    public boolean isStarted() {
        return started;
    }
    
    /**
     * 获取服务器名称。
     *
     * @return 服务器名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取服务器监听的端口。
     *
     * @return 端口号，如果服务器未启动则返回 -1
     */
    public int getPort() {
        if (serverChannel != null) {
            return ((InetSocketAddress) serverChannel.localAddress()).getPort();
        }
        return -1;
    }
    
    /**
     * 检查服务器 Channel 是否活跃。
     *
     * @return true 如果服务器 Channel 活跃，否则 false
     */
    public boolean isActive() {
        return serverChannel != null && serverChannel.isActive();
    }
}