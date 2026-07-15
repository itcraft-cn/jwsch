package cn.itcraft.jwsch.common.cache;

import java.util.concurrent.TimeUnit;

public final class CacheConfig {
    
    private final long maximumSize;
    private final int initialCapacity;
    private final int concurrencyLevel;
    private final long expireAfterWriteMs;
    private final long expireAfterAccessMs;
    private final long refreshAfterWriteMs;
    private final boolean recordStats;
    
    private CacheConfig(Builder builder) {
        this.maximumSize = builder.maximumSize;
        this.initialCapacity = builder.initialCapacity;
        this.concurrencyLevel = builder.concurrencyLevel;
        this.expireAfterWriteMs = builder.expireAfterWriteMs;
        this.expireAfterAccessMs = builder.expireAfterAccessMs;
        this.refreshAfterWriteMs = builder.refreshAfterWriteMs;
        this.recordStats = builder.recordStats;
    }
    
    public long getMaximumSize() {
        return maximumSize;
    }
    
    public int getInitialCapacity() {
        return initialCapacity;
    }
    
    public int getConcurrencyLevel() {
        return concurrencyLevel;
    }
    
    public long getExpireAfterWriteMs() {
        return expireAfterWriteMs;
    }
    
    public long getExpireAfterAccessMs() {
        return expireAfterAccessMs;
    }
    
    public long getRefreshAfterWriteMs() {
        return refreshAfterWriteMs;
    }
    
    public boolean isRecordStats() {
        return recordStats;
    }
    
    public static final class Builder {
        private long maximumSize = Long.MAX_VALUE;
        private int initialCapacity = 16;
        private int concurrencyLevel = 16;
        private long expireAfterWriteMs = -1;
        private long expireAfterAccessMs = -1;
        private long refreshAfterWriteMs = -1;
        private boolean recordStats = false;
        
        public Builder maximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
            return this;
        }
        
        public Builder initialCapacity(int initialCapacity) {
            this.initialCapacity = initialCapacity;
            return this;
        }
        
        public Builder concurrencyLevel(int concurrencyLevel) {
            this.concurrencyLevel = concurrencyLevel;
            return this;
        }
        
        public Builder expireAfterWrite(long duration, TimeUnit unit) {
            this.expireAfterWriteMs = unit.toMillis(duration);
            return this;
        }
        
        public Builder expireAfterAccess(long duration, TimeUnit unit) {
            this.expireAfterAccessMs = unit.toMillis(duration);
            return this;
        }
        
        public Builder refreshAfterWrite(long duration, TimeUnit unit) {
            this.refreshAfterWriteMs = unit.toMillis(duration);
            return this;
        }
        
        public Builder recordStats(boolean recordStats) {
            this.recordStats = recordStats;
            return this;
        }
        
        public CacheConfig build() {
            return new CacheConfig(this);
        }
    }
}