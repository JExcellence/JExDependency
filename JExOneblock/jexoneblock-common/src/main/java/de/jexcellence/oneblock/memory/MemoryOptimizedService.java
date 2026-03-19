package de.jexcellence.oneblock.memory;

import de.jexcellence.oneblock.cache.CacheManager;
import de.jexcellence.oneblock.memory.poolable.PoolableList;
import de.jexcellence.oneblock.memory.poolable.PoolableStringBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Memory Optimized Service
 * 
 * Provides memory-optimized utilities and integrates memory management
 * with OneBlock services for optimal performance and reduced GC pressure.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class MemoryOptimizedService implements MemoryManager.MemoryCleanupCallback {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final MemoryManager memoryManager;
    private final ObjectPoolManager poolManager;
    
    // Simple operation counters
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
    
    // Object pools for common objects
    private final ObjectPoolManager.ObjectPool<PoolableStringBuilder> stringBuilderPool;
    private final ObjectPoolManager.ObjectPool<PoolableList<String>> stringListPool;
    private final ObjectPoolManager.ObjectPool<PoolableList<Object>> objectListPool;
    
    private static MemoryOptimizedService instance;
    
    private MemoryOptimizedService() {
        this.memoryManager = MemoryManager.getInstance();
        this.poolManager = memoryManager.getObjectPoolManager();
        
        // Create common object pools
        this.stringBuilderPool = poolManager.getOrCreatePool(
            "string_builder", 
            PoolableStringBuilder::new, 
            100
        );
        
        this.stringListPool = poolManager.getOrCreatePool(
            "string_list", 
            PoolableList::new, 
            50
        );
        
        this.objectListPool = poolManager.getOrCreatePool(
            "object_list", 
            PoolableList::new, 
            50
        );
        
        // Register for memory cleanup callbacks
        memoryManager.registerCleanupCallback("memory_optimized_service", this);
        
        LOGGER.info("MemoryOptimizedService initialized with object pooling");
    }
    
    /**
     * Increments a counter
     */
    private void incrementCounter(@NotNull String name) {
        counters.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * Gets the singleton instance
     */
    @NotNull
    public static synchronized MemoryOptimizedService getInstance() {
        if (instance == null) {
            instance = new MemoryOptimizedService();
        }
        return instance;
    }
    
    /**
     * Borrows a poolable StringBuilder
     * 
     * @return poolable StringBuilder
     */
    @NotNull
    public PoolableStringBuilder borrowStringBuilder() {
        PoolableStringBuilder sb = stringBuilderPool.borrow();
        incrementCounter("stringbuilder_borrows");
        return sb;
    }
    
    /**
     * Returns a poolable StringBuilder
     * 
     * @param stringBuilder StringBuilder to return
     */
    public void returnStringBuilder(@NotNull PoolableStringBuilder stringBuilder) {
        stringBuilderPool.returnObject(stringBuilder);
        incrementCounter("stringbuilder_returns");
    }
    
    /**
     * Borrows a poolable string list
     * 
     * @return poolable string list
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public PoolableList<String> borrowStringList() {
        PoolableList<String> list = stringListPool.borrow();
        incrementCounter("stringlist_borrows");
        return list;
    }
    
    /**
     * Returns a poolable string list
     * 
     * @param list string list to return
     */
    public void returnStringList(@NotNull PoolableList<String> list) {
        stringListPool.returnObject(list);
        incrementCounter("stringlist_returns");
    }
    
    /**
     * Borrows a poolable object list
     * 
     * @return poolable object list
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public PoolableList<Object> borrowObjectList() {
        PoolableList<Object> list = objectListPool.borrow();
        incrementCounter("objectlist_borrows");
        return list;
    }
    
    /**
     * Returns a poolable object list
     * 
     * @param list object list to return
     */
    public void returnObjectList(@NotNull PoolableList<Object> list) {
        objectListPool.returnObject(list);
        incrementCounter("objectlist_returns");
    }
    
    /**
     * Executes a function with a pooled StringBuilder
     * 
     * @param function function to execute
     * @return result string
     */
    @NotNull
    public String withStringBuilder(@NotNull java.util.function.Function<PoolableStringBuilder, String> function) {
        PoolableStringBuilder sb = borrowStringBuilder();
        try {
            return function.apply(sb);
        } finally {
            returnStringBuilder(sb);
        }
    }
    
    /**
     * Executes a function with a pooled string list
     * 
     * @param function function to execute
     * @param <R> return type
     * @return function result
     */
    @NotNull
    public <R> R withStringList(@NotNull java.util.function.Function<PoolableList<String>, R> function) {
        PoolableList<String> list = borrowStringList();
        try {
            return function.apply(list);
        } finally {
            returnStringList(list);
        }
    }
    
    /**
     * Executes a function with a pooled object list
     * 
     * @param function function to execute
     * @param <R> return type
     * @return function result
     */
    @NotNull
    public <R> R withObjectList(@NotNull java.util.function.Function<PoolableList<Object>, R> function) {
        PoolableList<Object> list = borrowObjectList();
        try {
            return function.apply(list);
        } finally {
            returnObjectList(list);
        }
    }
    
    /**
     * Optimizes memory usage across all OneBlock systems
     */
    public void optimizeSystemMemory() {
        LOGGER.info("Starting system-wide memory optimization...");
        
        // Get current memory statistics
        MemoryManager.MemoryStatistics beforeStats = memoryManager.getMemoryStatistics();
        
        // Optimize caches
        CacheManager cacheManager = CacheManager.getInstance();
        cacheManager.performCleanup();
        int cachesCleaned = cacheManager.getAllCacheStatistics().size();
        
        // Optimize object pools
        MemoryManager.MemoryPressureLevel pressureLevel = memoryManager.getMemoryPressureLevel();
        poolManager.cleanup(pressureLevel);
        
        // Trigger memory cleanup
        memoryManager.performMemoryCleanup(pressureLevel);
        
        // Perform garbage collection if needed
        if (pressureLevel == MemoryManager.MemoryPressureLevel.HIGH || 
            pressureLevel == MemoryManager.MemoryPressureLevel.CRITICAL) {
            memoryManager.performGarbageCollection(false);
        }
        
        // Get after statistics
        MemoryManager.MemoryStatistics afterStats = memoryManager.getMemoryStatistics();
        
        long memoryFreed = beforeStats.getHeapUsed() - afterStats.getHeapUsed();
        
        LOGGER.info(String.format("Memory optimization completed - Freed: %d MB, Caches cleaned: %d, " +
                "Memory usage: %.1f%% -> %.1f%%",
            memoryFreed / 1024 / 1024,
            cachesCleaned,
            beforeStats.getHeapUsagePercent() * 100,
            afterStats.getHeapUsagePercent() * 100
        ));
        
        incrementCounter("memory_optimizations");
    }
    
    /**
     * Gets memory optimization statistics
     * 
     * @return memory optimization statistics
     */
    @NotNull
    public MemoryOptimizationStats getOptimizationStats() {
        MemoryManager.MemoryStatistics memStats = memoryManager.getMemoryStatistics();
        ObjectPoolManager.PoolStatistics poolStats = poolManager.getStatistics();
        CacheManager.CacheStatistics cacheStats = CacheManager.getInstance().getGlobalStatistics();
        
        return new MemoryOptimizationStats(memStats, poolStats, cacheStats);
    }
    
    /**
     * Gets memory optimization recommendations
     * 
     * @return list of optimization recommendations
     */
    @NotNull
    public List<MemoryManager.MemoryRecommendation> getOptimizationRecommendations() {
        List<MemoryManager.MemoryRecommendation> recommendations = memoryManager.getMemoryRecommendations();
        
        // Add pool-specific recommendations
        ObjectPoolManager.PoolStatistics poolStats = poolManager.getStatistics();
        if (poolStats.getAverageUtilization() > 0.9) {
            recommendations.add(new MemoryManager.MemoryRecommendation(
                MemoryManager.MemoryRecommendation.Type.OBJECT_POOLING,
                MemoryManager.MemoryRecommendation.Priority.MEDIUM,
                "High object pool utilization: " + String.format("%.1f%%", poolStats.getAverageUtilization() * 100),
                "Consider increasing pool sizes for frequently used objects"
            ));
        }
        
        // Add cache-specific recommendations
        CacheManager.CacheStatistics cacheStats = CacheManager.getInstance().getGlobalStatistics();
        if (cacheStats.getEvictionCount() > cacheStats.getHitCount() * 0.1) {
            recommendations.add(new MemoryManager.MemoryRecommendation(
                MemoryManager.MemoryRecommendation.Type.MEMORY_USAGE,
                MemoryManager.MemoryRecommendation.Priority.MEDIUM,
                "High cache eviction rate detected",
                "Consider increasing cache sizes or adjusting expiration policies"
            ));
        }
        
        return recommendations;
    }
    
    @Override
    public void onMemoryPressure(@NotNull MemoryManager.MemoryPressureLevel level) {
        LOGGER.info("Memory pressure detected (" + level + "), performing cleanup...");
        
        switch (level) {
            case CRITICAL:
                // Aggressive cleanup
                CacheManager.getInstance().performCleanup();
                poolManager.cleanup(level);
                break;
                
            case HIGH:
                // Moderate cleanup
                CacheManager.getInstance().performCleanup();
                poolManager.cleanup(level);
                break;
                
            case MEDIUM:
                // Light cleanup
                poolManager.cleanup(level);
                break;
                
            case LOW:
                // No action needed
                break;
        }
        
        incrementCounter("memory_pressure_cleanups");
    }
    
    /**
     * Memory optimization statistics
     */
    public static class MemoryOptimizationStats {
        private final MemoryManager.MemoryStatistics memoryStats;
        private final ObjectPoolManager.PoolStatistics poolStats;
        private final CacheManager.CacheStatistics cacheStats;
        
        public MemoryOptimizationStats(MemoryManager.MemoryStatistics memoryStats,
                                     ObjectPoolManager.PoolStatistics poolStats,
                                     CacheManager.CacheStatistics cacheStats) {
            this.memoryStats = memoryStats;
            this.poolStats = poolStats;
            this.cacheStats = cacheStats;
        }
        
        public MemoryManager.MemoryStatistics getMemoryStats() { return memoryStats; }
        public ObjectPoolManager.PoolStatistics getPoolStats() { return poolStats; }
        public CacheManager.CacheStatistics getCacheStats() { return cacheStats; }
        
        public double getOverallEfficiency() {
            // Calculate overall memory efficiency based on cache hit rates and pool utilization
            double cacheEfficiency = cacheStats.getHitRate();
            double poolEfficiency = poolStats.getAverageUtilization();
            double memoryEfficiency = 1.0 - memoryStats.getHeapUsagePercent();
            
            return (cacheEfficiency + poolEfficiency + memoryEfficiency) / 3.0;
        }
        
        @Override
        public String toString() {
            return String.format("MemoryOptimizationStats{efficiency=%.1f%%, memory=%s, pools=%s, cache=%s}",
                getOverallEfficiency() * 100, memoryStats, poolStats, cacheStats);
        }
    }
}