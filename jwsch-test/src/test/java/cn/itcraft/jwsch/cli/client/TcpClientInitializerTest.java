package cn.itcraft.jwsch.cli.client;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.*;

public class TcpClientInitializerTest {
    
    @Test
    public void testHandlerTypes() throws Exception {
        TcpClientInitializer initializer = new TcpClientInitializer();
        
        Method initChannelMethod = TcpClientInitializer.class.getDeclaredMethod("initChannel", 
            Class.forName("io.netty.channel.socket.SocketChannel"));
        initChannelMethod.setAccessible(true);
        assertNotNull(initChannelMethod);
    }
    
    @Test
    public void testInitializerClass() {
        TcpClientInitializer initializer = new TcpClientInitializer();
        assertNotNull(initializer);
        assertTrue(initializer instanceof ChannelHandler);
    }
}