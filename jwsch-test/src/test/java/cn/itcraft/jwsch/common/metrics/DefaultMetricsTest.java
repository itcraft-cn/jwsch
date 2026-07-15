package cn.itcraft.jwsch.common.metrics;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DefaultMetricsTest {
    
    private DefaultMetrics metrics;
    
    @Before
    public void setUp() {
        metrics = new DefaultMetrics();
    }
    
    @Test
    public void testIncrement() {
        metrics.increment("test.counter");
        
        Counter counter = metrics.getCounter("test.counter");
        assertEquals(1, counter.get());
    }
    
    @Test
    public void testIncrementWithDelta() {
        metrics.increment("test.counter", 10);
        
        Counter counter = metrics.getCounter("test.counter");
        assertEquals(10, counter.get());
    }
    
    @Test
    public void testDecrement() {
        metrics.increment("test.counter", 10);
        metrics.decrement("test.counter");
        
        Counter counter = metrics.getCounter("test.counter");
        assertEquals(9, counter.get());
    }
    
    @Test
    public void testRecord() {
        metrics.record("test.gauge", 100);
        
        Gauge gauge = metrics.getGauge("test.gauge");
        assertEquals(100, gauge.get());
    }
    
    @Test
    public void testGetCounter() {
        Counter counter1 = metrics.getCounter("test.counter");
        Counter counter2 = metrics.getCounter("test.counter");
        
        assertSame(counter1, counter2);
    }
    
    @Test
    public void testGetGauge() {
        Gauge gauge1 = metrics.getGauge("test.gauge");
        Gauge gauge2 = metrics.getGauge("test.gauge");
        
        assertSame(gauge1, gauge2);
    }
    
    @Test
    public void testRemove() {
        metrics.increment("test.counter");
        metrics.record("test.gauge", 100);
        
        metrics.remove("test.counter");
        metrics.remove("test.gauge");
        
        Counter newCounter = metrics.getCounter("test.counter");
        Gauge newGauge = metrics.getGauge("test.gauge");
        
        assertEquals(0, newCounter.get());
        assertEquals(0, newGauge.get());
    }
    
    @Test
    public void testClear() {
        metrics.increment("test.counter1");
        metrics.increment("test.counter2");
        metrics.record("test.gauge1", 100);
        
        metrics.clear();
        
        assertEquals(0, metrics.getCounter("test.counter1").get());
        assertEquals(0, metrics.getCounter("test.counter2").get());
        assertEquals(0, metrics.getGauge("test.gauge1").get());
    }
    
    @Test
    public void testCounterGetAndReset() {
        metrics.increment("test.counter", 100);
        
        Counter counter = metrics.getCounter("test.counter");
        assertEquals(100, counter.get());
        
        counter.reset();
        assertEquals(0, counter.get());
    }
    
    @Test
    public void testCounterDecrementWithDelta() {
        metrics.increment("test.counter", 100);
        
        Counter counter = metrics.getCounter("test.counter");
        counter.decrement(30);
        
        assertEquals(70, counter.get());
    }
    
    @Test
    public void testGaugeIncrementDecrement() {
        metrics.record("test.gauge", 50);
        
        Gauge gauge = metrics.getGauge("test.gauge");
        assertEquals(50, gauge.get());
        
        gauge.increment();
        assertEquals(51, gauge.get());
        
        gauge.decrement();
        assertEquals(50, gauge.get());
    }
    
    @Test
    public void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int iterations = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        metrics.increment("concurrent.counter");
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertEquals(0, errors.get());
        assertEquals(threadCount * iterations, metrics.getCounter("concurrent.counter").get());
    }
}