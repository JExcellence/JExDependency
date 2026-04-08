package de.jexcellence.oneblock.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Centralized Cache Manager
 * 
 * Provides a unified caching system for all OneBlock components with
 * intelligent cache management, automatic cleanup, and performance monitoring.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class CacheManager {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(2);
    private final CacheStatistics globalStats = new CacheStatistics();
    
    private static CacheManager instance;
    
    private CacheManager() {
        // Start periodic cleanup
        cleanupExecutor.scheduleAtFixedRate(this::performCleanup, 5, 5, TimeUnit.MINUTES);
        cleanupExecutor.scheduleAtFixedRate(this::updateStatistics, 1, 1, TimeUnit.MINUTES);
    }
    
    /**
     * Gets the singleton instance of CacheManager
     */
    @NotNull
    public static synchronized CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }
    
    /**
     * Creates or gets a cache with the specified configuration
     * 
     * @param name the cache name
     * @param config the cache configuration
     * @return the cache instance
     */
    @NotNull
    public <K, V> Cache<K, V> getOrCreateCache(@NotNull String name, @NotNull CacheConfig config) {
        @SuppressWarnings("unchecked")
        Cache<K, V> cache = (Cache<K, V>) caches.get(name);
        
        if (cache == null) {
            cache = new CacheImpl<>(name, config);
            caches.put(name, cache);
            LOGGER.info("Created cache: " + name + " with config: " + config);
        }
        
        return cache;
    }
    
    /**
     * Gets an existing cache by name
     * 
     * @param name the cache name
     * @return the cache instance or null if not found
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(@NotNull String name) {
        return (Cache<K, V>) caches.get(name);
    }
    
    /**
     * Removes a cache
     * 
     * @param name the cache name
     */
    public void removeCache(@NotNull String name) {
        Cache<?, ?> cache = caches.remove(name);
        if (cache != null) {
            cache.clear();
            LOGGER.info("Removed cache: " + name);
        }
    }
    
    /**
     * Clears all caches
     */
    public void clearAllCaches() {
        for (Cache<?, ?> cache : caches.values()) {
            cache.clear();
        }
        LOGGER.info("Cleared all caches");
    }
    
    /**
     * Gets global cache statistics
     */
    @NotNull
    public CacheStatistics getGlobalStatistics() {
        return globalStats;
    }
    
    /**
     * Gets statistics for all caches
     */
    @NotNull
    public Map<String, CacheStatistics> getAllCacheStatistics() {
        Map<String, CacheStatistics> stats = new HashMap<>();
        for (Map.Entry<String, Cache<?, ?>> entry : caches.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().getStatistics());
        }
        return stats;
    }
    
    /**
     * Performs cleanup on all caches
     */
    public void performCleanup() {
        long startTime = System.currentTimeMillis();
        int totalCleaned = 0;
        
        for (Cache<?, ?> cache : caches.values()) {
            totalCleaned += cache.cleanup();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        LOGGER.fine("Cache cleanup completed in " + duration + "ms, cleaned " + totalCleaned + " entries");
    }
    
    /**
     * Updates global statistics
     */
    private void updateStatistics() {
        globalStats.reset();
        
        for (Cache<?, ?> cache : caches.values()) {
            CacheStatistics cacheStats = cache.getStatistics();
            globalStats.merge(cacheStats);
        }
    }
    
    /**
     * Shuts down the cache manager
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        clearAllCaches();
        LOGGER.info("Cache manager shut down");
    }
    
    /**
     * Cache interface
     */
    public interface Cache<K, V> {
        
        /**
         * Gets a value from the cache
         */
        @Nullable
        V get(@NotNull K key);
        
        /**
         * Gets a value from the cache or computes it if missing
         */
        @NotNull
        V getOrCompute(@NotNull K key, @NotNull Function<K, V> computer);
        
        /**
         * Puts a value in the cache
         */
        void put(@NotNull K key, @NotNull V value);
        
        /**
         * Removes a value from the cache
         */
        void remove(@NotNull K key);
        
        /**
         * Checks if the cache contains a key
         */
        boolean containsKey(@NotNull K key);
        
        /**
         * Gets the cache size
         */
        int size();
        
        /**
         * Clears the cache
         */
        void clear();
        
        /**
         * Performs cleanup and returns number of removed entries
         */
        int cleanup();
        
        /**
         * Gets cache statistics
         */
        @NotNull
        CacheStatistics getStatistics();
        
        /**
         * Gets cache configuration
         */
        @NotNull
        CacheConfig getConfig();
    }
    
    /**
     * Cache configuration
     */
    public static class CacheConfig {
        private final int maxSize;
        private final long expireAfterWrite;
        private final long expireAfterAccess;
        private final boolean recordStats;
        private final EvictionPolicy evictionPolicy;
        
        public enum EvictionPolicy {
            LRU, LFU, FIFO, RANDOM
        }
        
        private CacheConfig(Builder builder) {
            this.maxSize = builder.maxSize;
            this.expireAfterWrite = builder.expireAfterWrite;
            this.expireAfterAccess = builder.expireAfterAccess;
            this.recordStats = builder.recordStats;
            this.evictionPolicy = builder.evictionPolicy;
        }
        
        public int getMaxSize() { return maxSize; }
        public long getExpireAfterWrite() { return expireAfterWrite; }
        public long getExpireAfterAccess() { return expireAfterAccess; }
        public boolean isRecordStats() { return recordStats; }
        public EvictionPolicy getEvictionPolicy() { return evictionPolicy; }
        
        @NotNull
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private int maxSize = 1000;
            private long expireAfterWrite = TimeUnit.HOURS.toMillis(1);
            private long expireAfterAccess = TimeUnit.MINUTES.toMillis(30);
            private boolean recordStats = true;
            private EvictionPolicy evictionPolicy = EvictionPolicy.LRU;
            
            public Builder maxSize(int maxSize) {
                this.maxSize = maxSize;
                return this;
            }
            
            public Builder expireAfterWrite(long duration, TimeUnit unit) {
                this.expireAfterWrite = unit.toMillis(duration);
                return this;
            }
            
            public Builder expireAfterAccess(long duration, TimeUnit unit) {
                this.expireAfterAccess = unit.toMillis(duration);
                return this;
            }
            
            public Builder recordStats(boolean recordStats) {
                this.recordStats = recordStats;
                return this;
            }
            
            public Builder evictionPolicy(EvictionPolicy policy) {
                this.evictionPolicy = policy;
                return this;
            }
            
            public CacheConfig build() {
                return new CacheConfig(this);
            }
        }
        
        @Override
        public String toString() {
            return String.format("CacheConfig{maxSize=%d, expireAfterWrite=%dms, expireAfterAccess=%dms, recordStats=%s, evictionPolicy=%s}",
                maxSize, expireAfterWrite, expireAfterAccess, recordStats, evictionPolicy);
        }
    }
    
    /**
     * Cache statistics
     */
    public static class CacheStatistics {
        private long hitCount = 0;
        private long missCount = 0;
        private long loadCount = 0;
        private long evictionCount = 0;
        private long totalLoadTime = 0;
        private int currentSize = 0;
        
        public void recordHit() { hitCount++; }
        public void recordMiss() { missCount++; }
        public void recordLoad(long loadTime) { 
            loadCount++; 
            totalLoadTime += loadTime;
        }
        public void recordEviction() { evictionCount++; }
        public void setCurrentSize(int size) { currentSize = size; }
        
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public long getLoadCount() { return loadCount; }
        public long getEvictionCount() { return evictionCount; }
        public long getTotalLoadTime() { return totalLoadTime; }
        public int getCurrentSize() { return currentSize; }
        
        public double getHitRate() {
            long total = hitCount + missCount;
            return total == 0 ? 0.0 : (double) hitCount / total;
        }
        
        public double getAverageLoadTime() {
            return loadCount == 0 ? 0.0 : (double) totalLoadTime / loadCount;
        }
        
        public void reset() {
            hitCount = 0;
            missCount = 0;
            loadCount = 0;
            evictionCount = 0;
            totalLoadTime = 0;
            currentSize = 0;
        }
        
        public void merge(CacheStatistics other) {
            hitCount += other.hitCount;
            missCount += other.missCount;
            loadCount += other.loadCount;
            evictionCount += other.evictionCount;
            totalLoadTime += other.totalLoadTime;
            currentSize += other.currentSize;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStatistics{hitRate=%.2f%%, size=%d, evictions=%d, avgLoadTime=%.2fms}",
                getHitRate() * 100, currentSize, evictionCount, getAverageLoadTime());
        }
    }
    
    /**
     * Cache implementation
     */
    private static class CacheImpl<K, V> implements Cache<K, V> {
        private final String name;
        private final CacheConfig config;
        private final Map<K, CacheEntry<V>> storage = new ConcurrentHashMap<>();
        private final CacheStatistics statistics = new CacheStatistics();
        
        public CacheImpl(String name, CacheConfig config) {
            this.name = name;
            this.config = config;
        }
        
        @Override
        @Nullable
        public V get(@NotNull K key) {
            CacheEntry<V> entry = storage.get(key);
            
            if (entry == null) {
                if (config.isRecordStats()) {
                    statistics.recordMiss();
                }
                return null;
            }
            
            long currentTime = System.currentTimeMillis();
            
            // Check expiration
            if (isExpired(entry, currentTime)) {
                storage.remove(key);
                if (config.isRecordStats()) {
                    statistics.recordMiss();
                    statistics.recordEviction();
                }
                return null;
            }
            
            // Update access time
            entry.setLastAccessed(currentTime);
            entry.incrementAccessCount();
            
            if (config.isRecordStats()) {
                statistics.recordHit();
            }
            
            return entry.getValue();
        }
        
        @Override
        @NotNull
        public V getOrCompute(@NotNull K key, @NotNull Function<K, V> computer) {
            V value = get(key);
            if (value != null) {
                return value;
            }
            
            long startTime = System.currentTimeMillis();
            value = computer.apply(key);
            long loadTime = System.currentTimeMillis() - startTime;
            
            if (value != null) {
                put(key, value);
                if (config.isRecordStats()) {
                    statistics.recordLoad(loadTime);
                }
            }
            
            return value;
        }
        
        @Override
        public void put(@NotNull K key, @NotNull V value) {
            long currentTime = System.currentTimeMillis();
            CacheEntry<V> entry = new CacheEntry<>(value, currentTime);
            
            storage.put(key, entry);
            
            // Check size limit and evict if necessary
            if (storage.size() > config.getMaxSize()) {
                evictEntries();
            }
            
            if (config.isRecordStats()) {
                statistics.setCurrentSize(storage.size());
            }
        }
        
        @Override
        public void remove(@NotNull K key) {
            storage.remove(key);
            if (config.isRecordStats()) {
                statistics.setCurrentSize(storage.size());
            }
        }
        
        @Override
        public boolean containsKey(@NotNull K key) {
            CacheEntry<V> entry = storage.get(key);
            if (entry == null) {
                return false;
            }
            
            // Check if expired
            if (isExpired(entry, System.currentTimeMillis())) {
                storage.remove(key);
                return false;
            }
            
            return true;
        }
        
        @Override
        public int size() {
            return storage.size();
        }
        
        @Override
        public void clear() {
            storage.clear();
            if (config.isRecordStats()) {
                statistics.setCurrentSize(0);
            }
        }
        
        @Override
        public int cleanup() {
            long currentTime = System.currentTimeMillis();
            int removedCount = 0;
            
            Iterator<Map.Entry<K, CacheEntry<V>>> iterator = storage.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<K, CacheEntry<V>> entry = iterator.next();
                if (isExpired(entry.getValue(), currentTime)) {
                    iterator.remove();
                    removedCount++;
                }
            }
            
            if (config.isRecordStats() && removedCount > 0) {
                statistics.recordEviction();
                statistics.setCurrentSize(storage.size());
            }
            
            return removedCount;
        }
        
        @Override
        @NotNull
        public CacheStatistics getStatistics() {
            if (config.isRecordStats()) {
                statistics.setCurrentSize(storage.size());
            }
            return statistics;
        }
        
        @Override
        @NotNull
        public CacheConfig getConfig() {
            return config;
        }
        
        private boolean isExpired(CacheEntry<V> entry, long currentTime) {
            // Check write expiration
            if (config.getExpireAfterWrite() > 0 && 
                currentTime - entry.getCreatedTime() > config.getExpireAfterWrite()) {
                return true;
            }
            
            // Check access expiration
            if (config.getExpireAfterAccess() > 0 && 
                currentTime - entry.getLastAccessed() > config.getExpireAfterAccess()) {
                return true;
            }
            
            return false;
        }
        
        private void evictEntries() {
            int targetSize = (int) (config.getMaxSize() * 0.8); // Evict to 80% capacity
            int toEvict = storage.size() - targetSize;
            
            if (toEvict <= 0) return;
            
            List<Map.Entry<K, CacheEntry<V>>> entries = new ArrayList<>(storage.entrySet());
            
            // Sort based on eviction policy
            switch (config.getEvictionPolicy()) {
                case LRU -> entries.sort((a, b) -> Long.compare(a.getValue().getLastAccessed(), b.getValue().getLastAccessed()));
                case LFU -> entries.sort((a, b) -> Long.compare(a.getValue().getAccessCount(), b.getValue().getAccessCount()));
                case FIFO -> entries.sort((a, b) -> Long.compare(a.getValue().getCreatedTime(), b.getValue().getCreatedTime()));
                case RANDOM -> Collections.shuffle(entries);
            }
            
            // Remove entries
            for (int i = 0; i < toEvict && i < entries.size(); i++) {
                storage.remove(entries.get(i).getKey());
                if (config.isRecordStats()) {
                    statistics.recordEviction();
                }
            }
            
            if (config.isRecordStats()) {
                statistics.setCurrentSize(storage.size());
            }
        }
    }
    
    /**
     * Cache entry wrapper
     */
    private static class CacheEntry<V> {
        private final V value;
        private final long createdTime;
        private volatile long lastAccessed;
        private volatile long accessCount = 1;
        
        public CacheEntry(V value, long createdTime) {
            this.value = value;
            this.createdTime = createdTime;
            this.lastAccessed = createdTime;
        }
        
        public V getValue() { return value; }
        public long getCreatedTime() { return createdTime; }
        public long getLastAccessed() { return lastAccessed; }
        public long getAccessCount() { return accessCount; }
        
        public void setLastAccessed(long lastAccessed) { this.lastAccessed = lastAccessed; }
        public void incrementAccessCount() { this.accessCount++; }
    }
}