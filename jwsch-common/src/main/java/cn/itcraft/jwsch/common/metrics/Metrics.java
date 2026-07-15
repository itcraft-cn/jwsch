package cn.itcraft.jwsch.common.metrics;

public interface Metrics {
    
    void increment(String name);
    
    void increment(String name, long delta);
    
    void decrement(String name);
    
    void record(String name, long value);
    
    Counter getCounter(String name);
    
    Gauge getGauge(String name);
    
    void remove(String name);
    
    void clear();
}