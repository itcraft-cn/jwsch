package cn.itcraft.jwsch.srv.benchmark;

import cn.itcraft.jwsch.srv.loadbalance.LoadBalance;
import cn.itcraft.jwsch.srv.loadbalance.RandomLoadBalance;
import cn.itcraft.jwsch.srv.loadbalance.RoundRobinLoadBalance;
import cn.itcraft.jwsch.srv.loadbalance.WeightedRoundRobinLoadBalance;
import cn.itcraft.jwsch.srv.registry.ServiceInstance;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class LoadBalanceBenchmark {

    private LoadBalance randomLoadBalance;
    private LoadBalance roundRobinLoadBalance;
    private LoadBalance weightedRoundRobinLoadBalance;

    private List<ServiceInstance> instances;
    private List<ServiceInstance> weightedInstances;

    @Param({"10", "100", "1000"})
    private int instanceCount;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(LoadBalanceBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(5)
                .measurementIterations(10)
                .warmupTime(TimeValue.milliseconds(100))
                .measurementTime(TimeValue.milliseconds(100))
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Trial)
    public void setup() {
        randomLoadBalance = new RandomLoadBalance();
        roundRobinLoadBalance = new RoundRobinLoadBalance();
        weightedRoundRobinLoadBalance = new WeightedRoundRobinLoadBalance();

        instances = new ArrayList<>();
        for (int i = 0; i < instanceCount; i++) {
            instances.add(new ServiceInstance("test-service", "192.168.1." + (i % 256), 8080 + i));
        }

        weightedInstances = new ArrayList<>();
        for (int i = 0; i < instanceCount; i++) {
            int weight = (i % 10) + 1;
            weightedInstances.add(new ServiceInstance("test-service", "192.168.1." + (i % 256), 8080 + i, weight));
        }
    }

    @Benchmark
    public ServiceInstance benchmarkRandomLoadBalance() {
        return randomLoadBalance.select(instances);
    }

    @Benchmark
    public ServiceInstance benchmarkRoundRobinLoadBalance() {
        return roundRobinLoadBalance.select(instances);
    }

    @Benchmark
    public ServiceInstance benchmarkWeightedRoundRobinLoadBalance() {
        return weightedRoundRobinLoadBalance.select(weightedInstances);
    }
}
