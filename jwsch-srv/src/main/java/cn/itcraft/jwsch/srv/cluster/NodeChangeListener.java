package cn.itcraft.jwsch.srv.cluster;

/**
 * Listener for node membership changes in the cluster.
 * 
 * <p>Implementations are notified when nodes join or leave the cluster mesh.
 * Used by cluster components to react to topology changes.
 */
public interface NodeChangeListener {
    
    /**
     * Called when a new node joins the cluster.
     * 
     * @param node information about the newly joined node
     */
    void onNodeJoin(NodeInfo node);
    
    /**
     * Called when a node leaves the cluster.
     * 
     * @param nodeId identifier of the node that left
     */
    void onNodeLeave(String nodeId);
}