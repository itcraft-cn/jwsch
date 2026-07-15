package cn.itcraft.jwsch.common.benchmark;

import cn.itcraft.jwsch.common.cache.Cache;
import cn.itcraft.jwsch.common.cache.CacheConfig;
import cn.itcraft.jwsch.common.cache.ConcurrentHashMapCache;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class ConcurrentHashMapCacheBenchmark {

    private Cache<String, String> cache;
    private AtomicInteger keyCounter;

    @Param({"10000", "100000", "1000000"})
    private int cacheSize;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ConcurrentHashMapCacheBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(5)
                .measurementIterations(10)
                .warmupTime(TimeValue.milliseconds(100))
                .measurementTime(TimeValue.milliseconds(100))
                .threads(4)
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Trial)
    public void setup() {
        CacheConfig config = new CacheConfig.Builder()
                .maximumSize(cacheSize)
                .initialCapacity(cacheSize / 10)
                .concurrencyLevel(16)
                .build();

        cache = new ConcurrentHashMapCache<>(config);
        keyCounter = new AtomicInteger(0);

        for (int i = 0; i < cacheSize / 2; i++) {
            cache.put("key-" + i, "value-" + i);
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        cache.clear();
    }

    @Benchmark
    public String benchmarkGet_singleThread() {
        int key = keyCounter.incrementAndGet() % (cacheSize / 2);
        return cache.get("key-" + key);
    }

    @Benchmark
    public void benchmarkPut_singleThread() {
        int key = keyCounter.incrementAndGet();
        cache.put("key-" + key, "value-" + key);
    }

    @Benchmark
    public String benchmarkGet_multiThread() {
        int key = keyCounter.incrementAndGet() % (cacheSize / 2);
        return cache.get("key-" + key);
    }

    @Benchmark
    public void benchmarkPut_multiThread() {
        int key = keyCounter.incrementAndGet();
        cache.put("key-" + key, "value-" + key);
    }

    @Benchmark
    public String benchmarkMixed_multiThread() {
        int key = keyCounter.incrementAndGet();
        if (key % 2 == 0) {
            return cache.get("key-" + (key % (cacheSize / 2)));
        } else {
            cache.put("key-" + key, "value-" + key);
            return null;
        }
    }
}
