package cn.itcraft.jwsch.srv.security;

/**
 * 安全管理器。
 * 
 * <p>统一的认证和授权入口，封装 {@link Authenticator} 和 {@link Authorizer} 组件。
 * 提供便捷方法处理认证、授权以及二者组合的操作。
 * 
 * <p>默认使用 {@link SimpleTokenAuthenticator} 和 {@link SimpleAuthorizer}，
 * 可通过构造函数注入自定义实现。
 */
public class SecurityManager {
    
    private final Authenticator authenticator;
    private final Authorizer authorizer;
    
    /**
     * 使用默认的认证器和授权器创建安全管理器。
     */
    public SecurityManager() {
        this(new SimpleTokenAuthenticator(), new SimpleAuthorizer());
    }
    
    /**
     * 使用指定的认证器和授权器创建安全管理器。
     *
     * @param authenticator 认证器实现
     * @param authorizer 授权器实现
     * @throws NullPointerException 如果 authenticator 或 authorizer 为 null
     */
    public SecurityManager(Authenticator authenticator, Authorizer authorizer) {
        this.authenticator = authenticator;
        this.authorizer = authorizer;
    }
    
    /**
     * 认证客户端提供的凭证。
     *
     * @param token 客户端提供的认证凭证
     * @return 认证结果
     * @throws NullPointerException 如果 token 为 null
     */
    public AuthenticationResult authenticate(String token) {
        return authenticator.authenticate(token);
    }
    
    /**
     * 基于权限字符串授权。
     *
     * @param principal 主体标识
     * @param permission 权限字符串
     * @return 授权结果
     * @throws NullPointerException 如果 principal 或 permission 为 null
     */
    public AuthorizationResult authorize(String principal, String permission) {
        return authorizer.authorize(principal, permission);
    }
    
    /**
     * 基于资源和操作授权。
     *
     * @param principal 主体标识
     * @param resource 资源标识
     * @param action 操作类型
     * @return 授权结果
     * @throws NullPointerException 如果 principal、resource 或 action 为 null
     */
    public AuthorizationResult authorize(String principal, String resource, String action) {
        return authorizer.authorize(principal, resource, action);
    }
    
    /**
     * 认证并授权（组合操作）。
     * 
     * <p>先认证 token，如果认证成功则检查主体是否有指定权限。
     * 如果认证失败，返回认证失败结果；如果授权失败，返回认证失败结果（原因使用授权拒绝原因）。
     *
     * @param token 客户端提供的认证凭证
     * @param permission 需要检查的权限
     * @return 认证结果（如果授权失败，返回认证失败结果）
     * @throws NullPointerException 如果 token 或 permission 为 null
     */
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
    
    /**
     * 获取认证器。
     *
     * @return 认证器实例
     */
    public Authenticator getAuthenticator() {
        return authenticator;
    }
    
    /**
     * 获取授权器。
     *
     * @return 授权器实例
     */
    public Authorizer getAuthorizer() {
        return authorizer;
    }
}