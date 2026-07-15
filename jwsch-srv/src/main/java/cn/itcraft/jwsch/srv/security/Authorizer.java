package cn.itcraft.jwsch.srv.security;

public interface Authorizer {
    
    AuthorizationResult authorize(String principal, String permission);
    
    AuthorizationResult authorize(String principal, String resource, String action);
}