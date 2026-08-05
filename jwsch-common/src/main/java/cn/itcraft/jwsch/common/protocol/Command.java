package cn.itcraft.jwsch.common.protocol;

/**
 * Command definitions for jwsch protocol.
 * 
 * <p>Commands are categorized into two ranges:
 * <ul>
 *   <li>0x01-0x08: Client-Server commands (REQUEST, RESPONSE, PUSH, etc.)</li>
 *   <li>0x10-0x15: Cluster commands (JOIN, MEMBERSHIP, SYNC, etc.)</li>
 * </ul>
 * 
 * <p>Client-Server commands are used for communication between clients and server,
 * Cluster commands are used for inter-node communication in cluster mode.
 */
public final class Command {
    
    /**
     * Client request to server - sends a message targeting specific connection
     * Header fields: sourceId, targetId, topic (optional), body
     */
    public static final byte REQUEST = 0x01;
    
    /**
     * Server response to client request - acknowledges processing result
     * Header fields: sourceId (server), targetId (client), errorCode, body (optional)
     */
    public static final byte RESPONSE = 0x02;
    
    /**
     * Push message from publisher to all subscribers of a topic
     * Header fields: sourceId (publisher), topic, body
     */
    public static final byte PUSH = 0x03;
    
    /**
     * Broadcast message from publisher to all connected clients
     * Header fields: sourceId (publisher), body
     */
    public static final byte BROADCAST = 0x04;
    
    /**
     * Client subscription request to a topic
     * Header fields: sourceId (client), topic
     */
    public static final byte SUBSCRIBE = 0x05;
    
    /**
     * Heartbeat message to keep connection alive
     * Header fields: sourceId (sender)
     */
    public static final byte HEARTBEAT = 0x06;
    
    /**
     * Acknowledgment for non-request messages
     * Header fields: sourceId (ack sender), targetId (ack receiver)
     */
    public static final byte ACK = 0x07;
    
    /**
     * Server response to client connection request
     * Header fields: sourceId (server), targetId (assigned connectionId), errorCode
     */
    public static final byte CONNECT_RESPONSE = 0x08;
    
    /**
     * Cluster: node join notification to announce new node
     * Used when a new node joins the cluster
     */
    public static final byte CLUSTER_JOIN = 0x10;
    
    /**
     * Cluster: node membership update to sync node status
     * Broadcasts node join/leave events
     */
    public static final byte CLUSTER_MEMBERSHIP = 0x11;
    
    /**
     * Cluster: connection and topic synchronization between nodes
     * Periodically syncs connection and subscription information
     */
    public static final byte CLUSTER_SYNC = 0x12;
    
    /**
     * Cluster: forward REQUEST to target node
     * Routes client request to node where target connection resides
     */
    public static final byte CLUSTER_FORWARD = 0x13;
    
    /**
     * Cluster: broadcast PUSH/BROADCAST to all nodes
     * Diffuses push/broadcast messages across cluster
     */
    public static final byte CLUSTER_BROADCAST = 0x14;
    
    /**
     * Cluster: heartbeat between cluster nodes
     * Detects node failures and maintains cluster health
     */
    public static final byte CLUSTER_HEARTBEAT = 0x15;
    
    private Command() {
    }
    
    /**
     * Validates if a command byte is within defined ranges
     * 
     * @param command the command byte to validate
     * @return true if command is valid, false otherwise
     */
    public static boolean isValid(byte command) {
        return (command >= REQUEST && command <= CONNECT_RESPONSE) 
            || (command >= CLUSTER_JOIN && command <= CLUSTER_HEARTBEAT);
    }
    
    /**
     * Checks if a command is a cluster command
     * 
     * @param command the command byte to check
     * @return true if command is a cluster command (0x10-0x15), false otherwise
     */
    public static boolean isClusterCommand(byte command) {
        return command >= CLUSTER_JOIN && command <= CLUSTER_HEARTBEAT;
    }
}
