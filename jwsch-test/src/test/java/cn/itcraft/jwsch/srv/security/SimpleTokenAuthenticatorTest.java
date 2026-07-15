package cn.itcraft.jwsch.srv.security;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SimpleTokenAuthenticatorTest {
    
    private SimpleTokenAuthenticator authenticator;
    
    @Before
    public void setUp() {
        authenticator = new SimpleTokenAuthenticator();
    }
    
    @Test
    public void testAuthenticate_validToken() {
        authenticator.addToken("token123", "user1");
        
        AuthenticationResult result = authenticator.authenticate("token123");
        
        assertTrue(result.isSuccess());
        assertEquals("user1", result.getPrincipal());
    }
    
    @Test
    public void testAuthenticate_invalidToken() {
        authenticator.addToken("token123", "user1");
        
        AuthenticationResult result = authenticator.authenticate("wrong-token");
        
        assertFalse(result.isSuccess());
        assertEquals("Invalid token", result.getErrorMessage());
    }
    
    @Test
    public void testAuthenticate_nullToken() {
        AuthenticationResult result = authenticator.authenticate(null);
        
        assertFalse(result.isSuccess());
        assertEquals("Token is empty", result.getErrorMessage());
    }
    
    @Test
    public void testAuthenticate_emptyToken() {
        AuthenticationResult result = authenticator.authenticate("");
        
        assertFalse(result.isSuccess());
        assertEquals("Token is empty", result.getErrorMessage());
    }
    
    @Test
    public void testRemoveToken() {
        authenticator.addToken("token123", "user1");
        authenticator.removeToken("token123");
        
        AuthenticationResult result = authenticator.authenticate("token123");
        
        assertFalse(result.isSuccess());
    }
    
    @Test
    public void testGetType() {
        assertEquals("simple", authenticator.getType());
    }
    
    @Test
    public void testGetTokenCount() {
        authenticator.addToken("token1", "user1");
        authenticator.addToken("token2", "user2");
        
        assertEquals(2, authenticator.getTokenCount());
    }
    
    @Test
    public void testClear() {
        authenticator.addToken("token1", "user1");
        authenticator.clear();
        
        assertEquals(0, authenticator.getTokenCount());
    }
}