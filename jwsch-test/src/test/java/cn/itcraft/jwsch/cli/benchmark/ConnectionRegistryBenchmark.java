package cn.itcraft.jwsch.cli.benchmark;

import cn.itcraft.jwsch.cli.connection.ConnectionInfo;
import cn.itcraft.jwsch.cli.connection.ConnectionRegistry;
import cn.itcraft.jwsch.cli.connection.ConnectionType;
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
import java.util.concurrent.atomic.AtomicLong;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class ConnectionRegistryBenchmark {

    private ConnectionRegistry registry;
    private AtomicLong idCounter;

    @Param({"1000", "10000", "100000"})
    private int initialConnections;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ConnectionRegistryBenchmark.class.getSimpleName())
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
        registry = new ConnectionRegistry();
        idCounter = new AtomicLong(0);

        for (int i = 0; i < initialConnections; i++) {
            ConnectionInfo info = new ConnectionInfo.Builder()
                    .connectionId(i)
                    .remoteAddress("192.168.1." + (i % 256) + ":" + (8080 + i % 1000))
                    .connectionType(ConnectionType.BACKEND)
                    .status(cn.itcraft.jwsch.cli.connection.ConnectionStatus.ACTIVE)
                    .build();
            registry.register(info);
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        registry.clear();
    }

    @Benchmark
    public void benchmarkRegister() {
        long id = idCounter.incrementAndGet();
        ConnectionInfo info = new ConnectionInfo.Builder()
                .connectionId(id)
                .remoteAddress("192.168.2." + (id % 256) + ":" + (9090 + id % 1000))
                .connectionType(ConnectionType.BACKEND)
                .status(cn.itcraft.jwsch.cli.connection.ConnectionStatus.ACTIVE)
                .build();
        registry.register(info);
    }

    @Benchmark
    public void benchmarkUnregister() {
        long id = (idCounter.incrementAndGet()) % initialConnections;
        registry.unregister(id);
    }

    @Benchmark
    public Object benchmarkLookup() {
        long id = (idCounter.incrementAndGet()) % initialConnections;
        return registry.lookup(id);
    }

    @Benchmark
    public Object benchmarkLookupByRemoteAddress() {
        int index = (int) (idCounter.incrementAndGet() % 256);
        return registry.lookupByRemoteAddress("192.168.1." + index + ":8080");
    }

    @Benchmark
    public void benchmarkConcurrent_register() {
        long id = idCounter.incrementAndGet();
        ConnectionInfo info = new ConnectionInfo.Builder()
                .connectionId(id)
                .remoteAddress("192.168.3." + (id % 256) + ":" + (7070 + id % 1000))
                .connectionType(ConnectionType.FRONTEND)
                .status(cn.itcraft.jwsch.cli.connection.ConnectionStatus.ACTIVE)
                .build();
        registry.register(info);
    }

    @Benchmark
    public Object benchmarkConcurrent_lookup() {
        long id = (idCounter.incrementAndGet()) % initialConnections;
        return registry.lookup(id);
    }
}
