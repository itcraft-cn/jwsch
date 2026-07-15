package cn.itcraft.jwsch.srv.security;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class SimpleAuthorizerTest {
    
    private SimpleAuthorizer authorizer;
    
    @Before
    public void setUp() {
        authorizer = new SimpleAuthorizer();
    }
    
    @Test
    public void testAuthorize_exactPermission() {
        authorizer.grantPermission("user1", "topic:read");
        
        AuthorizationResult result = authorizer.authorize("user1", "topic:read");
        
        assertTrue(result.isAllowed());
    }
    
    @Test
    public void testAuthorize_noPermission() {
        AuthorizationResult result = authorizer.authorize("user1", "topic:read");
        
        assertFalse(result.isAllowed());
    }
    
    @Test
    public void testAuthorize_wildcardPermission() {
        authorizer.grantPermission("user1", "*");
        
        AuthorizationResult result = authorizer.authorize("user1", "topic:read");
        
        assertTrue(result.isAllowed());
    }
    
    @Test
    public void testAuthorize_resourceWildcard() {
        authorizer.grantPermission("user1", "topic:*");
        
        AuthorizationResult result1 = authorizer.authorize("user1", "topic:read");
        AuthorizationResult result2 = authorizer.authorize("user1", "topic:write");
        AuthorizationResult result3 = authorizer.authorize("user1", "user:read");
        
        assertTrue(result1.isAllowed());
        assertTrue(result2.isAllowed());
        assertFalse(result3.isAllowed());
    }
    
    @Test
    public void testAuthorize_nullPrincipal() {
        AuthorizationResult result = authorizer.authorize(null, "topic:read");
        
        assertFalse(result.isAllowed());
        assertEquals("Principal or permission is null", result.getReason());
    }
    
    @Test
    public void testAuthorize_nullPermission() {
        AuthorizationResult result = authorizer.authorize("user1", null);
        
        assertFalse(result.isAllowed());
    }
    
    @Test
    public void testAuthorize_resourceAction() {
        authorizer.grantPermission("user1", "topic:read");
        
        AuthorizationResult result = authorizer.authorize("user1", "topic", "read");
        
        assertTrue(result.isAllowed());
    }
    
    @Test
    public void testRevokePermission() {
        authorizer.grantPermission("user1", "topic:read");
        authorizer.revokePermission("user1", "topic:read");
        
        AuthorizationResult result = authorizer.authorize("user1", "topic:read");
        
        assertFalse(result.isAllowed());
    }
    
    @Test
    public void testSetPermissions() {
        Set<String> permissions = new HashSet<>();
        permissions.add("topic:read");
        permissions.add("topic:write");
        
        authorizer.setPermissions("user1", permissions);
        
        assertTrue(authorizer.authorize("user1", "topic:read").isAllowed());
        assertTrue(authorizer.authorize("user1", "topic:write").isAllowed());
    }
    
    @Test
    public void testGetPermissions() {
        authorizer.grantPermission("user1", "topic:read");
        authorizer.grantPermission("user1", "topic:write");
        
        Set<String> permissions = authorizer.getPermissions("user1");
        
        assertEquals(2, permissions.size());
        assertTrue(permissions.contains("topic:read"));
        assertTrue(permissions.contains("topic:write"));
    }
    
    @Test
    public void testGetPermissions_unknownPrincipal() {
        Set<String> permissions = authorizer.getPermissions("unknown");
        
        assertTrue(permissions.isEmpty());
    }
    
    @Test
    public void testClear() {
        authorizer.grantPermission("user1", "topic:read");
        authorizer.clear();
        
        assertFalse(authorizer.authorize("user1", "topic:read").isAllowed());
    }
}