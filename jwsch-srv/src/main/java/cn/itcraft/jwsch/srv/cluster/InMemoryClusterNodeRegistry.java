package cn.itcraft.jwsch.srv.cluster;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryClusterNodeRegistry {
    
    private final List<NodeInfo> nodes = new CopyOnWriteArrayList<>();
    private final List<NodeChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final String localNodeId;
    
    public InMemoryClusterNodeRegistry(String localNodeId) {
        this.localNodeId = localNodeId;
    }
    
    public void register(NodeInfo node) {
        if (node == null || node.getNodeId().equals(localNodeId)) {
            return;
        }
        
        if (!nodes.contains(node)) {
            nodes.add(node);
            notifyNodeJoin(node);
        }
    }
    
    public void deregister(String nodeId) {
        NodeInfo removed = null;
        for (NodeInfo node : nodes) {
            if (node.getNodeId().equals(nodeId)) {
                removed = node;
                break;
            }
        }
        
        if (removed != null) {
            nodes.remove(removed);
            notifyNodeLeave(nodeId);
        }
    }
    
    public List<NodeInfo> getNodes() {
        return new CopyOnWriteArrayList<>(nodes);
    }
    
    public NodeInfo getNode(String nodeId) {
        for (NodeInfo node : nodes) {
            if (node.getNodeId().equals(nodeId)) {
                return node;
            }
        }
        return null;
    }
    
    public int getNodeCount() {
        return nodes.size();
    }
    
    public void subscribe(NodeChangeListener listener) {
        listeners.add(listener);
    }
    
    public void unsubscribe(NodeChangeListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyNodeJoin(NodeInfo node) {
        for (NodeChangeListener listener : listeners) {
            try {
                listener.onNodeJoin(node);
            } catch (Exception e) {
            }
        }
    }
    
    private void notifyNodeLeave(String nodeId) {
        for (NodeChangeListener listener : listeners) {
            try {
                listener.onNodeLeave(nodeId);
            } catch (Exception e) {
            }
        }
    }
    
    public void clear() {
        nodes.clear();
    }
    
    public String getLocalNodeId() {
        return localNodeId;
    }
}