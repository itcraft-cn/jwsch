package cn.itcraft.jwsch.srv;

import cn.itcraft.jwsch.srv.cluster.ClusterConfigTest;
import cn.itcraft.jwsch.srv.cluster.ClusterConnectionRegistryTest;
import cn.itcraft.jwsch.srv.cluster.ClusterServerTest;
import cn.itcraft.jwsch.srv.cluster.InMemoryClusterNodeRegistryTest;
import cn.itcraft.jwsch.srv.cluster.NodeInfoTest;
import cn.itcraft.jwsch.srv.config.ServerConfigTest;
import cn.itcraft.jwsch.srv.config.WebSocketServerConfigTest;
import cn.itcraft.jwsch.srv.health.HealthAggregatorTest;
import cn.itcraft.jwsch.srv.health.HealthCheckServerTest;
import cn.itcraft.jwsch.srv.health.HealthInfoTest;
import cn.itcraft.jwsch.srv.integration.BroadcastIntegrationTest;
import cn.itcraft.jwsch.srv.integration.IntegrationTest;
import cn.itcraft.jwsch.srv.loadbalance.ConsistentHashLoadBalanceTest;
import cn.itcraft.jwsch.srv.loadbalance.RandomLoadBalanceTest;
import cn.itcraft.jwsch.srv.loadbalance.RoundRobinLoadBalanceTest;
import cn.itcraft.jwsch.srv.metrics.MetricsServerTest;
import cn.itcraft.jwsch.srv.metrics.ServerMetricsTest;
import cn.itcraft.jwsch.srv.registry.InMemoryServiceRegistryTest;
import cn.itcraft.jwsch.srv.registry.RegistryFactoryTest;
import cn.itcraft.jwsch.srv.registry.ServiceInstanceTest;
import cn.itcraft.jwsch.srv.router.PacketRouterExtraTest;
import cn.itcraft.jwsch.srv.router.ResponseMappingTest;
import cn.itcraft.jwsch.srv.router.TopicSubscriptionTest;
import cn.itcraft.jwsch.srv.security.SecurityManagerTest;
import cn.itcraft.jwsch.srv.security.SimpleAuthorizerTest;
import cn.itcraft.jwsch.srv.security.SimpleTokenAuthenticatorTest;
import cn.itcraft.jwsch.srv.server.AbstractNettyServerTest;
import cn.itcraft.jwsch.srv.server.tcp.TcpServerHandlerExtraTest;
import cn.itcraft.jwsch.srv.server.websocket.WebSocketConnectionManagerTest;
import cn.itcraft.jwsch.srv.server.websocket.WebSocketHandlerExtraTest;
import cn.itcraft.jwsch.srv.server.websocket.WebSocketServerTest;
import cn.itcraft.jwsch.srv.stats.TopicMessageTrackerTest;
import cn.itcraft.jwsch.srv.stats.TopicStatsManagerTest;
import cn.itcraft.jwsch.srv.stats.TopicSubscriptionTrackerTest;
import cn.itcraft.jwsch.srv.stats.TopicTrafficTrackerTest;
import cn.itcraft.jwsch.srv.tracing.TracingContextTest;
import cn.itcraft.jwsch.srv.tracing.TracingManagerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * jwsch-srv 模块测试套件。
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    JwschServerTest.class,
    AbstractNettyServerTest.class,
    ClusterConfigTest.class,
    ClusterConnectionRegistryTest.class,
    ClusterServerTest.class,
    InMemoryClusterNodeRegistryTest.class,
    NodeInfoTest.class,
    ServerConfigTest.class,
    WebSocketServerConfigTest.class,
    HealthAggregatorTest.class,
    HealthCheckServerTest.class,
    HealthInfoTest.class,
    BroadcastIntegrationTest.class,
    IntegrationTest.class,
    ConsistentHashLoadBalanceTest.class,
    RandomLoadBalanceTest.class,
    RoundRobinLoadBalanceTest.class,
    MetricsServerTest.class,
    ServerMetricsTest.class,
    InMemoryServiceRegistryTest.class,
    RegistryFactoryTest.class,
    ServiceInstanceTest.class,
    PacketRouterExtraTest.class,
    ResponseMappingTest.class,
    TopicSubscriptionTest.class,
    SecurityManagerTest.class,
    SimpleAuthorizerTest.class,
    SimpleTokenAuthenticatorTest.class,
    TcpServerHandlerExtraTest.class,
    WebSocketConnectionManagerTest.class,
    WebSocketHandlerExtraTest.class,
    WebSocketServerTest.class,
    TopicMessageTrackerTest.class,
    TopicStatsManagerTest.class,
    TopicSubscriptionTrackerTest.class,
    TopicTrafficTrackerTest.class,
    TracingContextTest.class,
    TracingManagerTest.class
})
public class SrvTestSuite {
}