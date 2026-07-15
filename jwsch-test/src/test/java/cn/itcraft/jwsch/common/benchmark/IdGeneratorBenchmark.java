package cn.itcraft.jwsch.common.benchmark;

import cn.itcraft.jwsch.common.id.IdGenerator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class IdGeneratorBenchmark {

    private IdGenerator idGenerator;
    private int counter;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(IdGeneratorBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(5)
                .measurementIterations(10)
                .warmupTime(TimeValue.milliseconds(100))
                .measurementTime(TimeValue.milliseconds(100))
                .build();

        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        idGenerator = new IdGenerator();
        counter = 0;
    }

    @Benchmark
    public long benchmarkNextId() {
        return IdGenerator.nextId();
    }

    @Benchmark
    public long benchmarkGenerateFrontendId() {
        counter++;
        return idGenerator.generateFrontendId("192.168.1.100", 8080 + counter % 10000);
    }

    @Benchmark
    public long benchmarkGenerateBackendId() {
        counter++;
        return idGenerator.generateBackendId("192.168.2.100", 9090 + counter % 10000);
    }

    @Benchmark
    public long benchmarkGenerateNodeId() {
        counter++;
        return idGenerator.generateNodeId("node-" + counter % 1000, "host-" + counter % 100);
    }
}
