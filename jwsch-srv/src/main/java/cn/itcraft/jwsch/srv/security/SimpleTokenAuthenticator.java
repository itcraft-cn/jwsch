package cn.itcraft.jwsch.srv.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleTokenAuthenticator implements Authenticator {
    
    private static final String TYPE = "simple";
    
    private final Map<String, String> tokenStore = new ConcurrentHashMap<>();
    
    public SimpleTokenAuthenticator() {
    }
    
    public void addToken(String token, String principal) {
        tokenStore.put(token, principal);
    }
    
    public void removeToken(String token) {
        tokenStore.remove(token);
    }
    
    @Override
    public AuthenticationResult authenticate(String token) {
        if (token == null || token.isEmpty()) {
            return AuthenticationResult.failure("Token is empty");
        }
        
        String principal = tokenStore.get(token);
        if (principal != null) {
            return AuthenticationResult.success(principal);
        }
        
        return AuthenticationResult.failure("Invalid token");
    }
    
    @Override
    public String getType() {
        return TYPE;
    }
    
    public int getTokenCount() {
        return tokenStore.size();
    }
    
    public void clear() {
        tokenStore.clear();
    }
}