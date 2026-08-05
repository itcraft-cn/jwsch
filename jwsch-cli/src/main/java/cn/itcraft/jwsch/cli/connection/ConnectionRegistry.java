package cn.itcraft.jwsch.cli.connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Connection registry for tracking active connections.
 *
 * <p>Thread-safe registry that maintains two indexes:
 * <ul>
 *   <li>Primary index: connectionId → ConnectionInfo</li>
 *   <li>Secondary index: remoteAddress → set of connectionIds</li>
 * </ul>
 *
 * <p>Features:
 * <ul>
 *   <li>Concurrent registration and unregistration</li>
 *   <li>Lookup by connection ID, type, status, or address</li>
 *   <li>Connection count statistics</li>
 *   <li>Automatic index cleanup on unregistration</li>
 * </ul>
 *
 * <p>Used by client connection pools to track TCP connections.
 */
public class ConnectionRegistry {
    
    private final ConcurrentMap<Long, ConnectionInfo> connectionMap;
    private final ConcurrentMap<String, Set<Long>> addressIndex;
    
    /**
     * Creates a ConnectionRegistry with default capacity.
     */
    public ConnectionRegistry() {
        this.connectionMap = new ConcurrentHashMap<>();
        this.addressIndex = new ConcurrentHashMap<>();
    }
    
    /**
     * Creates a ConnectionRegistry with specified initial capacity.
     *
     * @param initialCapacity initial hash table capacity
     */
    public ConnectionRegistry(int initialCapacity) {
        this.connectionMap = new ConcurrentHashMap<>(initialCapacity);
        this.addressIndex = new ConcurrentHashMap<>(initialCapacity);
    }
    
    /**
     * Registers a connection.
     *
     * @param info connection information
     * @throws IllegalArgumentException if info is null
     */
    public void register(ConnectionInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("ConnectionInfo cannot be null");
        }
        
        long connectionId = info.getConnectionId();
        connectionMap.put(connectionId, info);
        
        String remoteAddress = info.getRemoteAddress();
        if (remoteAddress != null) {
            addressIndex.computeIfAbsent(remoteAddress, k -> ConcurrentHashMap.newKeySet())
                .add(connectionId);
        }
    }
    
    /**
     * Unregisters a connection by ID.
     *
     * @param connectionId connection identifier
     * @return removed ConnectionInfo, or null if not found
     */
    public ConnectionInfo unregister(long connectionId) {
        ConnectionInfo info = connectionMap.remove(connectionId);
        
        if (info != null) {
            String remoteAddress = info.getRemoteAddress();
            if (remoteAddress != null) {
                Set<Long> ids = addressIndex.get(remoteAddress);
                if (ids != null) {
                    ids.remove(connectionId);
                    if (ids.isEmpty()) {
                        addressIndex.remove(remoteAddress);
                    }
                }
            }
        }
        
        return info;
    }
    
    /**
     * Looks up connection by ID.
     *
     * @param connectionId connection identifier
     * @return ConnectionInfo or null
     */
    public ConnectionInfo lookup(long connectionId) {
        return connectionMap.get(connectionId);
    }
    
    /**
     * Returns all registered connections.
     *
     * @return list of all connections (copy)
     */
    public List<ConnectionInfo> lookupAll() {
        return new ArrayList<>(connectionMap.values());
    }
    
    /**
     * Looks up connections by type.
     *
     * @param type connection type filter
     * @return list of matching connections
     */
    public List<ConnectionInfo> lookupByType(ConnectionType type) {
        List<ConnectionInfo> result = new ArrayList<>();
        for (ConnectionInfo info : connectionMap.values()) {
            if (info.getConnectionType() == type) {
                result.add(info);
            }
        }
        return result;
    }
    
    /**
     * Looks up connections by remote address.
     *
     * @param remoteAddress remote address (IP:port)
     * @return list of connections to specified address
     */
    public List<ConnectionInfo> lookupByRemoteAddress(String remoteAddress) {
        Set<Long> ids = addressIndex.get(remoteAddress);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<ConnectionInfo> result = new ArrayList<>();
        for (Long id : ids) {
            ConnectionInfo info = connectionMap.get(id);
            if (info != null) {
                result.add(info);
            }
        }
        return result;
    }
    
    /**
     * Looks up connections by status.
     *
     * @param status connection status filter
     * @return list of matching connections
     */
    public List<ConnectionInfo> lookupByStatus(ConnectionStatus status) {
        List<ConnectionInfo> result = new ArrayList<>();
        for (ConnectionInfo info : connectionMap.values()) {
            if (info.getStatus() == status) {
                result.add(info);
            }
        }
        return result;
    }
    
    /**
     * Returns total number of connections.
     */
    public int getConnectionCount() {
        return connectionMap.size();
    }
    
    /**
     * Returns number of connections of specified type.
     *
     * @param type connection type to count
     * @return count of matching connections
     */
    public int getConnectionCount(ConnectionType type) {
        int count = 0;
        for (ConnectionInfo info : connectionMap.values()) {
            if (info.getConnectionType() == type) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Clears all connections and indexes.
     */
    public void clear() {
        connectionMap.clear();
        addressIndex.clear();
    }
    
    /**
     * Checks if connection ID is registered.
     *
     * @param connectionId connection identifier
     * @return true if connection exists
     */
    public boolean contains(long connectionId) {
        return connectionMap.containsKey(connectionId);
    }
}