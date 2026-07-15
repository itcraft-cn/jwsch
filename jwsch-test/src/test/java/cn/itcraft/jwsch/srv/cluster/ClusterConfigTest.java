package cn.itcraft.jwsch.srv.cluster;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for ClusterConfig.
 *
 * <p>ClusterConfig was refactored in Phase 3:
 * <ul>
 *   <li>nodePrefix is now String (was int)</li>
 *   <li>nodeId is computed as {nodePrefix}-{advertiseHost}-{clusterPort}</li>
 *   <li>advertiseHost replaces host, resolved from JVM/env/auto-detect</li>
 *   <li>clusterPort returns bindPort if set, else basePort</li>
 * </ul>
 */
public class ClusterConfigTest {

    @Test
    public void testDefaultValues() {
        ClusterConfig config = new ClusterConfig();

        assertFalse(config.isEnabled());
        assertEquals("jwsch", config.getNodePrefix());
        assertEquals(9090, config.getBasePort());
        assertEquals(3, config.getPortRange());
        assertEquals(8080, config.getWebsocketPort());
        assertEquals(8081, config.getHttpPort());
        assertEquals(30, config.getSyncIntervalSeconds());
        assertEquals(30000, config.getConnectionTimeoutMs());
        assertEquals(10, config.getHeartbeatIntervalSeconds());
    }

    @Test
    public void testComputedNodeId() {
        ClusterConfig config = new ClusterConfig();
        config.setNodePrefix("test-node");
        config.setAdvertiseHost("192.168.1.1");
        config.setBindPort(9090);

        String nodeId = config.getNodeId();
        assertEquals("test-node-192.168.1.1-9090", nodeId);
    }

    @Test
    public void testClusterPortFromBindPort() {
        ClusterConfig config = new ClusterConfig();
        config.setBasePort(9090);
        config.setBindPort(9091);

        assertEquals(9091, config.getClusterPort());
    }

    @Test
    public void testClusterPortFromBasePort() {
        ClusterConfig config = new ClusterConfig();
        config.setBasePort(9090);

        assertEquals(9090, config.getClusterPort());
    }

    @Test
    public void testToNodeInfo() {
        ClusterConfig config = new ClusterConfig();
        config.setNodePrefix("node-a");
        config.setAdvertiseHost("127.0.0.1");
        config.setBindPort(9090);
        config.setWebsocketPort(8080);
        config.setHttpPort(8081);

        NodeInfo nodeInfo = config.toNodeInfo();

        assertEquals("node-a-127.0.0.1-9090", nodeInfo.getNodeId());
        assertEquals("127.0.0.1", nodeInfo.getHost());
        assertEquals(9090, nodeInfo.getClusterPort());
        assertEquals(8080, nodeInfo.getWebsocketPort());
        assertEquals(8081, nodeInfo.getHttpPort());
    }
}