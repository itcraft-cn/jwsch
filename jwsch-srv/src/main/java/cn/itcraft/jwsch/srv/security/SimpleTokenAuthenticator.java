package cn.itcraft.jwsch.srv.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单令牌认证器实现。
 * 
 * <p>基于内存的令牌存储，支持令牌的添加、移除和验证。
 * 令牌与主体标识（principal）的映射关系存储在内存中。
 * 
 * <p>适用于开发和测试环境，生产环境应使用更安全的认证方案（如 JWT、OAuth2）。
 */
public class SimpleTokenAuthenticator implements Authenticator {
    
    private static final String TYPE = "simple";
    
    private final Map<String, String> tokenStore = new ConcurrentHashMap<>();
    
    /**
     * 创建简单令牌认证器。
     */
    public SimpleTokenAuthenticator() {
    }
    
    /**
     * 添加令牌到存储。
     *
     * @param token 令牌字符串
     * @param principal 主体标识
     * @throws NullPointerException 如果 token 或 principal 为 null
     */
    public void addToken(String token, String principal) {
        tokenStore.put(token, principal);
    }
    
    /**
     * 从存储中移除令牌。
     *
     * @param token 要移除的令牌
     */
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
    
    /**
     * 获取存储的令牌数量。
     *
     * @return 令牌数量
     */
    public int getTokenCount() {
        return tokenStore.size();
    }
    
    /**
     * 清空所有令牌存储。
     */
    public void clear() {
        tokenStore.clear();
    }
}