package cn.itcraft.jwsch.srv.server.tcp;

import cn.itcraft.jwsch.common.flowcontrol.FlowControlConfig;
import cn.itcraft.jwsch.common.protocol.PacketDecoder;
import cn.itcraft.jwsch.common.protocol.PacketEncoder;
import cn.itcraft.jwsch.srv.flowcontrol.InboundRateLimiterHandler;
import cn.itcraft.jwsch.srv.metrics.ServerMetrics;
import cn.itcraft.jwsch.srv.router.PacketRouter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class TcpServerInitializer extends ChannelInitializer<SocketChannel> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TcpServerInitializer.class);
    private static final int READER_IDLE_TIME_SECONDS = 180;
    private static final int WRITER_IDLE_TIME_SECONDS = 0;
    private static final int ALL_IDLE_TIME_SECONDS = 0;
    
    private final PacketRouter packetRouter;
    private final ServerMetrics serverMetrics;
    private final FlowControlConfig flowControlConfig;
    
    public TcpServerInitializer(PacketRouter packetRouter) {
        this(packetRouter, null, FlowControlConfig.defaultConfig());
    }
    
    public TcpServerInitializer(PacketRouter packetRouter, ServerMetrics serverMetrics) {
        this(packetRouter, serverMetrics, FlowControlConfig.defaultConfig());
    }
    
    public TcpServerInitializer(PacketRouter packetRouter, ServerMetrics serverMetrics, 
            FlowControlConfig flowControlConfig) {
        this.packetRouter = packetRouter;
        this.serverMetrics = serverMetrics;
        this.flowControlConfig = flowControlConfig != null ? flowControlConfig : FlowControlConfig.defaultConfig();
    }
    
    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        
        pipeline.addLast("idleState", new IdleStateHandler(
            READER_IDLE_TIME_SECONDS, 
            WRITER_IDLE_TIME_SECONDS, 
            ALL_IDLE_TIME_SECONDS, 
            TimeUnit.SECONDS));
        pipeline.addLast("decoder", new PacketDecoder());
        pipeline.addLast("encoder", new PacketEncoder());
        
        if (flowControlConfig.isInboundEnabled()) {
            LOGGER.info("L1 rate limiter enabled: maxTokens={}, burstSize={}", 
                flowControlConfig.getMaxTokensPerSecond(), flowControlConfig.getBurstSize());
            pipeline.addLast("rateLimiter", new InboundRateLimiterHandler(flowControlConfig));
        } else {
            LOGGER.info("L1 rate limiter disabled");
        }
        
        pipeline.addLast("handler", new TcpServerHandler(packetRouter, serverMetrics));
    }
}