package cn.itcraft.jwsch.srv.security;

/**
 * 授权器接口。
 * 
 * <p>定义授权组件的标准接口，负责验证已认证主体是否有权限执行特定操作。
 * 授权器根据主体标识和权限/资源信息返回 {@link AuthorizationResult}，包含是否允许访问以及拒绝原因。
 * 
 * <p>支持两种授权模式：
 * <ul>
 *   <li>基于权限字符串的授权：检查主体是否拥有特定权限</li>
 *   <li>基于资源和操作的授权：检查主体是否对特定资源有特定操作权限</li>
 * </ul>
 */
public interface Authorizer {
    
    /**
     * 基于权限字符串授权。
     *
     * @param principal 主体标识（如用户 ID）
     * @param permission 权限字符串（如 "read.topic"，"publish.message"）
     * @return 授权结果，包含是否允许以及拒绝原因
     * @throws NullPointerException 如果 principal 或 permission 为 null
     */
    AuthorizationResult authorize(String principal, String permission);
    
    /**
     * 基于资源和操作授权。
     *
     * @param principal 主体标识
     * @param resource 资源标识（如 "topic:news"，"queue:orders"）
     * @param action 操作类型（如 "read"，"write"，"delete"）
     * @return 授权结果，包含是否允许以及拒绝原因
     * @throws NullPointerException 如果 principal、resource 或 action 为 null
     */
    AuthorizationResult authorize(String principal, String resource, String action);
}