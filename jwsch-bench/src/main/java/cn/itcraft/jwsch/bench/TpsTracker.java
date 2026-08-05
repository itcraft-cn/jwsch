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
    
    /**
     * 创建 TPS 统计追踪器实例。
     * 
     * @param name          追踪器名称（如 "PUB" 或 "SUB"）
     * @param instanceCount 实例数量
     */
    public TpsTracker(String name, int instanceCount) {
        this.name = name;
        this.instanceCount = instanceCount;
    }
    
    /**
     * 增加计数器。
     */
    public void increment() {
        counter.increment();
    }
    
    /**
     * 输出 TPS 统计报告。
     * 
     * <p>计算并输出当前的 TPS、总计数和最近时间窗口内的计数增量。
     */
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
    
    /**
     * 获取总计数。
     * 
     * @return 总计数
     */
    public long getTotalCount() {
        return counter.sum();
    }
}