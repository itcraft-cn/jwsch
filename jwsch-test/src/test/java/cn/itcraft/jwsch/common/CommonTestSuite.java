package cn.itcraft.jwsch.common;

import cn.itcraft.jwsch.common.bytebuf.ByteBufAllocatorFactoryTest;
import cn.itcraft.jwsch.common.bytebuf.ByteBufConfigTest;
import cn.itcraft.jwsch.common.cache.ConcurrentHashMapCacheTest;
import cn.itcraft.jwsch.common.cache.GuavaCacheTest;
import cn.itcraft.jwsch.common.config.TcpConfigTest;
import cn.itcraft.jwsch.common.config.WriteBufferWaterMarkTest;
import cn.itcraft.jwsch.common.eventloop.SharedEventLoopManagerTest;
import cn.itcraft.jwsch.common.exception.ErrorCodeTest;
import cn.itcraft.jwsch.common.id.IdGeneratorTest;
import cn.itcraft.jwsch.common.metrics.DefaultMetricsTest;
import cn.itcraft.jwsch.common.protocol.CommandTest;
import cn.itcraft.jwsch.common.protocol.PacketDecoderTest;
import cn.itcraft.jwsch.common.protocol.PacketEncoderTest;
import cn.itcraft.jwsch.common.protocol.PacketHeaderTest;
import cn.itcraft.jwsch.common.protocol.ProtocolConstsTest;
import cn.itcraft.jwsch.common.ssl.SslConfigTest;
import cn.itcraft.jwsch.common.ssl.SslContextFactoryTest;
import cn.itcraft.jwsch.common.util.StringUtilsTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * jwsch-common 模块测试套件。
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    ByteBufAllocatorFactoryTest.class,
    ByteBufConfigTest.class,
    ConcurrentHashMapCacheTest.class,
    GuavaCacheTest.class,
    TcpConfigTest.class,
    WriteBufferWaterMarkTest.class,
    SharedEventLoopManagerTest.class,
    ErrorCodeTest.class,
    IdGeneratorTest.class,
    DefaultMetricsTest.class,
    CommandTest.class,
    PacketDecoderTest.class,
    PacketEncoderTest.class,
    PacketHeaderTest.class,
    ProtocolConstsTest.class,
    SslConfigTest.class,
    SslContextFactoryTest.class,
    StringUtilsTest.class
})
public class CommonTestSuite {
}