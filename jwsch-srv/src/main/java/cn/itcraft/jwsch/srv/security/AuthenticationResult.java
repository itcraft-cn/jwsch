package cn.itcraft.jwsch.srv.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AuthenticationResult {
    
    private final boolean success;
    private final String principal;
    private final Map<String, String> attributes;
    private final String errorMessage;
    
    private AuthenticationResult(boolean success, String principal, 
                                  Map<String, String> attributes, String errorMessage) {
        this.success = success;
        this.principal = principal;
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
        this.errorMessage = errorMessage;
    }
    
    public static AuthenticationResult success(String principal) {
        return new AuthenticationResult(true, principal, null, null);
    }
    
    public static AuthenticationResult success(String principal, Map<String, String> attributes) {
        return new AuthenticationResult(true, principal, attributes, null);
    }
    
    public static AuthenticationResult failure(String errorMessage) {
        return new AuthenticationResult(false, null, null, errorMessage);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getPrincipal() {
        return principal;
    }
    
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
    
    public String getAttribute(String key) {
        return attributes.get(key);
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    @Override
    public String toString() {
        return "AuthenticationResult{" +
            "success=" + success +
            ", principal='" + principal + '\'' +
            ", errorMessage='" + errorMessage + '\'' +
            '}';
    }
}