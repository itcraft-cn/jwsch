package cn.itcraft.jwsch.common.cache;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration.
 *
 * <p>Configuration options for cache implementations:
 * <ul>
 *   <li>Size limits (maximum entries)</li>
 *   <li>Initial capacity and concurrency level</li>
 *   <li>Expiration policies (write/access)</li>
 *   <li>Refresh intervals</li>
 *   <li>Statistics recording</li>
 * </ul>
 *
 * <p>Default values:
 * <ul>
 *   <li>maximumSize: Long.MAX_VALUE (unlimited)</li>
 *   <li>initialCapacity: 16</li>
 *   <li>concurrencyLevel: 16</li>
 *   <li>expireAfterWriteMs: -1 (disabled)</li>
 *   <li>expireAfterAccessMs: -1 (disabled)</li>
 *   <li>refreshAfterWriteMs: -1 (disabled)</li>
 *   <li>recordStats: false</li>
 * </ul>
 */
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

    /**
     * Returns maximum cache size (-1 for unlimited).
     */
    public long getMaximumSize() {
        return maximumSize;
    }

    /**
     * Returns initial hash table capacity.
     */
    public int getInitialCapacity() {
        return initialCapacity;
    }

    /**
     * Returns concurrency level for concurrent maps.
     */
    public int getConcurrencyLevel() {
        return concurrencyLevel;
    }

    /**
     * Returns expiration timeout after write (ms, -1 for disabled).
     */
    public long getExpireAfterWriteMs() {
        return expireAfterWriteMs;
    }

    /**
     * Returns expiration timeout after access (ms, -1 for disabled).
     */
    public long getExpireAfterAccessMs() {
        return expireAfterAccessMs;
    }

    /**
     * Returns refresh interval after write (ms, -1 for disabled).
     */
    public long getRefreshAfterWriteMs() {
        return refreshAfterWriteMs;
    }

    /**
     * Returns whether statistics are recorded.
     */
    public boolean isRecordStats() {
        return recordStats;
    }

    /**
     * Builder for CacheConfig.
     */
    public static final class Builder {
        private long maximumSize = Long.MAX_VALUE;
        private int initialCapacity = 16;
        private int concurrencyLevel = 16;
        private long expireAfterWriteMs = -1;
        private long expireAfterAccessMs = -1;
        private long refreshAfterWriteMs = -1;
        private boolean recordStats = false;

        /**
         * Sets maximum cache size.
         *
         * @param maximumSize maximum entries (-1 for unlimited)
         */
        public Builder maximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
            return this;
        }

        /**
         * Sets initial hash table capacity.
         *
         * @param initialCapacity initial capacity
         */
        public Builder initialCapacity(int initialCapacity) {
            this.initialCapacity = initialCapacity;
            return this;
        }

        /**
         * Sets concurrency level for concurrent maps.
         *
         * @param concurrencyLevel concurrency level
         */
        public Builder concurrencyLevel(int concurrencyLevel) {
            this.concurrencyLevel = concurrencyLevel;
            return this;
        }

        /**
         * Sets expiration timeout after write.
         *
         * @param duration time duration
         * @param unit     time unit
         */
        public Builder expireAfterWrite(long duration, TimeUnit unit) {
            this.expireAfterWriteMs = unit.toMillis(duration);
            return this;
        }

        /**
         * Sets expiration timeout after access.
         *
         * @param duration time duration
         * @param unit     time unit
         */
        public Builder expireAfterAccess(long duration, TimeUnit unit) {
            this.expireAfterAccessMs = unit.toMillis(duration);
            return this;
        }

        /**
         * Sets refresh interval after write.
         *
         * @param duration time duration
         * @param unit     time unit
         */
        public Builder refreshAfterWrite(long duration, TimeUnit unit) {
            this.refreshAfterWriteMs = unit.toMillis(duration);
            return this;
        }

        /**
         * Sets whether to record statistics.
         *
         * @param recordStats true to record statistics
         */
        public Builder recordStats(boolean recordStats) {
            this.recordStats = recordStats;
            return this;
        }

        /**
         * Builds the CacheConfig.
         */
        public CacheConfig build() {
            return new CacheConfig(this);
        }
    }
}
