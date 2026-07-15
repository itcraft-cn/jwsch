package cn.itcraft.jwsch.srv.cluster;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class NodeInfoTest {
    
    private NodeInfo nodeInfo;
    
    @Before
    public void setUp() {
        nodeInfo = new NodeInfo("node-01", "192.168.1.1", 9090, 8080, 8081);
    }
    
    @Test
    public void testBasicProperties() {
        assertEquals("node-01", nodeInfo.getNodeId());
        assertEquals("192.168.1.1", nodeInfo.getHost());
        assertEquals(9090, nodeInfo.getClusterPort());
        assertEquals(8080, nodeInfo.getWebsocketPort());
        assertEquals(8081, nodeInfo.getHttpPort());
        assertEquals(NodeStatus.UP, nodeInfo.getStatus());
    }
    
    @Test
    public void testGetClusterAddress() {
        assertEquals("192.168.1.1:9090", nodeInfo.getClusterAddress());
    }
    
    @Test
    public void testGetWebsocketAddress() {
        assertEquals("192.168.1.1:8080", nodeInfo.getWebsocketAddress());
    }
    
    @Test
    public void testWithStatus() {
        NodeInfo downNode = nodeInfo.withStatus(NodeStatus.DOWN);
        assertEquals(NodeStatus.DOWN, downNode.getStatus());
        assertEquals(nodeInfo.getNodeId(), downNode.getNodeId());
    }
    
    @Test
    public void testEquals() {
        NodeInfo same = new NodeInfo("node-01", "192.168.1.2", 9091, 8081, 8082);
        NodeInfo different = new NodeInfo("node-02", "192.168.1.1", 9090, 8080, 8081);
        
        assertEquals(nodeInfo, same);
        assertNotEquals(nodeInfo, different);
    }
    
    @Test
    public void testHashCode() {
        NodeInfo same = new NodeInfo("node-01", "192.168.1.2", 9091, 8081, 8082);
        assertEquals(nodeInfo.hashCode(), same.hashCode());
    }
}