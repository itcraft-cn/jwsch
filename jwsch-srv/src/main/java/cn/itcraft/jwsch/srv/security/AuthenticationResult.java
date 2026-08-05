package cn.itcraft.jwsch.srv.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证结果。
 * 
 * <p>封装认证操作的返回结果，包含认证是否成功、主体标识、附加属性和错误信息。
 * 认证成功后可以获取 principal 和可选属性；认证失败时包含错误消息。
 * 
 * <p>该类不可变，所有修改方法都返回新的实例。
 */
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
    
    /**
     * 创建认证成功的结果。
     *
     * @param principal 认证通过的主体标识（如用户 ID）
     * @return 认证成功的结果
     * @throws NullPointerException 如果 principal 为 null
     */
    public static AuthenticationResult success(String principal) {
        return new AuthenticationResult(true, principal, null, null);
    }
    
    /**
     * 创建认证成功的结果（带附加属性）。
     *
     * @param principal 认证通过的主体标识
     * @param attributes 附加属性键值对（可为 null）
     * @return 认证成功的结果
     * @throws NullPointerException 如果 principal 为 null
     */
    public static AuthenticationResult success(String principal, Map<String, String> attributes) {
        return new AuthenticationResult(true, principal, attributes, null);
    }
    
    /**
     * 创建认证失败的结果。
     *
     * @param errorMessage 错误消息
     * @return 认证失败的结果
     * @throws NullPointerException 如果 errorMessage 为 null
     */
    public static AuthenticationResult failure(String errorMessage) {
        return new AuthenticationResult(false, null, null, errorMessage);
    }
    
    /**
     * 检查认证是否成功。
     *
     * @return true 如果认证成功，否则 false
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * 获取认证主体标识。
     *
     * @return 主体标识（认证成功时返回，失败时为 null）
     */
    public String getPrincipal() {
        return principal;
    }
    
    /**
     * 获取附加属性（不可修改的视图）。
     *
     * @return 附加属性键值对，不会为 null
     */
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
    
    /**
     * 获取指定键的附加属性值。
     *
     * @param key 属性键
     * @return 属性值，如果不存在则返回 null
     */
    public String getAttribute(String key) {
        return attributes.get(key);
    }
    
    /**
     * 获取认证失败的错误消息。
     *
     * @return 错误消息（认证成功时为 null）
     */
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