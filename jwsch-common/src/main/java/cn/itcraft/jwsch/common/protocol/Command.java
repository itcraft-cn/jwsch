package cn.itcraft.jwsch.common.protocol;

/**
 * Command definitions for jwsch protocol.
 * 
 * <p>Commands are categorized into two ranges:
 * <ul>
 *   <li>0x01-0x08: Client-Server commands (REQUEST, RESPONSE, PUSH, etc.)</li>
 *   <li>0x10-0x15: Cluster commands (JOIN, MEMBERSHIP, SYNC, etc.)</li>
 * </ul>
 */
public final class Command {
    
    public static final byte REQUEST = 0x01;
    
    public static final byte RESPONSE = 0x02;
    
    public static final byte PUSH = 0x03;
    
    public static final byte BROADCAST = 0x04;
    
    public static final byte SUBSCRIBE = 0x05;
    
    public static final byte HEARTBEAT = 0x06;
    
    public static final byte ACK = 0x07;
    
    public static final byte CONNECT_RESPONSE = 0x08;
    
    public static final byte CLUSTER_JOIN = 0x10;
    
    public static final byte CLUSTER_MEMBERSHIP = 0x11;
    
    public static final byte CLUSTER_SYNC = 0x12;
    
    public static final byte CLUSTER_FORWARD = 0x13;
    
    public static final byte CLUSTER_BROADCAST = 0x14;
    
    public static final byte CLUSTER_HEARTBEAT = 0x15;
    
    private Command() {
    }
    
    public static boolean isValid(byte command) {
        return (command >= REQUEST && command <= CONNECT_RESPONSE) 
            || (command >= CLUSTER_JOIN && command <= CLUSTER_HEARTBEAT);
    }
    
    public static boolean isClusterCommand(byte command) {
        return command >= CLUSTER_JOIN && command <= CLUSTER_HEARTBEAT;
    }
}
