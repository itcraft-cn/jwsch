package cn.itcraft.jwsch.srv.cluster;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for tracking local and remote connections in cluster.
 * 
 * <p>Maintains three indexes:
 * <ul>
 *   <li>localConnections: connectionId to ConnectionMeta (local connections)</li>
 *   <li>remoteConnections: connectionId to RemoteConnection (remote connections)</li>
 *   <li>nodeToConnections: nodeId to Set of connectionIds (for node-level operations)</li>
 * </ul>
 * 
 * <p>Thread-safe using ConcurrentHashMap.
 */
public class ClusterConnectionRegistry {
    
    private final String localNodeId;
    private final Map<Long, ConnectionMeta> localConnections = new ConcurrentHashMap<>();
    private final Map<Long, RemoteConnection> remoteConnections = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> nodeToConnections = new ConcurrentHashMap<>();
    
    /**
     * Creates a new connection registry for the local node.
     * 
     * @param localNodeId the ID of the local node
     */
    
    /**
     * Adds a local connection to the registry.
     * 
     * @param connectionId the connection identifier
     * @param meta metadata about the connection
     */
    
    /**
     * Removes a local connection from the registry.
     * 
     * <p>Also cleans up any remote connection entry and node index entries.
     * 
     * @param connectionId the connection identifier
     */
    
    /**
     * Adds a remote connection to the registry.
     * 
     * <p>Also updates the node-to-connections index for efficient lookups.
     * 
     * @param connectionId the connection identifier
     * @param connection the remote connection information
     */
    
    /**
     * Removes a remote connection from the registry.
     * 
     * <p>Also cleans up the node index entry.
     * 
     * @param connectionId the connection identifier
     */
    
    /**
     * Removes all connections for a specific node.
     * 
     * <p>Used when a node leaves the cluster or times out.
     * 
     * @param nodeId the node identifier
     */
    
    /**
     * Checks if a connection is local to this node.
     * 
     * @param connectionId the connection identifier
     * @return true if the connection is local, false otherwise
     */
    
    /**
     * Finds the node that owns a connection.
     * 
     * @param connectionId the connection identifier
     * @return the node identifier, or null if connection not found
     */
    
    /**
     * Gets the network address of the node that owns a connection.
     * 
     * <p>Returns null for local connections.
     * 
     * @param connectionId the connection identifier
     * @return the node address, or null for local connections or if not found
     */
    
    /**
     * Gets the node identifier for a connection.
     * 
     * @param connectionId the connection identifier
     * @return the node identifier, or null if connection not found
     */
    
    /**
     * Gets metadata for a local connection.
     * 
     * @param connectionId the connection identifier
     * @return the connection metadata, or null if not found
     */
    
    /**
     * Gets information for a remote connection.
     * 
     * @param connectionId the connection identifier
     * @return the remote connection, or null if not found
     */
    
    /**
     * Gets all local connection identifiers.
     * 
     * @return a set of local connection identifiers
     */
    
    /**
     * Gets all remote connection identifiers.
     * 
     * @return a set of remote connection identifiers
     */
    
    /**
     * Gets all connection identifiers for a specific node.
     * 
     * @param nodeId the node identifier
     * @return a set of connection identifiers for the node, empty if none
     */
    
    /**
     * Gets all known remote nodes with connections.
     * 
     * @return a set of node identifiers
     */
    
    /**
     * Gets all local connections.
     * 
     * @return an unmodifiable map of connection ID to metadata
     */
    
    /**
     * Gets all remote connections.
     * 
     * @return an unmodifiable map of connection ID to remote connection info
     */
    
    /**
     * Gets the number of local connections.
     * 
     * @return the count of local connections
     */
    
    /**
     * Gets the number of remote connections.
     * 
     * @return the count of remote connections
     */
    
    /**
     * Gets the total number of connections (local + remote).
     * 
     * @return the total connection count
     */
    
    /**
     * Clears all connection registrations.
     * 
     * <p>Useful for resetting state during node restart or cluster reconfiguration.
     */
    
    /**
     * Gets the local node identifier.
     * 
     * @return the local node ID
     */
    
    /**
     * Removes a connection from the node-to-connections index.
     * 
     * <p>If the node has no more connections, removes the node entry entirely.
     * 
     * @param nodeId the node identifier
     * @param connectionId the connection identifier
     */
}
