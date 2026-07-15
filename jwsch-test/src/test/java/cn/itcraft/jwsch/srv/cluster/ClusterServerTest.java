package cn.itcraft.jwsch.srv.cluster;

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ClusterServerTest {
    
    private static final AtomicInteger PORT_COUNTER = new AtomicInteger(40000);
    
    private ClusterServer server;
    
    @After
    public void tearDown() {
        if (server != null) {
            server.stop();
        }
    }
    
    @Test
    public void testStart_stop() throws InterruptedException {
        ClusterConfig config = new ClusterConfig();
        config.setBindPort(PORT_COUNTER.getAndIncrement());
        ClusterConnectionRegistry connectionRegistry = new ClusterConnectionRegistry("local-node");
        InMemoryClusterNodeRegistry nodeRegistry = new InMemoryClusterNodeRegistry("local-node");

        server = new ClusterServer(config, connectionRegistry, nodeRegistry);
        server.start();
        assertTrue(server.getPort() > 0);
        server.stop();
    }

    @Test
    public void testGetPort() throws InterruptedException {
        ClusterConfig config = new ClusterConfig();
        int port = PORT_COUNTER.getAndIncrement();
        config.setBindPort(port);
        ClusterConnectionRegistry connectionRegistry = new ClusterConnectionRegistry("local-node");
        InMemoryClusterNodeRegistry nodeRegistry = new InMemoryClusterNodeRegistry("local-node");

        server = new ClusterServer(config, connectionRegistry, nodeRegistry);
        server.start();
        assertTrue(server.getPort() > 0);
    }
}