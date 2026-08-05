package cn.itcraft.jwsch.srv.cluster;

/**
 * Status of a cluster node.
 */
public enum NodeStatus {
    /**
     * Node is healthy and accepting connections.
     */
    UP,
    
    /**
     * Node is unavailable or unreachable.
     */
    DOWN
}