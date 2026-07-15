package cn.itcraft.jwsch.srv.stats;

import cn.itcraft.jwsch.common.protocol.Command;

/**
 * 命令类型枚举，用于统计分类。
 * 
 * <p>将协议命令映射到统计类别。
 */
public enum CommandType {
    
    REQUEST,
    RESPONSE,
    PUSH,
    BROADCAST,
    OTHER;
    
    /**
     * 根据命令字节获取命令类型。
     * 
     * @param command 命令字节
     * @return 命令类型
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