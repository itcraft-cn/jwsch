package cn.itcraft.jwsch.common.metrics;

public interface Gauge {
    
    void set(long value);
    
    long get();
    
    void increment();
    
    void decrement();
}