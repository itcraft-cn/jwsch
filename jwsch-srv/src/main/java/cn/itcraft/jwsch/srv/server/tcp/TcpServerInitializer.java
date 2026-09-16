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

/**
 * TCP 服务器 Channel 初始化器。
 * 
 * <p>配置 TCP 服务器的 ChannelPipeline：
 * <ul>
 *   <li>IdleStateHandler：检测空闲连接（180 秒读空闲）</li>
 *   <li>PacketDecoder：解码二进制数据为 Packet</li>
 *   <li>PacketEncoder：编码 Packet 为二进制数据</li>
 *   <li>InboundRateLimiterHandler（可选）：L1 入站速率限制</li>
 *   <li>TcpServerHandler：业务逻辑处理器</li>
 * </ul>
 * 
 * <p>支持流量控制配置，可启用入站速率限制。
 */
public class TcpServerInitializer extends ChannelInitializer<SocketChannel> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TcpServerInitializer.class);
    private static final int READER_IDLE_TIME_SECONDS = 180;
    private static final int WRITER_IDLE_TIME_SECONDS = 0;
    private static final int ALL_IDLE_TIME_SECONDS = 0;
    
    private final PacketRouter packetRouter;
    private final ServerMetrics serverMetrics;
    private final FlowControlConfig flowControlConfig;
    /**
     * 数据包总长度软上限（默认 200KB，硬上限 500KB），超限入站/出站包被丢弃。
     */
    private final int maxPacketLength;
    /**
     * 过大的数据包内容是否进队列由独立线程打印（启动时固化）。
     */
    private final cn.itcraft.jwsch.common.protocol.OversizePacketLogger oversizeLogger;
    
    /**
     * 使用默认的 ServerMetrics（null）和 FlowControlConfig 创建初始化器。
     *
     * @param packetRouter 数据包路由器
     * @throws NullPointerException 如果 packetRouter 为 null
     */
    public TcpServerInitializer(PacketRouter packetRouter) {
        this(packetRouter, null, FlowControlConfig.defaultConfig(),
            cn.itcraft.jwsch.common.protocol.ProtocolConsts.DEFAULT_MAX_PACKET_LENGTH, false);
    }
    
    /**
     * 使用指定的 ServerMetrics 和默认的 FlowControlConfig 创建初始化器。
     *
     * @param packetRouter 数据包路由器
     * @param serverMetrics 服务器指标收集器（可为 null）
     * @throws NullPointerException 如果 packetRouter 为 null
     */
    public TcpServerInitializer(PacketRouter packetRouter, ServerMetrics serverMetrics) {
        this(packetRouter, serverMetrics, FlowControlConfig.defaultConfig(),
            cn.itcraft.jwsch.common.protocol.ProtocolConsts.DEFAULT_MAX_PACKET_LENGTH, false);
    }
    
    /**
     * 创建 TCP 服务器初始化器。
     *
     * @param packetRouter 数据包路由器
     * @param serverMetrics 服务器指标收集器（可为 null）
     * @param flowControlConfig 流量控制配置（可为 null，使用默认配置）
     * @throws NullPointerException 如果 packetRouter 为 null
     */
    public TcpServerInitializer(PacketRouter packetRouter, ServerMetrics serverMetrics, 
            FlowControlConfig flowControlConfig) {
        this(packetRouter, serverMetrics, flowControlConfig,
            cn.itcraft.jwsch.common.protocol.ProtocolConsts.DEFAULT_MAX_PACKET_LENGTH, false);
    }
    
    /**
     * 创建 TCP 服务器初始化器（指定包大小上限）。
     *
     * @param packetRouter 数据包路由器
     * @param serverMetrics 服务器指标收集器（可为 null）
     * @param flowControlConfig 流量控制配置（可为 null，使用默认配置）
     * @param maxPacketLength 数据包总长度软上限（硬上限 500KB，超限钳制）
     * @throws NullPointerException 如果 packetRouter 为 null
     */
    public TcpServerInitializer(PacketRouter packetRouter, ServerMetrics serverMetrics, 
            FlowControlConfig flowControlConfig, int maxPacketLength, boolean logOversizeContent) {
        this.packetRouter = packetRouter;
        this.serverMetrics = serverMetrics;
        this.flowControlConfig = flowControlConfig != null ? flowControlConfig : FlowControlConfig.defaultConfig();
        this.maxPacketLength = cn.itcraft.jwsch.common.config.TcpConfig
            .normalizePacketLimit(maxPacketLength);
        this.oversizeLogger = cn.itcraft.jwsch.common.protocol.OversizePacketLoggers.create(logOversizeContent);
    }
    
    /**
     * 初始化 Channel，配置 Pipeline。
     *
     * @param ch SocketChannel
     */
    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        
        pipeline.addLast("idleState", new IdleStateHandler(
            READER_IDLE_TIME_SECONDS, 
            WRITER_IDLE_TIME_SECONDS, 
            ALL_IDLE_TIME_SECONDS, 
            TimeUnit.SECONDS));
        pipeline.addLast("decoder", new PacketDecoder(maxPacketLength, oversizeLogger));
        pipeline.addLast("encoder", new PacketEncoder(maxPacketLength, oversizeLogger));
        
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