package cn.itcraft.jwsch.cli.connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConnectionRegistry {
    
    private final ConcurrentMap<Long, ConnectionInfo> connectionMap;
    private final ConcurrentMap<String, Set<Long>> addressIndex;
    
    public ConnectionRegistry() {
        this.connectionMap = new ConcurrentHashMap<>();
        this.addressIndex = new ConcurrentHashMap<>();
    }
    
    public ConnectionRegistry(int initialCapacity) {
        this.connectionMap = new ConcurrentHashMap<>(initialCapacity);
        this.addressIndex = new ConcurrentHashMap<>(initialCapacity);
    }
    
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
    
    public ConnectionInfo lookup(long connectionId) {
        return connectionMap.get(connectionId);
    }
    
    public List<ConnectionInfo> lookupAll() {
        return new ArrayList<>(connectionMap.values());
    }
    
    public List<ConnectionInfo> lookupByType(ConnectionType type) {
        List<ConnectionInfo> result = new ArrayList<>();
        for (ConnectionInfo info : connectionMap.values()) {
            if (info.getConnectionType() == type) {
                result.add(info);
            }
        }
        return result;
    }
    
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
    
    public List<ConnectionInfo> lookupByStatus(ConnectionStatus status) {
        List<ConnectionInfo> result = new ArrayList<>();
        for (ConnectionInfo info : connectionMap.values()) {
            if (info.getStatus() == status) {
                result.add(info);
            }
        }
        return result;
    }
    
    public int getConnectionCount() {
        return connectionMap.size();
    }
    
    public int getConnectionCount(ConnectionType type) {
        int count = 0;
        for (ConnectionInfo info : connectionMap.values()) {
            if (info.getConnectionType() == type) {
                count++;
            }
        }
        return count;
    }
    
    public void clear() {
        connectionMap.clear();
        addressIndex.clear();
    }
    
    public boolean contains(long connectionId) {
        return connectionMap.containsKey(connectionId);
    }
}