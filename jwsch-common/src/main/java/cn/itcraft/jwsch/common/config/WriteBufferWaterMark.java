package cn.itcraft.jwsch.common.config;

public final class WriteBufferWaterMark {
    
    private final int low;
    private final int high;
    
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
    
    public int getLow() {
        return low;
    }
    
    public int getHigh() {
        return high;
    }
}