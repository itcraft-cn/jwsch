package cn.itcraft.jwsch.bench;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * TPS 统计追踪器。
 * 
 * <p>使用 LongAdder 实现高并发计数，定时输出 TPS 统计。
 */
public final class TpsTracker {
    
    private final String name;
    private final LongAdder counter = new LongAdder();
    private final AtomicLong lastCount = new AtomicLong(0);
    private final AtomicLong lastTime = new AtomicLong(System.currentTimeMillis());
    private final int instanceCount;
    
    public TpsTracker(String name, int instanceCount) {
        this.name = name;
        this.instanceCount = instanceCount;
    }
    
    public void increment() {
        counter.increment();
    }
    
    public void report() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastTime.get();
        long currentCount = counter.sum();
        long delta = currentCount - lastCount.get();
        double tps = (delta * 1000.0) / elapsed;
        
        String suffix = instanceCount > 1 ? " (" + instanceCount + " " + name.toLowerCase() + "s)" : "";
        System.out.printf("[%s] TPS: %.2f | Total: %d | Last %ds: %d%s%n",
            name, tps, currentCount, (int)(elapsed/1000), delta, suffix);
        
        lastCount.set(currentCount);
        lastTime.set(now);
    }
    
    public long getTotalCount() {
        return counter.sum();
    }
}