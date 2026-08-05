package cn.itcraft.jwsch.srv.cluster;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory cluster node registry implementation.
 * 
 * <p>Maintains a thread-safe list of cluster nodes and notifies listeners of node changes.
 * Used by cluster mesh manager to track cluster membership.
 */
public class InMemoryClusterNodeRegistry {
    
    private final List<NodeInfo> nodes = new CopyOnWriteArrayList<>();
    private final List<NodeChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final String localNodeId;
    
    /**
     * Creates an in-memory cluster node registry.
     * 
     * @param localNodeId identifier of the local node (not registered)
     */
    public InMemoryClusterNodeRegistry(String localNodeId) {
        this.localNodeId = localNodeId;
    }
    
    /**
     * Registers a cluster node.
     * 
     * <p>If the node is null or is the local node, does nothing.
     * If the node is not already registered, adds it and notifies listeners.
     * 
     * @param node node information
     */
    public void register(NodeInfo node) {
        if (node == null || node.getNodeId().equals(localNodeId)) {
            return;
        }
        
        if (!nodes.contains(node)) {
            nodes.add(node);
            notifyNodeJoin(node);
        }
    }
    
    /**
     * Deregisters a cluster node.
     * 
     * @param nodeId identifier of the node to remove
     */
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
    
    /**
     * Returns a copy of all registered nodes.
     * 
     * @return list of node information
     */
    public List<NodeInfo> getNodes() {
        return new CopyOnWriteArrayList<>(nodes);
    }
    
    /**
     * Finds a node by its identifier.
     * 
     * @param nodeId node identifier
     * @return node information, or {@code null} if not found
     */
    public NodeInfo getNode(String nodeId) {
        for (NodeInfo node : nodes) {
            if (node.getNodeId().equals(nodeId)) {
                return node;
            }
        }
        return null;
    }
    
    /**
     * Returns the number of registered nodes.
     * 
     * @return node count
     */
    public int getNodeCount() {
        return nodes.size();
    }
    
    /**
     * Subscribes a node change listener.
     * 
     * @param listener listener to add
     */
    public void subscribe(NodeChangeListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Unsubscribes a node change listener.
     * 
     * @param listener listener to remove
     */
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
    
    /**
     * Clears all registered nodes.
     */
    public void clear() {
        nodes.clear();
    }
    
    /**
     * Returns the local node identifier.
     * 
     * @return local node ID
     */
    public String getLocalNodeId() {
        return localNodeId;
    }
}