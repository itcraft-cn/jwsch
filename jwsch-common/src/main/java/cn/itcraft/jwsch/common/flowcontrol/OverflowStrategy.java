package cn.itcraft.jwsch.common.flowcontrol;

/**
 * Overflow strategy enumeration.
 *
 * <p>Defines handling strategies when outbound queue is full:
 * <ul>
 *   <li>DROP_OLDEST: Drop oldest messages, prioritize new messages</li>
 *   <li>DROP_NEWEST: Drop new messages, ensure delivery of queued messages</li>
 *   <li>DISCONNECT: Disconnect slow consumer</li>
 *   <li>DROP_OLDEST_THEN_DISCONNECT: Drop oldest first, disconnect after threshold</li>
 * </ul>
 */
public enum OverflowStrategy {
    
    DROP_OLDEST,
    DROP_NEWEST,
    DISCONNECT,
    DROP_OLDEST_THEN_DISCONNECT
}
