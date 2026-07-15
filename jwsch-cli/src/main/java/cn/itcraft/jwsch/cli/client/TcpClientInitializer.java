package cn.itcraft.jwsch.cli.client;

import cn.itcraft.jwsch.cli.config.TcpClientConfig;
import cn.itcraft.jwsch.common.protocol.PacketDecoder;
import cn.itcraft.jwsch.common.protocol.PacketEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * TCP 客户端 Channel 初始化器。
 * 
 * <p>Channel Pipeline 结构：
 * <pre>
 * [SSL] → [IdleState] → [Decoder] → [Encoder] → [Handler]
 * </pre>
 * 
 * <p>各 Handler 说明：
 * <ul>
 *   <li>SSL：可选，用于 TLS 加密</li>
 *   <li>IdleState：写空闲触发事件，空闲时间由配置决定</li>
 *   <li>Decoder：二进制协议解码</li>
 *   <li>Encoder：二进制协议编码</li>
 *   <li>Handler：业务消息处理、心跳发送</li>
 * </ul>
 */
public final class TcpClientInitializer extends ChannelInitializer<SocketChannel> {
    
    private final SslContext sslContext;
    private final TcpClientConfig config;
    
    public TcpClientInitializer() {
        this(null, new TcpClientConfig());
    }
    
    public TcpClientInitializer(SslContext sslContext) {
        this(sslContext, new TcpClientConfig());
    }
    
    public TcpClientInitializer(SslContext sslContext, TcpClientConfig config) {
        this.sslContext = sslContext;
        this.config = config;
    }
    
    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        
        if (sslContext != null) {
            pipeline.addLast("ssl", sslContext.newHandler(ch.alloc()));
        }
        
        pipeline.addLast("idleState", new IdleStateHandler(0, config.getIdleTime(), 0, TimeUnit.SECONDS))
            .addLast("decoder", new PacketDecoder())
            .addLast("encoder", new PacketEncoder())
            .addLast("handler", new TcpHandler(config));
    }
}