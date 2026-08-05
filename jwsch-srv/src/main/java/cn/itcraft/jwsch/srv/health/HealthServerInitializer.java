package cn.itcraft.jwsch.srv.health;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * Netty channel initializer for health check server.
 *
 * <p>Configures the HTTP pipeline for health check requests:
 * <ol>
 *   <li>HTTP server codec for request/response encoding/decoding</li>
 *   <li>HTTP object aggregator for full HTTP messages</li>
 *   <li>Health check handler for processing health endpoints</li>
 * </ol>
 */
class HealthServerInitializer extends ChannelInitializer<SocketChannel> {
    
    private final HealthCheckHandler healthCheckHandler;
    
    /**
     * Creates a HealthServerInitializer with the specified handler.
     *
     * @param healthCheckHandler the health check handler
     */
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