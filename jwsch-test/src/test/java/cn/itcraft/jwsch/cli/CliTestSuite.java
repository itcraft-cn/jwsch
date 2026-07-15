package cn.itcraft.jwsch.cli;

import cn.itcraft.jwsch.cli.client.TcpClientInitializerTest;
import cn.itcraft.jwsch.cli.client.TcpClientTest;
import cn.itcraft.jwsch.cli.client.TcpHandlerTest;
import cn.itcraft.jwsch.cli.config.ClientConfigTest;
import cn.itcraft.jwsch.cli.config.EventLoopConfigTest;
import cn.itcraft.jwsch.cli.config.TcpClientConfigTest;
import cn.itcraft.jwsch.cli.connection.ConnectionInfoTest;
import cn.itcraft.jwsch.cli.connection.ConnectionRegistryTest;
import cn.itcraft.jwsch.cli.connection.ConnectionStatusTest;
import cn.itcraft.jwsch.cli.connection.ConnectionTypeTest;
import cn.itcraft.jwsch.cli.pool.TcpConnectionPoolTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * jwsch-cli 模块测试套件。
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    TcpClientInitializerTest.class,
    TcpClientTest.class,
    TcpHandlerTest.class,
    ClientConfigTest.class,
    EventLoopConfigTest.class,
    TcpClientConfigTest.class,
    ConnectionInfoTest.class,
    ConnectionRegistryTest.class,
    ConnectionStatusTest.class,
    ConnectionTypeTest.class,
    TcpConnectionPoolTest.class
})
public class CliTestSuite {
}