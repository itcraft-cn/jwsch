package cn.itcraft.jwsch.srv.server;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public abstract class AbstractNettyServer {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected final EventLoopGroup bossGroup;
    protected final EventLoopGroup workerGroup;
    protected final String name;
    protected Channel serverChannel;
    protected volatile boolean started;
    
    protected AbstractNettyServer(String name, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this.name = name;
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.started = false;
    }
    
    public abstract void start();
    
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
    
    public boolean isStarted() {
        return started;
    }
    
    public String getName() {
        return name;
    }
    
    public int getPort() {
        if (serverChannel != null) {
            return ((InetSocketAddress) serverChannel.localAddress()).getPort();
        }
        return -1;
    }
    
    public boolean isActive() {
        return serverChannel != null && serverChannel.isActive();
    }
}