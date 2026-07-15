package cn.itcraft.jwsch.srv.cluster;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class InMemoryClusterNodeRegistryTest {
    
    private InMemoryClusterNodeRegistry registry;
    
    @Before
    public void setUp() {
        registry = new InMemoryClusterNodeRegistry("local-node");
    }
    
    @Test
    public void testRegister() {
        NodeInfo node = new NodeInfo("node-01", "192.168.1.1", 9090, 8080, 8081);
        registry.register(node);
        
        List<NodeInfo> nodes = registry.getNodes();
        assertEquals(1, nodes.size());
        assertEquals(node, nodes.get(0));
    }
    
    @Test
    public void testRegisterIgnoresLocalNode() {
        NodeInfo localNode = new NodeInfo("local-node", "192.168.1.0", 9090, 8080, 8081);
        registry.register(localNode);
        
        assertEquals(0, registry.getNodeCount());
    }
    
    @Test
    public void testRegisterIgnoresDuplicates() {
        NodeInfo node1 = new NodeInfo("node-01", "192.168.1.1", 9090, 8080, 8081);
        NodeInfo node2 = new NodeInfo("node-01", "192.168.1.1", 9091, 8081, 8082);
        
        registry.register(node1);
        registry.register(node2);
        
        assertEquals(1, registry.getNodeCount());
    }
    
    @Test
    public void testDeregister() {
        NodeInfo node = new NodeInfo("node-01", "192.168.1.1", 9090, 8080, 8081);
        registry.register(node);
        registry.deregister("node-01");
        
        assertEquals(0, registry.getNodeCount());
    }
    
    @Test
    public void testGetNode() {
        NodeInfo node = new NodeInfo("node-01", "192.168.1.1", 9090, 8080, 8081);
        registry.register(node);
        
        NodeInfo found = registry.getNode("node-01");
        assertNotNull(found);
        assertEquals("node-01", found.getNodeId());
        
        assertNull(registry.getNode("nonexistent"));
    }
    
    @Test
    public void testNodeChangeListener() {
        final NodeInfo[] joinedNode = {null};
        final String[] leftNodeId = {null};
        
        registry.subscribe(new NodeChangeListener() {
            @Override
            public void onNodeJoin(NodeInfo node) {
                joinedNode[0] = node;
            }
            
            @Override
            public void onNodeLeave(String nodeId) {
                leftNodeId[0] = nodeId;
            }
        });
        
        NodeInfo node = new NodeInfo("node-01", "192.168.1.1", 9090, 8080, 8081);
        registry.register(node);
        
        assertNotNull(joinedNode[0]);
        assertEquals("node-01", joinedNode[0].getNodeId());
        
        registry.deregister("node-01");
        
        assertEquals("node-01", leftNodeId[0]);
    }
    
    @Test
    public void testClear() {
        registry.register(new NodeInfo("node-01", "192.168.1.1", 9090, 8080, 8081));
        registry.register(new NodeInfo("node-02", "192.168.1.2", 9090, 8080, 8081));
        
        registry.clear();
        
        assertEquals(0, registry.getNodeCount());
    }
}