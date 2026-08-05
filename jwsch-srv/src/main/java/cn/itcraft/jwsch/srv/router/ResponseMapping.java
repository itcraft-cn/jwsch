package cn.itcraft.jwsch.srv.router;

import cn.itcraft.jwsch.common.protocol.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 响应映射管理器。
 * 
 * <p>管理异步请求-响应的映射关系，主要功能包括：
 * <ul>
 *   <li>请求 ID 生成：确保每个请求有唯一标识</li>
 *   <li>异步 Future 管理：创建和跟踪 CompletableFuture</li>
 *   <li>超时处理：自动取消超时的请求</li>
 *   <li>响应完成：将收到的响应匹配到对应的 Future</li>
 * </ul>
 * 
 * <p>使用场景：
 * <pre>
 * // 客户端发送请求
 * int requestId = mapping.generateRequestId();
 * CompletableFuture<Packet> future = mapping.createFuture(requestId);
 * channel.writeAndFlush(requestPacket);
 * 
 * // 服务端处理响应
 * Packet response = processRequest(requestPacket);
 * mapping.completeResponse(requestId, response);
 * 
 * // 客户端等待响应
 * Packet result = future.get(30, TimeUnit.SECONDS);
 * </pre>
 */
public class ResponseMapping {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseMapping.class);
    
    private static final int DEFAULT_TIMEOUT_MS = 30000;
    
    private final ConcurrentHashMap<Integer, CompletableFuture<Packet>> pendingRequests;
    private final AtomicInteger requestIdGenerator;
    private final ScheduledExecutorService scheduler;
    private final int timeoutMs;
    
    /**
     * 默认构造函数，使用默认超时时间（30秒）。
     */
    public ResponseMapping() {
        this(DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 构造函数。
     *
     * @param timeoutMs 请求超时时间（毫秒），如果小于等于 0 则使用默认值
     */
    public ResponseMapping(int timeoutMs) {
        this.pendingRequests = new ConcurrentHashMap<>();
        this.requestIdGenerator = new AtomicInteger(0);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "response-mapping-timeout");
            t.setDaemon(true);
            return t;
        });
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
    }
    
    /**
     * 生成唯一的请求 ID。
     *
     * @return 新的请求 ID
     */
    public int generateRequestId() {
        return requestIdGenerator.incrementAndGet();
    }
    
    /**
     * 为指定请求 ID 创建异步 Future。
     *
     * <p>创建 Future 的同时会设置超时定时器，如果超时则自动取消请求。
     *
     * @param requestId 请求 ID
     * @return 与该请求关联的 CompletableFuture
     */
    public CompletableFuture<Packet> createFuture(int requestId) {
        CompletableFuture<Packet> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        
        scheduler.schedule(() -> {
            CompletableFuture<Packet> removed = pendingRequests.remove(requestId);
            if (removed != null) {
                removed.completeExceptionally(new java.util.concurrent.TimeoutException(
                    "Request timeout: requestId=" + requestId));
                LOGGER.warn("Request timeout: requestId={}", requestId);
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);
        
        return future;
    }
    
    /**
     * 完成指定请求的响应。
     *
     * @param requestId 请求 ID
     * @param response 响应数据包
     * @return 是否成功完成响应（如果请求不存在则返回 false）
     */
    public boolean completeResponse(int requestId, Packet response) {
        CompletableFuture<Packet> future = pendingRequests.remove(requestId);
        if (future != null) {
            return future.complete(response);
        }
        LOGGER.warn("Response received for unknown request: requestId={}", requestId);
        return false;
    }
    
    /**
     * 异常完成指定请求。
     *
     * @param requestId 请求 ID
     * @param ex 异常
     * @return 是否成功完成异常（如果请求不存在则返回 false）
     */
    public boolean completeExceptionally(int requestId, Throwable ex) {
        CompletableFuture<Packet> future = pendingRequests.remove(requestId);
        if (future != null) {
            return future.completeExceptionally(ex);
        }
        return false;
    }
    
    /**
     * 移除指定请求的 Future。
     *
     * @param requestId 请求 ID
     */
    public void removeFuture(int requestId) {
        pendingRequests.remove(requestId);
    }
    
    /**
     * 获取待处理的请求数量。
     *
     * @return 待处理请求数量
     */
    public int getPendingCount() {
        return pendingRequests.size();
    }
    
    /**
     * 关闭响应映射管理器。
     * 
     * <p>取消所有待处理的请求并清理资源。
     */
    public void shutdown() {
        for (Map.Entry<Integer, CompletableFuture<Packet>> entry : pendingRequests.entrySet()) {
            entry.getValue().completeExceptionally(
                new java.util.concurrent.CancellationException("ResponseMapping shutdown"));
        }
        pendingRequests.clear();
        scheduler.shutdown();
        LOGGER.info("ResponseMapping shutdown, cleared {} pending requests", pendingRequests.size());
    }
}