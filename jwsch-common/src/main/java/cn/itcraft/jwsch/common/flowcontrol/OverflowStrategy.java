package cn.itcraft.jwsch.common.flowcontrol;

/**
 * 溢出策略枚举。
 *
 * <p>定义当出站队列满时的处理策略：
 * <ul>
 *   <li>DROP_OLDEST: 丢弃最旧消息，保证新消息优先</li>
 *   <li>DROP_NEWEST: 丢弃新消息，保证已入队消息投递</li>
 *   <li>DISCONNECT: 断开慢消费者连接</li>
 *   <li>DROP_OLDEST_THEN_DISCONNECT: 先丢弃旧消息，超极限阈值后断开</li>
 * </ul>
 */
public enum OverflowStrategy {
    
    DROP_OLDEST,
    DROP_NEWEST,
    DISCONNECT,
    DROP_OLDEST_THEN_DISCONNECT
}
