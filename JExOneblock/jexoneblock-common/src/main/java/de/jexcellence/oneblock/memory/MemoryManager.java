package de.jexcellence.oneblock.memory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Memory Manager
 * 
 * Provides intelligent memory management with automatic garbage collection optimization,
 * memory leak detection, and proactive memory cleanup for optimal OneBlock performance.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class MemoryManager {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final MemoryMXBean memoryBean;
    private final ScheduledExecutorService memoryMonitor;
    
    // Simple operation counters
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
    
    // Memory thresholds
    private static final double HIGH_MEMORY_THRESHOLD = 0.85; // 85%
    private static final double CRITICAL_MEMORY_THRESHOLD = 0.95; // 95%
    private static final long MEMORY_CHECK_INTERVAL = 30; // seconds
    
    // Memory tracking
    private final AtomicLong totalAllocated = new AtomicLong(0);
    private final AtomicLong totalFreed = new AtomicLong(0);
    private final AtomicLong gcCount = new AtomicLong(0);
    private final AtomicLong lastGcTime = new AtomicLong(0);
    
    // Memory cleanup callbacks
    private final ConcurrentHashMap<String, WeakReference<MemoryCleanupCallback>> cleanupCallbacks = new ConcurrentHashMap<>();
    
    // Memory pools for object reuse
    private final ObjectPoolManager objectPoolManager;
    
    private static MemoryManager instance;
    
    private MemoryManager() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.objectPoolManager = new ObjectPoolManager();
        this.memoryMonitor = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "OneBlock-MemoryMonitor");
            t.setDaemon(true);
            return t;
        });
        
        startMemoryMonitoring();
        
        LOGGER.info("MemoryManager initialized with intelligent memory optimization");
    }
    
    /**
     * Increments a counter
     */
    private void incrementCounter(@NotNull String name) {
        counters.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * Adds to a counter
     */
    private void addToCounter(@NotNull String name, long value) {
        counters.computeIfAbsent(name, k -> new AtomicLong(0)).addAndGet(value);
    }
    
    /**
     * Records an operation
     */
    private void recordOperation(@NotNull String name, long durationNanos, boolean success) {
        if (success) {
            incrementCounter(name + "_success");
        } else {
            incrementCounter(name + "_failed");
        }
        addToCounter(name + "_duration_ns", durationNanos);
    }
    
    /**
     * Gets the singleton instance
     */
    @NotNull
    public static synchronized MemoryManager getInstance() {
        if (instance == null) {
            instance = new MemoryManager();
        }
        return instance;
    }
    
    /**
     * Gets current memory statistics
     * 
     * @return current memory statistics
     */
    @NotNull
    public MemoryStatistics getMemoryStatistics() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        long heapUsed = heapUsage.getUsed();
        long heapMax = heapUsage.getMax();
        long heapCommitted = heapUsage.getCommitted();
        
        long nonHeapUsed = nonHeapUsage.getUsed();
        long nonHeapMax = nonHeapUsage.getMax();
        long nonHeapCommitted = nonHeapUsage.getCommitted();
        
        double heapUsagePercent = heapMax > 0 ? (double) heapUsed / heapMax : 0.0;
        double nonHeapUsagePercent = nonHeapMax > 0 ? (double) nonHeapUsed / nonHeapMax : 0.0;
        
        return new MemoryStatistics(
            heapUsed, heapMax, heapCommitted, heapUsagePercent,
            nonHeapUsed, nonHeapMax, nonHeapCommitted, nonHeapUsagePercent,
            totalAllocated.get(), totalFreed.get(), gcCount.get(), lastGcTime.get()
        );
    }
    
    /**
     * Registers a memory cleanup callback
     * 
     * @param name callback name
     * @param callback cleanup callback
     */
    public void registerCleanupCallback(@NotNull String name, @NotNull MemoryCleanupCallback callback) {
        cleanupCallbacks.put(name, new WeakReference<>(callback));
        LOGGER.fine("Registered memory cleanup callback: " + name);
    }
    
    /**
     * Unregisters a memory cleanup callback
     * 
     * @param name callback name
     */
    public void unregisterCleanupCallback(@NotNull String name) {
        cleanupCallbacks.remove(name);
        LOGGER.fine("Unregistered memory cleanup callback: " + name);
    }
    
    /**
     * Performs intelligent garbage collection
     * 
     * @param force whether to force full GC
     * @return true if GC was performed
     */
    public boolean performGarbageCollection(boolean force) {
        MemoryStatistics stats = getMemoryStatistics();
        
        if (force || stats.getHeapUsagePercent() > HIGH_MEMORY_THRESHOLD) {
            long startTime = System.currentTimeMillis();
            
            // Trigger cleanup callbacks first
            triggerMemoryCleanup(MemoryPressureLevel.HIGH);
            
            // Perform garbage collection
            System.gc();
            
            long gcTime = System.currentTimeMillis() - startTime;
            gcCount.incrementAndGet();
            lastGcTime.set(gcTime);
            
            recordOperation("garbage_collection", gcTime * 1_000_000, true);
            incrementCounter("gc_triggered");
            
            LOGGER.info("Garbage collection completed in " + gcTime + "ms (forced: " + force + ")");
            return true;
        }
        
        return false;
    }
    
    /**
     * Performs proactive memory cleanup
     * 
     * @param level memory pressure level
     */
    public void performMemoryCleanup(@NotNull MemoryPressureLevel level) {
        long startTime = System.currentTimeMillis();
        
        // Trigger cleanup callbacks
        int callbacksTriggered = triggerMemoryCleanup(level);
        
        // Clean up object pools
        objectPoolManager.cleanup(level);
        
        // Clean up weak references
        cleanupWeakReferences();
        
        long cleanupTime = System.currentTimeMillis() - startTime;
        
        recordOperation("memory_cleanup", cleanupTime * 1_000_000, true);
        incrementCounter("memory_cleanups");
        
        LOGGER.info("Memory cleanup completed in " + cleanupTime + "ms (level: " + level + 
                   ", callbacks: " + callbacksTriggered + ")");
    }
    
    /**
     * Allocates memory with tracking
     * 
     * @param size size in bytes
     */
    public void trackAllocation(long size) {
        totalAllocated.addAndGet(size);
        addToCounter("memory_allocated", size);
    }
    
    /**
     * Tracks memory deallocation
     * 
     * @param size size in bytes
     */
    public void trackDeallocation(long size) {
        totalFreed.addAndGet(size);
        addToCounter("memory_freed", size);
    }
    
    /**
     * Gets object pool manager
     * 
     * @return object pool manager
     */
    @NotNull
    public ObjectPoolManager getObjectPoolManager() {
        return objectPoolManager;
    }
    
    /**
     * Checks if memory usage is critical
     * 
     * @return true if memory usage is critical
     */
    public boolean isMemoryUsageCritical() {
        return getMemoryStatistics().getHeapUsagePercent() > CRITICAL_MEMORY_THRESHOLD;
    }
    
    /**
     * Checks if memory usage is high
     * 
     * @return true if memory usage is high
     */
    public boolean isMemoryUsageHigh() {
        return getMemoryStatistics().getHeapUsagePercent() > HIGH_MEMORY_THRESHOLD;
    }
    
    /**
     * Gets memory pressure level
     * 
     * @return current memory pressure level
     */
    @NotNull
    public MemoryPressureLevel getMemoryPressureLevel() {
        double usage = getMemoryStatistics().getHeapUsagePercent();
        
        if (usage > CRITICAL_MEMORY_THRESHOLD) {
            return MemoryPressureLevel.CRITICAL;
        } else if (usage > HIGH_MEMORY_THRESHOLD) {
            return MemoryPressureLevel.HIGH;
        } else if (usage > 0.7) {
            return MemoryPressureLevel.MEDIUM;
        } else {
            return MemoryPressureLevel.LOW;
        }
    }
    
    /**
     * Optimizes memory usage based on current conditions
     */
    public void optimizeMemoryUsage() {
        MemoryPressureLevel level = getMemoryPressureLevel();
        
        switch (level) {
            case CRITICAL:
                LOGGER.warning("Critical memory usage detected, performing aggressive cleanup");
                performMemoryCleanup(MemoryPressureLevel.CRITICAL);
                performGarbageCollection(true);
                break;
                
            case HIGH:
                LOGGER.info("High memory usage detected, performing cleanup");
                performMemoryCleanup(MemoryPressureLevel.HIGH);
                performGarbageCollection(false);
                break;
                
            case MEDIUM:
                performMemoryCleanup(MemoryPressureLevel.MEDIUM);
                break;
                
            case LOW:
                // No action needed
                break;
        }
    }
    
    /**
     * Gets memory usage recommendations
     * 
     * @return list of memory optimization recommendations
     */
    @NotNull
    public java.util.List<MemoryRecommendation> getMemoryRecommendations() {
        java.util.List<MemoryRecommendation> recommendations = new java.util.ArrayList<>();
        MemoryStatistics stats = getMemoryStatistics();
        
        // High memory usage
        if (stats.getHeapUsagePercent() > HIGH_MEMORY_THRESHOLD) {
            recommendations.add(new MemoryRecommendation(
                MemoryRecommendation.Type.MEMORY_USAGE,
                MemoryRecommendation.Priority.HIGH,
                "High heap memory usage: " + String.format("%.1f%%", stats.getHeapUsagePercent() * 100),
                "Consider increasing heap size or optimizing memory usage"
            ));
        }
        
        // Frequent GC
        if (gcCount.get() > 0) {
            long avgGcTime = lastGcTime.get();
            if (avgGcTime > 1000) { // More than 1 second
                recommendations.add(new MemoryRecommendation(
                    MemoryRecommendation.Type.GARBAGE_COLLECTION,
                    MemoryRecommendation.Priority.MEDIUM,
                    "Long garbage collection times: " + avgGcTime + "ms",
                    "Consider tuning GC parameters or reducing memory allocation"
                ));
            }
        }
        
        // Memory leak detection
        long netAllocation = totalAllocated.get() - totalFreed.get();
        if (netAllocation > stats.getHeapUsed() * 2) {
            recommendations.add(new MemoryRecommendation(
                MemoryRecommendation.Type.MEMORY_LEAK,
                MemoryRecommendation.Priority.HIGH,
                "Potential memory leak detected",
                "Review object lifecycle and ensure proper cleanup"
            ));
        }
        
        // Object pool optimization
        ObjectPoolManager.PoolStatistics poolStats = objectPoolManager.getStatistics();
        if (poolStats.getTotalPools() > 0 && poolStats.getAverageUtilization() < 0.3) {
            recommendations.add(new MemoryRecommendation(
                MemoryRecommendation.Type.OBJECT_POOLING,
                MemoryRecommendation.Priority.LOW,
                "Low object pool utilization: " + String.format("%.1f%%", poolStats.getAverageUtilization() * 100),
                "Consider reducing pool sizes or reviewing pooling strategy"
            ));
        }
        
        return recommendations;
    }
    
    /**
     * Starts memory monitoring
     */
    private void startMemoryMonitoring() {
        memoryMonitor.scheduleAtFixedRate(() -> {
            try {
                MemoryStatistics stats = getMemoryStatistics();
                
                // Update counters
                addToCounter("heap_memory_used", stats.getHeapUsed());
                addToCounter("heap_memory_max", stats.getHeapMax());
                addToCounter("nonheap_memory_used", stats.getNonHeapUsed());
                
                // Check for memory pressure
                MemoryPressureLevel level = getMemoryPressureLevel();
                if (level == MemoryPressureLevel.HIGH || level == MemoryPressureLevel.CRITICAL) {
                    optimizeMemoryUsage();
                }
                
                // Log memory status periodically
                if (System.currentTimeMillis() % (5 * 60 * 1000) < MEMORY_CHECK_INTERVAL * 1000) {
                    LOGGER.info(String.format("Memory Status - Heap: %.1f%% (%d/%d MB), NonHeap: %.1f%% (%d/%d MB)",
                        stats.getHeapUsagePercent() * 100,
                        stats.getHeapUsed() / 1024 / 1024,
                        stats.getHeapMax() / 1024 / 1024,
                        stats.getNonHeapUsagePercent() * 100,
                        stats.getNonHeapUsed() / 1024 / 1024,
                        stats.getNonHeapMax() / 1024 / 1024
                    ));
                }
                
            } catch (Exception e) {
                LOGGER.warning("Error during memory monitoring: " + e.getMessage());
            }
        }, MEMORY_CHECK_INTERVAL, MEMORY_CHECK_INTERVAL, TimeUnit.SECONDS);
    }
    
    /**
     * Triggers memory cleanup callbacks
     */
    private int triggerMemoryCleanup(@NotNull MemoryPressureLevel level) {
        int triggered = 0;
        
        for (java.util.Map.Entry<String, WeakReference<MemoryCleanupCallback>> entry : cleanupCallbacks.entrySet()) {
            WeakReference<MemoryCleanupCallback> ref = entry.getValue();
            MemoryCleanupCallback callback = ref.get();
            
            if (callback != null) {
                try {
                    callback.onMemoryPressure(level);
                    triggered++;
                } catch (Exception e) {
                    LOGGER.warning("Error in memory cleanup callback " + entry.getKey() + ": " + e.getMessage());
                }
            }
        }
        
        return triggered;
    }
    
    /**
     * Cleans up weak references
     */
    private void cleanupWeakReferences() {
        cleanupCallbacks.entrySet().removeIf(entry -> entry.getValue().get() == null);
    }
    
    /**
     * Shuts down memory manager
     */
    public void shutdown() {
        memoryMonitor.shutdown();
        try {
            if (!memoryMonitor.awaitTermination(5, TimeUnit.SECONDS)) {
                memoryMonitor.shutdownNow();
            }
        } catch (InterruptedException e) {
            memoryMonitor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        objectPoolManager.shutdown();
        cleanupCallbacks.clear();
        
        LOGGER.info("MemoryManager shut down");
    }
    
    /**
     * Memory pressure levels
     */
    public enum MemoryPressureLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    /**
     * Memory cleanup callback interface
     */
    public interface MemoryCleanupCallback {
        void onMemoryPressure(@NotNull MemoryPressureLevel level);
    }
    
    /**
     * Memory statistics
     */
    public static class MemoryStatistics {
        private final long heapUsed;
        private final long heapMax;
        private final long heapCommitted;
        private final double heapUsagePercent;
        
        private final long nonHeapUsed;
        private final long nonHeapMax;
        private final long nonHeapCommitted;
        private final double nonHeapUsagePercent;
        
        private final long totalAllocated;
        private final long totalFreed;
        private final long gcCount;
        private final long lastGcTime;
        
        public MemoryStatistics(long heapUsed, long heapMax, long heapCommitted, double heapUsagePercent,
                              long nonHeapUsed, long nonHeapMax, long nonHeapCommitted, double nonHeapUsagePercent,
                              long totalAllocated, long totalFreed, long gcCount, long lastGcTime) {
            this.heapUsed = heapUsed;
            this.heapMax = heapMax;
            this.heapCommitted = heapCommitted;
            this.heapUsagePercent = heapUsagePercent;
            this.nonHeapUsed = nonHeapUsed;
            this.nonHeapMax = nonHeapMax;
            this.nonHeapCommitted = nonHeapCommitted;
            this.nonHeapUsagePercent = nonHeapUsagePercent;
            this.totalAllocated = totalAllocated;
            this.totalFreed = totalFreed;
            this.gcCount = gcCount;
            this.lastGcTime = lastGcTime;
        }
        
        // Getters
        public long getHeapUsed() { return heapUsed; }
        public long getHeapMax() { return heapMax; }
        public long getHeapCommitted() { return heapCommitted; }
        public double getHeapUsagePercent() { return heapUsagePercent; }
        
        public long getNonHeapUsed() { return nonHeapUsed; }
        public long getNonHeapMax() { return nonHeapMax; }
        public long getNonHeapCommitted() { return nonHeapCommitted; }
        public double getNonHeapUsagePercent() { return nonHeapUsagePercent; }
        
        public long getTotalAllocated() { return totalAllocated; }
        public long getTotalFreed() { return totalFreed; }
        public long getGcCount() { return gcCount; }
        public long getLastGcTime() { return lastGcTime; }
        
        @Override
        public String toString() {
            return String.format("MemoryStats{heap=%.1f%% (%d/%d MB), nonHeap=%.1f%% (%d/%d MB), gc=%d}",
                heapUsagePercent * 100, heapUsed / 1024 / 1024, heapMax / 1024 / 1024,
                nonHeapUsagePercent * 100, nonHeapUsed / 1024 / 1024, nonHeapMax / 1024 / 1024,
                gcCount);
        }
    }
    
    /**
     * Memory recommendation
     */
    public static class MemoryRecommendation {
        public enum Type {
            MEMORY_USAGE, GARBAGE_COLLECTION, MEMORY_LEAK, OBJECT_POOLING, CONFIGURATION
        }
        
        public enum Priority {
            LOW, MEDIUM, HIGH, CRITICAL
        }
        
        private final Type type;
        private final Priority priority;
        private final String issue;
        private final String recommendation;
        
        public MemoryRecommendation(Type type, Priority priority, String issue, String recommendation) {
            this.type = type;
            this.priority = priority;
            this.issue = issue;
            this.recommendation = recommendation;
        }
        
        public Type getType() { return type; }
        public Priority getPriority() { return priority; }
        public String getIssue() { return issue; }
        public String getRecommendation() { return recommendation; }
        
        @Override
        public String toString() {
            return String.format("[%s-%s] %s: %s", type, priority, issue, recommendation);
        }
    }
}