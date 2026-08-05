package cn.itcraft.jwsch.srv.security;

import java.util.Set;

/**
 * 授权结果。
 * 
 * <p>封装授权操作的返回结果，包含是否允许访问以及拒绝原因。
 * 授权成功后 {@link #isAllowed()} 返回 true；授权失败时包含拒绝原因。
 * 
 * <p>该类不可变，所有修改方法都返回新的实例。
 */
public class AuthorizationResult {
    
    private final boolean allowed;
    private final String reason;
    
    private AuthorizationResult(boolean allowed, String reason) {
        this.allowed = allowed;
        this.reason = reason;
    }
    
    /**
     * 创建授权允许的结果。
     *
     * @return 授权允许的结果
     */
    public static AuthorizationResult allowed() {
        return new AuthorizationResult(true, null);
    }
    
    /**
     * 创建授权拒绝的结果。
     *
     * @param reason 拒绝原因
     * @return 授权拒绝的结果
     * @throws NullPointerException 如果 reason 为 null
     */
    public static AuthorizationResult denied(String reason) {
        return new AuthorizationResult(false, reason);
    }
    
    /**
     * 检查是否允许访问。
     *
     * @return true 如果允许访问，否则 false
     */
    public boolean isAllowed() {
        return allowed;
    }
    
    /**
     * 获取拒绝原因。
     *
     * @return 拒绝原因（授权允许时为 null）
     */
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