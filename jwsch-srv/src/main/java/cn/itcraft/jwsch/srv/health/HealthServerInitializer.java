package cn.itcraft.jwsch.srv.health;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

class HealthServerInitializer extends ChannelInitializer<SocketChannel> {
    
    private final HealthCheckHandler healthCheckHandler;
    
    HealthServerInitializer(HealthCheckHandler healthCheckHandler) {
        this.healthCheckHandler = healthCheckHandler;
    }
    
    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast("codec", new HttpServerCodec());
        p.addLast("aggregator", new HttpObjectAggregator(8192));
        p.addLast("handler", healthCheckHandler);
    }
}