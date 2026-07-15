package cn.itcraft.jwsch.common.exception;

public enum ErrorCode {
    
    SUCCESS(0, "Success"),
    
    INVALID_MAGIC(1, "Invalid magic"),
    INVALID_VERSION(2, "Invalid version"),
    INVALID_COMMAND(3, "Invalid command"),
    INVALID_HEADER_LENGTH(4, "Invalid header length"),
    INVALID_BODY_LENGTH(5, "Invalid body length"),
    INVALID_TOPIC_LENGTH(6, "Invalid topic length"),
    RATE_LIMITED(10, "Rate limited"),
    DECODE_FAILED(7, "Decode failed"),
    ENCODE_FAILED(8, "Encode failed"),
    
    CONNECTION_CLOSED(1001, "Connection closed"),
    CONNECTION_TIMEOUT(1002, "Connection timeout"),
    CONNECTION_REFUSED(1003, "Connection refused"),
    CONNECTION_RESET(1004, "Connection reset"),
    TOO_MANY_CONNECTIONS(1005, "Too many connections"),
    HEARTBEAT_TIMEOUT(1006, "Heartbeat timeout"),
    WEBSOCKET_UPGRADE_FAILED(1007, "WebSocket upgrade failed"),
    
    SERVICE_NOT_FOUND(2001, "Service not found"),
    NO_AVAILABLE_INSTANCE(2002, "No available instance"),
    ROUTE_FAILED(2003, "Route failed"),
    LOAD_BALANCE_FAILED(2004, "Load balance failed"),
    REQUEST_TIMEOUT(2005, "Request timeout"),
    RESPONSE_TIMEOUT(2006, "Response timeout"),
    
    REGISTER_FAILED(3001, "Register failed"),
    DEREGISTER_FAILED(3002, "Deregister failed"),
    DISCOVER_FAILED(3003, "Discover failed"),
    SUBSCRIBE_FAILED(3004, "Subscribe failed"),
    
    INTERNAL_ERROR(9001, "Internal error"),
    CONFIG_ERROR(9002, "Config error"),
    OUT_OF_MEMORY(9003, "Out of memory"),
    THREAD_POOL_REJECTED(9004, "Thread pool rejected");
    
    private final short code;
    private final String desc;
    
    ErrorCode(int code, String desc) {
        this.code = (short) code;
        this.desc = desc;
    }
    
    public short getCode() {
        return code;
    }
    
    public String getDesc() {
        return desc;
    }
    
    public static ErrorCode fromCode(short code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return null;
    }
}