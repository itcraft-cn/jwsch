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
    
    public ClusterConnectionRegistry(String localNodeId) {
        this.localNodeId = localNodeId;
    }
    
    public void addLocalConnection(long connectionId, ConnectionMeta meta) {
        localConnections.put(connectionId, meta);
    }
    
    public void removeLocalConnection(long connectionId) {
        localConnections.remove(connectionId);
        
        String nodeId = findNodeForConnection(connectionId);
        if (nodeId != null && !localNodeId.equals(nodeId)) {
            removeFromNodeIndex(nodeId, connectionId);
        }
        remoteConnections.remove(connectionId);
    }
    
    public void addRemoteConnection(long connectionId, RemoteConnection connection) {
        remoteConnections.put(connectionId, connection);
        
        String nodeId = connection.getNodeId();
        nodeToConnections.computeIfAbsent(nodeId, k -> ConcurrentHashMap.newKeySet())
            .add(connectionId);
    }
    
    public void removeRemoteConnection(long connectionId) {
        RemoteConnection removed = remoteConnections.remove(connectionId);
        if (removed != null) {
            removeFromNodeIndex(removed.getNodeId(), connectionId);
        }
    }
    
    public void removeNodeConnections(String nodeId) {
        Set<Long> connectionIds = nodeToConnections.remove(nodeId);
        if (connectionIds != null) {
            for (Long connectionId : connectionIds) {
                remoteConnections.remove(connectionId);
            }
        }
    }
    
    public boolean isLocal(long connectionId) {
        return localConnections.containsKey(connectionId);
    }
    
    public String findNodeForConnection(long connectionId) {
        if (isLocal(connectionId)) {
            return localNodeId;
        }
        
        RemoteConnection remote = remoteConnections.get(connectionId);
        return remote != null ? remote.getNodeId() : null;
    }
    
    public String getNodeAddress(long connectionId) {
        if (isLocal(connectionId)) {
            return null;
        }
        
        RemoteConnection remote = remoteConnections.get(connectionId);
        return remote != null ? remote.getNodeAddress() : null;
    }
    
    public String getNodeId(long connectionId) {
        return findNodeForConnection(connectionId);
    }
    
    public ConnectionMeta getLocalConnection(long connectionId) {
        return localConnections.get(connectionId);
    }
    
    public RemoteConnection getRemoteConnection(long connectionId) {
        return remoteConnections.get(connectionId);
    }
    
    public Set<Long> getLocalConnectionIds() {
        return new HashSet<>(localConnections.keySet());
    }
    
    public Set<Long> getRemoteConnectionIds() {
        return new HashSet<>(remoteConnections.keySet());
    }
    
    public Set<Long> getConnectionIdsForNode(String nodeId) {
        Set<Long> connectionIds = nodeToConnections.get(nodeId);
        return connectionIds != null ? Collections.unmodifiableSet(connectionIds) : Collections.emptySet();
    }
    
    public Set<String> getKnownNodes() {
        return new HashSet<>(nodeToConnections.keySet());
    }
    
    public Map<Long, ConnectionMeta> getAllLocalConnections() {
        return Collections.unmodifiableMap(localConnections);
    }
    
    public Map<Long, RemoteConnection> getAllRemoteConnections() {
        return Collections.unmodifiableMap(remoteConnections);
    }
    
    public int getLocalConnectionCount() {
        return localConnections.size();
    }
    
    public int getRemoteConnectionCount() {
        return remoteConnections.size();
    }
    
    public int getTotalConnectionCount() {
        return localConnections.size() + remoteConnections.size();
    }
    
    public void clear() {
        localConnections.clear();
        remoteConnections.clear();
        nodeToConnections.clear();
    }
    
    public String getLocalNodeId() {
        return localNodeId;
    }
    
    private void removeFromNodeIndex(String nodeId, long connectionId) {
        Set<Long> connectionIds = nodeToConnections.get(nodeId);
        if (connectionIds != null) {
            connectionIds.remove(connectionId);
            if (connectionIds.isEmpty()) {
                nodeToConnections.remove(nodeId);
            }
        }
    }
}
