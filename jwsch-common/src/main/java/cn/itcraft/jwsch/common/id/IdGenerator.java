package cn.itcraft.jwsch.common.id;

import com.google.common.hash.Hashing;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

public final class IdGenerator {
    
    private static final int DEFAULT_SEED = 0x1234ABCD;
    private static final AtomicLong COUNTER = new AtomicLong(System.currentTimeMillis());
    
    private final int seed;
    
    public IdGenerator() {
        this(DEFAULT_SEED);
    }
    
    public IdGenerator(int seed) {
        this.seed = seed;
    }
    
    public static long nextId() {
        return COUNTER.incrementAndGet();
    }
    
    public static long nextId(String prefix) {
        String input = prefix + "-" + COUNTER.incrementAndGet();
        return Hashing.murmur3_128(DEFAULT_SEED)
            .hashString(input, StandardCharsets.UTF_8)
            .asLong();
    }
    
    public long generateId(String input) {
        return Hashing.murmur3_128(seed)
            .hashString(input, StandardCharsets.UTF_8)
            .asLong();
    }
    
    public long generateFrontendId(String ip, int port) {
        String input = formatAddress(ip, port);
        return generateId(input);
    }
    
    public long generateBackendId(String ip, int port) {
        String input = formatAddress(ip, port);
        return generateId(input);
    }
    
    public long generateNodeId(String prefix, String hostname) {
        String input = prefix + "-" + hostname;
        return generateId(input);
    }
    
    private String formatAddress(String ip, int port) {
        if (ip.contains(":")) {
            return "[" + ip + "]:" + port;
        }
        return ip + ":" + port;
    }
}