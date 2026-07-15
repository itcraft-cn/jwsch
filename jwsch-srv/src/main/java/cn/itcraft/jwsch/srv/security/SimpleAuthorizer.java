package cn.itcraft.jwsch.srv.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleAuthorizer implements Authorizer {
    
    private final Map<String, Set<String>> permissionStore = new ConcurrentHashMap<>();
    
    public SimpleAuthorizer() {
    }
    
    public void grantPermission(String principal, String permission) {
        permissionStore.computeIfAbsent(principal, k -> ConcurrentHashMap.newKeySet())
            .add(permission);
    }
    
    public void revokePermission(String principal, String permission) {
        Set<String> permissions = permissionStore.get(principal);
        if (permissions != null) {
            permissions.remove(permission);
        }
    }
    
    public void setPermissions(String principal, Set<String> permissions) {
        permissionStore.put(principal, new HashSet<>(permissions));
    }
    
    public Set<String> getPermissions(String principal) {
        Set<String> permissions = permissionStore.get(principal);
        return permissions != null ? Collections.unmodifiableSet(permissions) : Collections.emptySet();
    }
    
    @Override
    public AuthorizationResult authorize(String principal, String permission) {
        if (principal == null || permission == null) {
            return AuthorizationResult.denied("Principal or permission is null");
        }
        
        Set<String> permissions = permissionStore.get(principal);
        if (permissions == null || permissions.isEmpty()) {
            return AuthorizationResult.denied("No permissions for principal: " + principal);
        }
        
        if (permissions.contains("*") || permissions.contains(permission)) {
            return AuthorizationResult.allowed();
        }
        
        if (permission.contains(":")) {
            String[] parts = permission.split(":", 2);
            String wildcardPermission = parts[0] + ":*";
            if (permissions.contains(wildcardPermission)) {
                return AuthorizationResult.allowed();
            }
        }
        
        return AuthorizationResult.denied("Permission denied: " + permission);
    }
    
    @Override
    public AuthorizationResult authorize(String principal, String resource, String action) {
        String permission = resource + ":" + action;
        return authorize(principal, permission);
    }
    
    public void clear() {
        permissionStore.clear();
    }
}