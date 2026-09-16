package cn.itcraft.jwsch.common.protocol;

/**
 * 过大数据包记录器工厂：启动时按配置固化实现，热路径不再有分支。
 */
public final class OversizePacketLoggers {

    private static final NoopOversizePacketLogger NOOP = NoopOversizePacketLogger.INSTANCE;
    private static final QueuedOversizePacketLogger QUEUED = QueuedOversizePacketLogger.INSTANCE;

    private OversizePacketLoggers() {
    }

    /**
     * 按配置返回惰性记录器实现。
     *
     * @param logContent true = 队列化独立线程打印内容；false = 空实现
     */
    public static OversizePacketLogger create(boolean logContent) {
        return logContent ? (OversizePacketLogger) QUEUED : NOOP;
    }
}
