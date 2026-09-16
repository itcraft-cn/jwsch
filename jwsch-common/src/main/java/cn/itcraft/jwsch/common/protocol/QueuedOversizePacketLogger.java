package cn.itcraft.jwsch.common.protocol;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * 队列化过大数据包记录器。
 *
 * <p>丢弃线程仅做入队操作（容量有限，队列满时直接丢弃，防止阻塞热路径），
 * 独立的守护日志线程负责将包的原始字节解析出前 200 字节并打印）；
 * 打印内容固定 200 字节，不可调整。
 */
final class QueuedOversizePacketLogger implements OversizePacketLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueuedOversizePacketLogger.class);

    /**
     * 打印的字节数（固定 200，不可调整）。
     */
    static final int DUMP_BYTES = 200;

    /**
     * 有界队列容量，队列满时直接丢弃记录（不阻塞热路径）。
     */
    private static final int QUEUE_CAPACITY = 64;

    private final ScheduledExecutorService executor;
    private final LongAdder queueFullCount = new LongAdder();

    static QueuedOversizePacketLogger INSTANCE = new QueuedOversizePacketLogger();

    private QueuedOversizePacketLogger() {
        this.executor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "jwsch-oversize-dump");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void onDropped(int packetLength, int limit, long hash, ByteBuf packetBytes) {
        if (packetBytes == null) {
            return;
        }
        try {
            executor.schedule(() -> {
                try {
                    LOGGER.warn("Oversize packet content: hash={}, length={}, limit={}, firstBytes={}",
                        Long.toHexString(hash), packetLength, limit,
                        PacketDecoder.dumpFirstBytes(packetBytes, DUMP_BYTES));
                } finally {
                    packetBytes.release();
                }
            }, 0, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            queueFullCount.increment();
            packetBytes.release();
        }
    }

    /**
     * 队列满被迫丢弃记录的总数。
     */
    long getQueueFullCount() {
        return queueFullCount.sum();
    }
}
