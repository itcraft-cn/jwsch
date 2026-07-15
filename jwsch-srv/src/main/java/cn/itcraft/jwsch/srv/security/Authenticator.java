package cn.itcraft.jwsch.srv.security;

public interface Authenticator {
    
    AuthenticationResult authenticate(String token);
    
    String getType();
}