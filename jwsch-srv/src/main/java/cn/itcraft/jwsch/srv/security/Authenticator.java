package cn.itcraft.jwsch.srv.security;

/**
 * 认证器接口。
 * 
 * <p>定义认证组件的标准接口，负责验证客户端提供的凭证（如 token）。
 * 认证器根据凭证返回 {@link AuthenticationResult}，包含认证是否成功、主体标识和附加属性。
 * 
 * <p>实现类应提供 {@link #getType()} 方法返回认证器类型，用于配置和日志记录。
 */
public interface Authenticator {
    
    /**
     * 认证客户端提供的凭证。
     *
     * @param token 客户端提供的认证凭证（如 JWT token、API key 等）
     * @return 认证结果，包含认证是否成功、主体标识和附加属性
     * @throws NullPointerException 如果 token 为 null
     */
    AuthenticationResult authenticate(String token);
    
    /**
     * 获取认证器类型。
     *
     * @return 认证器类型标识（如 "token"、"jwt"、"basic"）
     */
    String getType();
}