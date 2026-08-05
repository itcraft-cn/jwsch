/**
 * ID generation utility using MurmurHash3 algorithm.
 *
 * <p>Generates unique 64-bit identifiers for various entities:
 * <ul>
 *   <li>Connection IDs</li>
 *   <li>Node IDs (frontend/backend)</li>
 *   <li>Message IDs</li>
 * </ul>
 *
 * <p>Features:
 * <ul>
 *   <li>Uses MurmurHash3-128 for good distribution</li>
 *   <li>Thread-safe atomic counter for uniqueness</li>
 *   <li>Seed-based initialization for reproducibility</li>
 * </ul>
 */
public final class IdGenerator {
    
    private static final int DEFAULT_SEED = 0x1234ABCD;
    private static final AtomicLong COUNTER = new AtomicLong(System.currentTimeMillis());
    
    private final int seed;
    
    /**
     * Creates an IdGenerator with default seed.
     */
    public IdGenerator() {
        this(DEFAULT_SEED);
    }
    
    /**
     * Creates an IdGenerator with custom seed.
     *
     * @param seed seed for hash function
     */
    public IdGenerator(int seed) {
        this.seed = seed;
    }
    
    /**
     * Generates a simple sequential ID.
     * 
     * <p>Uses atomic counter starting from current timestamp.
     * 
     * @return unique 64-bit ID
     */
    public static long nextId() {
        return COUNTER.incrementAndGet();
    }
    
    /**
     * Generates a hashed ID with prefix.
     * 
     * <p>Combines prefix with atomic counter and applies MurmurHash3.
     * 
     * @param prefix string prefix (e.g., "conn", "msg")
     * @return hashed 64-bit ID
     */
    public static long nextId(String prefix) {
        String input = prefix + "-" + COUNTER.incrementAndGet();
        return Hashing.murmur3_128(DEFAULT_SEED)
            .hashString(input, StandardCharsets.UTF_8)
            .asLong();
    }
    
    /**
     * Generates a hashed ID from input string.
     *
     * @param input string to hash
     * @return hashed 64-bit ID
     */
    public long generateId(String input) {
        return Hashing.murmur3_128(seed)
            .hashString(input, StandardCharsets.UTF_8)
            .asLong();
    }
    
    /**
     * Generates a frontend node ID from IP and port.
     *
     * @param ip frontend IP address
     * @param port frontend port
     * @return node ID
     */
    public long generateFrontendId(String ip, int port) {
        String input = formatAddress(ip, port);
        return generateId(input);
    }
    
    /**
     * Generates a backend node ID from IP and port.
     *
     * @param ip backend IP address
     * @param port backend port
     * @return node ID
     */
    public long generateBackendId(String ip, int port) {
        String input = formatAddress(ip, port);
        return generateId(input);
    }
    
    /**
     * Generates a node ID with prefix and hostname.
     *
     * @param prefix node type prefix (e.g., "worker", "gateway")
     * @param hostname hostname
     * @return node ID
     */
    public long generateNodeId(String prefix, String hostname) {
        String input = prefix + "-" + hostname;
        return generateId(input);
    }
    
    /**
     * Formats IP address and port for hashing.
     * Handles IPv6 addresses with brackets.
     */
    private String formatAddress(String ip, int port) {
        if (ip.contains(":")) {
            return "[" + ip + "]:" + port;
        }
        return ip + ":" + port;
    }
}