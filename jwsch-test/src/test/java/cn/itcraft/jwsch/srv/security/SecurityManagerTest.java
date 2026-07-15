package cn.itcraft.jwsch.srv.security;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SecurityManagerTest {
    
    private SecurityManager securityManager;
    private SimpleTokenAuthenticator authenticator;
    private SimpleAuthorizer authorizer;
    
    @Before
    public void setUp() {
        authenticator = new SimpleTokenAuthenticator();
        authorizer = new SimpleAuthorizer();
        securityManager = new SecurityManager(authenticator, authorizer);
    }
    
    @Test
    public void testAuthenticate() {
        authenticator.addToken("token123", "user1");
        
        AuthenticationResult result = securityManager.authenticate("token123");
        
        assertTrue(result.isSuccess());
        assertEquals("user1", result.getPrincipal());
    }
    
    @Test
    public void testAuthorize() {
        authorizer.grantPermission("user1", "topic:read");
        
        AuthorizationResult result = securityManager.authorize("user1", "topic:read");
        
        assertTrue(result.isAllowed());
    }
    
    @Test
    public void testAuthorize_resourceAction() {
        authorizer.grantPermission("user1", "topic:read");
        
        AuthorizationResult result = securityManager.authorize("user1", "topic", "read");
        
        assertTrue(result.isAllowed());
    }
    
    @Test
    public void testAuthenticateAndAuthorize_success() {
        authenticator.addToken("token123", "user1");
        authorizer.grantPermission("user1", "topic:read");
        
        AuthenticationResult result = securityManager.authenticateAndAuthorize("token123", "topic:read");
        
        assertTrue(result.isSuccess());
        assertEquals("user1", result.getPrincipal());
    }
    
    @Test
    public void testAuthenticateAndAuthorize_authFail() {
        AuthenticationResult result = securityManager.authenticateAndAuthorize("wrong-token", "topic:read");
        
        assertFalse(result.isSuccess());
    }
    
    @Test
    public void testAuthenticateAndAuthorize_authzFail() {
        authenticator.addToken("token123", "user1");
        
        AuthenticationResult result = securityManager.authenticateAndAuthorize("token123", "topic:read");
        
        assertFalse(result.isSuccess());
    }
    
    @Test
    public void testDefaultConstructor() {
        SecurityManager defaultManager = new SecurityManager();
        
        assertNotNull(defaultManager.getAuthenticator());
        assertNotNull(defaultManager.getAuthorizer());
    }
    
    @Test
    public void testGetAuthenticator() {
        assertSame(authenticator, securityManager.getAuthenticator());
    }
    
    @Test
    public void testGetAuthorizer() {
        assertSame(authorizer, securityManager.getAuthorizer());
    }
}