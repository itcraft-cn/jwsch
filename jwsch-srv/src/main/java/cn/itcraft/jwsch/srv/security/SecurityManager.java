package cn.itcraft.jwsch.srv.security;

public class SecurityManager {
    
    private final Authenticator authenticator;
    private final Authorizer authorizer;
    
    public SecurityManager() {
        this(new SimpleTokenAuthenticator(), new SimpleAuthorizer());
    }
    
    public SecurityManager(Authenticator authenticator, Authorizer authorizer) {
        this.authenticator = authenticator;
        this.authorizer = authorizer;
    }
    
    public AuthenticationResult authenticate(String token) {
        return authenticator.authenticate(token);
    }
    
    public AuthorizationResult authorize(String principal, String permission) {
        return authorizer.authorize(principal, permission);
    }
    
    public AuthorizationResult authorize(String principal, String resource, String action) {
        return authorizer.authorize(principal, resource, action);
    }
    
    public AuthenticationResult authenticateAndAuthorize(String token, String permission) {
        AuthenticationResult authResult = authenticate(token);
        if (!authResult.isSuccess()) {
            return authResult;
        }
        
        AuthorizationResult authzResult = authorize(authResult.getPrincipal(), permission);
        if (!authzResult.isAllowed()) {
            return AuthenticationResult.failure(authzResult.getReason());
        }
        
        return authResult;
    }
    
    public Authenticator getAuthenticator() {
        return authenticator;
    }
    
    public Authorizer getAuthorizer() {
        return authorizer;
    }
}