package cn.itcraft.jwsch.common.config;

/**
 * Write buffer watermarks for Netty channel configuration.
 * 
 * <p>Controls when {@link io.netty.channel.Channel#isWritable()} returns false:
 * <ul>
 *   <li>When pending bytes >= high: channel becomes unwritable</li>
 *   <li>When pending bytes <= low: channel becomes writable again</li>
 * </ul>
 * 
 * <p>Prevents out-of-memory errors by applying backpressure
 * when the write buffer grows beyond the high watermark.
 */
public final class WriteBufferWaterMark {
    
    private final int low;
    private final int high;
    
    /**
     * Creates a WriteBufferWaterMark with specified low and high watermarks.
     * 
     * @param low  low watermark in bytes (must be >= 0)
     * @param high high watermark in bytes (must be >= low)
     * @throws IllegalArgumentException if low < 0 or high < low
     */
    public WriteBufferWaterMark(int low, int high) {
        if (low < 0) {
            throw new IllegalArgumentException("low must be >= 0");
        }
        if (high < low) {
            throw new IllegalArgumentException("high must be >= low");
        }
        this.low = low;
        this.high = high;
    }
    
    /**
     * Returns the low watermark in bytes.
     */
    public int getLow() {
        return low;
    }
    
    /**
     * Returns the high watermark in bytes.
     */
    public int getHigh() {
        return high;
    }
}