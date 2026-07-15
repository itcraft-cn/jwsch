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

public class ResponseMapping {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseMapping.class);
    
    private static final int DEFAULT_TIMEOUT_MS = 30000;
    
    private final ConcurrentHashMap<Integer, CompletableFuture<Packet>> pendingRequests;
    private final AtomicInteger requestIdGenerator;
    private final ScheduledExecutorService scheduler;
    private final int timeoutMs;
    
    public ResponseMapping() {
        this(DEFAULT_TIMEOUT_MS);
    }
    
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
    
    public int generateRequestId() {
        return requestIdGenerator.incrementAndGet();
    }
    
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
    
    public boolean completeResponse(int requestId, Packet response) {
        CompletableFuture<Packet> future = pendingRequests.remove(requestId);
        if (future != null) {
            return future.complete(response);
        }
        LOGGER.warn("Response received for unknown request: requestId={}", requestId);
        return false;
    }
    
    public boolean completeExceptionally(int requestId, Throwable ex) {
        CompletableFuture<Packet> future = pendingRequests.remove(requestId);
        if (future != null) {
            return future.completeExceptionally(ex);
        }
        return false;
    }
    
    public void removeFuture(int requestId) {
        pendingRequests.remove(requestId);
    }
    
    public int getPendingCount() {
        return pendingRequests.size();
    }
    
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