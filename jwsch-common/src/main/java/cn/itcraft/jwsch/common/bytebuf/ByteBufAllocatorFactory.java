/**
 * Factory for creating Netty ByteBufAllocator instances.
 *
 * <p>Creates pooled or unpooled allocators based on configuration:
 * <ul>
 *   <li>PooledByteBufAllocator: Better performance for high-throughput scenarios</li>
 *   <li>UnpooledByteBufAllocator: Simpler memory management, no pooling overhead</li>
 * </ul>
 *
 * <p>Supports direct (off-heap) or heap buffers:
 * <ul>
 *   <li>Direct buffers: Zero-copy for network I/O, managed by JVM</li>
 *   <li>Heap buffers: On-heap Java byte arrays</li>
 * </ul>
 */
public final class ByteBufAllocatorFactory {
    
    private final ByteBufConfig config;
    
    /**
     * Creates a factory with the specified configuration.
     *
     * @param config ByteBuf configuration
     * @throws NullPointerException if config is null
     */
    public ByteBufAllocatorFactory(ByteBufConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
    }
    
    /**
     * Creates a ByteBufAllocator based on configuration.
     *
     * @return configured ByteBufAllocator instance
     */
    public ByteBufAllocator create() {
        if (config.isPooled()) {
            return new PooledByteBufAllocator(config.isDirect());
        } else {
            return new UnpooledByteBufAllocator(config.isDirect());
        }
    }
}