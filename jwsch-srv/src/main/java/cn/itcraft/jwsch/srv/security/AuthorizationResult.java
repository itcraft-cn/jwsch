package cn.itcraft.jwsch.srv.security;

import java.util.Set;

public class AuthorizationResult {
    
    private final boolean allowed;
    private final String reason;
    
    private AuthorizationResult(boolean allowed, String reason) {
        this.allowed = allowed;
        this.reason = reason;
    }
    
    public static AuthorizationResult allowed() {
        return new AuthorizationResult(true, null);
    }
    
    public static AuthorizationResult denied(String reason) {
        return new AuthorizationResult(false, reason);
    }
    
    public boolean isAllowed() {
        return allowed;
    }
    
    public String getReason() {
        return reason;
    }
    
    @Override
    public String toString() {
        return "AuthorizationResult{" +
            "allowed=" + allowed +
            ", reason='" + reason + '\'' +
            '}';
    }
}