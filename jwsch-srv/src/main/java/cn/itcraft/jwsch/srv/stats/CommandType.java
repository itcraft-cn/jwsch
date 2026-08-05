package cn.itcraft.jwsch.srv.stats;

import cn.itcraft.jwsch.common.protocol.Command;

/**
 * 命令类型枚举，用于统计分类。
 * 
 * <p>将协议命令映射到统计类别，便于按命令类型进行消息计数和流量统计。
 * 
 * <p>支持的命令类型包括：
 * <ul>
 *   <li>REQUEST - 请求命令</li>
 *   <li>RESPONSE - 响应命令</li>
 *   <li>PUSH - 推送命令</li>
 *   <li>BROADCAST - 广播命令</li>
 *   <li>OTHER - 其他命令</li>
 * </ul>
 */
public enum CommandType {
    
    /** 请求命令 */
    REQUEST,
    
    /** 响应命令 */
    RESPONSE,
    
    /** 推送命令 */
    PUSH,
    
    /** 广播命令 */
    BROADCAST,
    
    /** 其他命令 */
    OTHER;
    
    /**
     * 根据命令字节获取命令类型。
     * 
     * @param command 命令字节，参见 {@link cn.itcraft.jwsch.common.protocol.Command}
     * @return 对应的命令类型枚举值，如果命令未匹配则返回 {@link #OTHER}
     */
    public static CommandType fromCommand(byte command) {
        switch (command) {
            case Command.REQUEST:
                return REQUEST;
            case Command.RESPONSE:
                return RESPONSE;
            case Command.PUSH:
                return PUSH;
            case Command.BROADCAST:
                return BROADCAST;
            default:
                return OTHER;
        }
    }
}