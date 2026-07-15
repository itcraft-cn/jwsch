package cn.itcraft.jwsch.common.metrics;

public interface Counter {
    
    void increment();
    
    void increment(long delta);
    
    void decrement();
    
    void decrement(long delta);
    
    long get();
    
    void reset();
}