package cn.itcraft.jwsch.bench.latency;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 延迟统计器。
 * 
 * <p>收集延迟样本，计算 avg/min/max/p99/p95/p90/p50。
 * 使用预分配 long[] 替代 CopyOnWriteArrayList，避免每次 record 触发数组拷贝。
 * 样本数超过容量时自动扩容（低频操作，仅在测试初始化阶段）。
 */
public final class LatencyTracker {

    private static final int INITIAL_CAPACITY = 65536;

    private volatile long[] samples;
    private final AtomicLong writeIndex = new AtomicLong(0);
    private final LongAdder sum = new LongAdder();
    private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);

    public LatencyTracker() {
        samples = new long[INITIAL_CAPACITY];
    }

    public void record(long latencyNanos) {
        long idx = writeIndex.getAndIncrement();

        int slot = (int) idx;
        if (slot >= samples.length) {
            grow(slot + 1);
        }

        samples[slot] = latencyNanos;

        sum.add(latencyNanos);

        long currentMin = min.get();
        while (latencyNanos < currentMin) {
            if (min.compareAndSet(currentMin, latencyNanos)) {
                break;
            }
            currentMin = min.get();
        }

        long currentMax = max.get();
        while (latencyNanos > currentMax) {
            if (max.compareAndSet(currentMax, latencyNanos)) {
                break;
            }
            currentMax = max.get();
        }
    }

    private synchronized void grow(int requiredCapacity) {
        if (requiredCapacity <= samples.length) {
            return;
        }
        int newCapacity = Math.max(requiredCapacity, samples.length * 2);
        samples = Arrays.copyOf(samples, newCapacity);
    }

    public void reportFinal() {
        long count = writeIndex.get();
        if (count == 0) {
            System.out.println("[LAT] No samples collected");
            return;
        }

        long avgNanos = sum.sum() / count;

        double avgMicros = avgNanos / 1000.0;
        double minMicros = min.get() / 1000.0;
        double maxMicros = max.get() / 1000.0;

        long[] snapshot = new long[(int) Math.min(count, Integer.MAX_VALUE)];
        System.arraycopy(samples, 0, snapshot, 0, snapshot.length);
        Arrays.sort(snapshot);

        double p99Micros = getPercentile(snapshot, 99) / 1000.0;
        double p95Micros = getPercentile(snapshot, 95) / 1000.0;
        double p90Micros = getPercentile(snapshot, 90) / 1000.0;
        double p50Micros = getPercentile(snapshot, 50) / 1000.0;

        System.out.println();
        System.out.println("=== Latency Statistics ===");
        System.out.println("Total samples: " + count);
        System.out.println("Avg: " + String.format("%.2f", avgMicros) + " μs");
        System.out.println("Min: " + String.format("%.2f", minMicros) + " μs");
        System.out.println("Max: " + String.format("%.2f", maxMicros) + " μs");
        System.out.println("P50: " + String.format("%.2f", p50Micros) + " μs");
        System.out.println("P90: " + String.format("%.2f", p90Micros) + " μs");
        System.out.println("P95: " + String.format("%.2f", p95Micros) + " μs");
        System.out.println("P99: " + String.format("%.2f", p99Micros) + " μs");
    }

    private long getPercentile(long[] sorted, int percentile) {
        if (sorted.length == 0) {
            return 0;
        }

        int index = (int) Math.ceil(percentile / 100.0 * sorted.length) - 1;
        index = Math.max(0, Math.min(index, sorted.length - 1));

        return sorted[index];
    }

    public long getCount() {
        return writeIndex.get();
    }

    public long getSum() {
        return sum.sum();
    }

    public long getMin() {
        long v = min.get();
        return v == Long.MAX_VALUE ? 0 : v;
    }

    public long getMax() {
        long v = max.get();
        return v == Long.MIN_VALUE ? 0 : v;
    }
}
