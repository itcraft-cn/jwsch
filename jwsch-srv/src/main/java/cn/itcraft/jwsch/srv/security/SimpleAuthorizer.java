package cn.itcraft.jwsch.srv.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单授权器实现。
 * 
 * <p>基于内存的权限存储，支持权限的授予、撤销和查询。
 * 支持通配符权限匹配：
 * <ul>
 *   <li>"*"：匹配所有权限</li>
 *   <li>"resource:*"：匹配指定资源的所有操作</li>
 * </ul>
 * 
 * <p>权限格式建议为 "resource:action"（如 "topic:read"），
 * 也支持任意字符串格式。
 */
public class SimpleAuthorizer implements Authorizer {
    
    private final Map<String, Set<String>> permissionStore = new ConcurrentHashMap<>();
    
    /**
     * 创建简单授权器。
     */
    public SimpleAuthorizer() {
    }
    
    /**
     * 授予权限给主体。
     *
     * @param principal 主体标识
     * @param permission 权限字符串
     * @throws NullPointerException 如果 principal 或 permission 为 null
     */
    public void grantPermission(String principal, String permission) {
        permissionStore.computeIfAbsent(principal, k -> ConcurrentHashMap.newKeySet())
            .add(permission);
    }
    
    /**
     * 撤销主体的特定权限。
     *
     * @param principal 主体标识
     * @param permission 权限字符串
     */
    public void revokePermission(String principal, String permission) {
        Set<String> permissions = permissionStore.get(principal);
        if (permissions != null) {
            permissions.remove(permission);
        }
    }
    
    /**
     * 设置主体的权限集合（替换现有权限）。
     *
     * @param principal 主体标识
     * @param permissions 权限集合
     * @throws NullPointerException 如果 principal 或 permissions 为 null
     */
    public void setPermissions(String principal, Set<String> permissions) {
        permissionStore.put(principal, new HashSet<>(permissions));
    }
    
    /**
     * 获取主体的权限集合（不可修改的视图）。
     *
     * @param principal 主体标识
     * @return 权限集合（不会为 null）
     */
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
    
    /**
     * 清空所有权限存储。
     */
    public void clear() {
        permissionStore.clear();
    }
}