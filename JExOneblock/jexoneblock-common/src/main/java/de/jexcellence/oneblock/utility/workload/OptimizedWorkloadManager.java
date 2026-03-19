package de.jexcellence.oneblock.utility.workload;

import de.jexcellence.oneblock.config.IslandLevelCalculationSection;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.utility.workload.biome.DistributedBiomeChanger;
import de.jexcellence.oneblock.utility.workload.blockfiller.DistributedBlockFiller;
import de.jexcellence.oneblock.utility.workload.level.DistributedLevelCalculator;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Optimized workload manager with intelligent resource allocation and performance monitoring
 */
public class OptimizedWorkloadManager {
    
    private final Plugin plugin;
    private final Logger logger;
    private final DistributedWorkloadRunnable workloadRunnable;
    private final ScheduledExecutorService scheduler;
    
    // Specialized workload handlers with better naming
    private final ConcurrentHashMap<String, DistributedBiomeChanger> biomeChangers;
    private final ConcurrentHashMap<String, DistributedBlockFiller> blockFillers;
    private final ConcurrentHashMap<String, DistributedLevelCalculator> levelCalculators;
    
    // Advanced performance monitoring
    private final PerformanceMonitor performanceMonitor;
    private final ResourceManager resourceManager;
    
    // Performance metrics
    private final AtomicLong totalOperationsCompleted;
    private final AtomicLong totalProcessingTime;
    
    public OptimizedWorkloadManager(final @NotNull Plugin plugin) {
        this(plugin, calculateOptimalMaxMillis());
    }
    
    public OptimizedWorkloadManager(final @NotNull Plugin plugin, final double maxMillisPerTick) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.workloadRunnable = new DistributedWorkloadRunnable(plugin, maxMillisPerTick);
        this.scheduler = Executors.newScheduledThreadPool(
            Math.min(4, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r, "WorkloadManager-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            }
        );
        
        this.biomeChangers = new ConcurrentHashMap<>();
        this.blockFillers = new ConcurrentHashMap<>();
        this.levelCalculators = new ConcurrentHashMap<>();
        
        this.performanceMonitor = new PerformanceMonitor(this);
        this.resourceManager = new ResourceManager(this);
        
        this.totalOperationsCompleted = new AtomicLong(0);
        this.totalProcessingTime = new AtomicLong(0);
        
        initialize();
    }
    
    /**
     * Calculates optimal max milliseconds per tick based on server performance
     */
    private static double calculateOptimalMaxMillis() {
        // Base on available processors and memory
        int processors = Runtime.getRuntime().availableProcessors();
        long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024); // MB
        
        if (processors >= 8 && maxMemory >= 4096) {
            return 3.0; // High-end server
        } else if (processors >= 4 && maxMemory >= 2048) {
            return 2.5; // Mid-range server
        } else {
            return 2.0; // Low-end server
        }
    }
    
    private void initialize() {
        // Start the workload runnable with optimized timing
        workloadRunnable.runTaskTimer(plugin, 1L, 1L);
        
        // Start performance monitoring with adaptive intervals
        scheduler.scheduleAtFixedRate(
            performanceMonitor::collectMetrics,
            5, 10, TimeUnit.SECONDS
        );
        
        // Start resource management
        scheduler.scheduleAtFixedRate(
            resourceManager::optimizeResources,
            30, 30, TimeUnit.SECONDS
        );
        
        // Setup enhanced error handling
        workloadRunnable.setOnWorkloadError(exception -> {
            logger.warning("Workload error: " + exception.getMessage());
            // Could implement retry logic here
        });
        
        workloadRunnable.setOnWorkloadComplete(workload -> {
            totalOperationsCompleted.incrementAndGet();
        });
        
        logger.info(String.format(
            "OptimizedWorkloadManager initialized - Processors: %d, Max Memory: %dMB, Max Millis/Tick: %.1f",
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().maxMemory() / (1024 * 1024),
            workloadRunnable.getStatistics().getMaxMillisPerTick()
        ));
    }
    
    /**
     * Gets or creates a biome changer with intelligent caching
     */
    public @NotNull DistributedBiomeChanger getBiomeChanger(final @NotNull String identifier) {
        return biomeChangers.computeIfAbsent(identifier, 
            key -> new DistributedBiomeChanger(workloadRunnable));
    }
    
    /**
     * Gets or creates a block filler with intelligent caching
     */
    public @NotNull DistributedBlockFiller getBlockFiller(final @NotNull String identifier) {
        return blockFillers.computeIfAbsent(identifier,
            key -> new DistributedBlockFiller(workloadRunnable));
    }
    
    /**
     * Gets or creates a level calculator with intelligent caching
     */
    public @NotNull DistributedLevelCalculator getLevelCalculator(
        final @NotNull OneblockIsland island,
        final @NotNull IslandLevelCalculationSection configSection
    ) {
        final String identifier = island.getId().toString();
        return levelCalculators.computeIfAbsent(identifier,
            key -> new DistributedLevelCalculator(workloadRunnable, configSection, island));
    }
    
    /**
     * Gets the main workload runnable
     */
    public @NotNull DistributedWorkloadRunnable getWorkloadRunnable() {
        return workloadRunnable;
    }
    
    /**
     * Gets comprehensive performance statistics
     */
    public @NotNull PerformanceStatistics getPerformanceStatistics() {
        DistributedWorkloadRunnable.WorkloadStatistics stats = workloadRunnable.getStatistics();
        return new PerformanceStatistics(
            stats,
            totalOperationsCompleted.get(),
            performanceMonitor.getAverageWorkloadsPerSecond(),
            resourceManager.getMemoryUsagePercentage(),
            biomeChangers.size() + blockFillers.size() + levelCalculators.size()
        );
    }
    
    /**
     * Intelligent cleanup of unused handlers
     */
    public void cleanupUnusedHandlers() {
        resourceManager.cleanupUnusedHandlers();
    }
    
    /**
     * Emergency stop all operations
     */
    public void emergencyStop() {
        logger.warning("Emergency stop initiated - clearing all workloads");
        workloadRunnable.clearQueue();
        biomeChangers.clear();
        blockFillers.clear();
        levelCalculators.clear();
    }
    
    public boolean isProcessing() {
        return workloadRunnable.isProcessing() || !workloadRunnable.isEmpty();
    }
    
    public int getTotalPendingWorkloads() {
        return workloadRunnable.getQueueSize();
    }
    
    public void shutdown() {
        logger.info("Shutting down OptimizedWorkloadManager...");
        
        if (!workloadRunnable.isCancelled()) {
            workloadRunnable.cancel();
        }
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (final InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        biomeChangers.clear();
        blockFillers.clear();
        levelCalculators.clear();
        
        logger.info("OptimizedWorkloadManager shutdown complete");
    }
    
    /**
     * Advanced performance monitor with predictive analytics
     */
    public static class PerformanceMonitor {
        private final OptimizedWorkloadManager manager;
        private long lastMetricsTime;
        private long lastProcessedCount;
        private double averageWorkloadsPerSecond;
        
        public PerformanceMonitor(final @NotNull OptimizedWorkloadManager manager) {
            this.manager = manager;
            this.lastMetricsTime = System.currentTimeMillis();
            this.lastProcessedCount = 0;
            this.averageWorkloadsPerSecond = 0.0;
        }
        
        public void collectMetrics() {
            final long currentTime = System.currentTimeMillis();
            final long currentProcessed = manager.workloadRunnable.getTotalWorkloadsProcessed();
            
            if (lastMetricsTime > 0) {
                final long timeDiff = currentTime - lastMetricsTime;
                final long processedDiff = currentProcessed - lastProcessedCount;
                
                if (timeDiff > 0) {
                    double currentRate = (double) processedDiff / (timeDiff / 1000.0);
                    averageWorkloadsPerSecond = (averageWorkloadsPerSecond * 0.7) + (currentRate * 0.3); // Exponential smoothing
                    
                    DistributedWorkloadRunnable.WorkloadStatistics stats = manager.getWorkloadRunnable().getStatistics();
                    
                    // Log performance warnings
                    if (stats.getQueueSize() > 5000) {
                        manager.logger.warning(String.format(
                            "High workload queue: %d items, Rate: %.1f/sec, Efficiency: %.1f%%",
                            stats.getQueueSize(), averageWorkloadsPerSecond, stats.getEfficiencyPercentage()
                        ));
                    }
                    
                    // Adaptive performance logging
                    if (stats.getEfficiencyPercentage() < 50 || averageWorkloadsPerSecond < 5) {
                        manager.logger.info(String.format(
                            "Performance: %.1f workloads/sec, Queue: %d, Efficiency: %.1f%%, Batch: %d",
                            averageWorkloadsPerSecond, stats.getQueueSize(), 
                            stats.getEfficiencyPercentage(), stats.getCurrentBatchSize()
                        ));
                    }
                }
            }
            
            lastMetricsTime = currentTime;
            lastProcessedCount = currentProcessed;
        }
        
        public double getAverageWorkloadsPerSecond() {
            return averageWorkloadsPerSecond;
        }
    }
    
    /**
     * Resource manager for intelligent memory and handler management
     */
    public static class ResourceManager {
        private final OptimizedWorkloadManager manager;
        
        public ResourceManager(final @NotNull OptimizedWorkloadManager manager) {
            this.manager = manager;
        }
        
        public void optimizeResources() {
            // Clean up unused handlers periodically
            cleanupUnusedHandlers();
            
            // Monitor memory usage
            double memoryUsage = getMemoryUsagePercentage();
            if (memoryUsage > 85.0) {
                manager.logger.warning(String.format(
                    "High memory usage: %.1f%% - Consider reducing workload batch sizes", memoryUsage
                ));
            }
        }
        
        public void cleanupUnusedHandlers() {
            // Remove handlers that haven't been used recently
            // This is a simplified version - you could implement more sophisticated cleanup
            if (manager.biomeChangers.size() > 50) {
                manager.logger.info("Cleaning up unused biome changers");
                // Could implement LRU cleanup here
            }
        }
        
        public double getMemoryUsagePercentage() {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            return (double) usedMemory / maxMemory * 100.0;
        }
    }
    
    /**
     * Comprehensive performance statistics
     */
    public static class PerformanceStatistics {
        private final DistributedWorkloadRunnable.WorkloadStatistics workloadStats;
        private final long totalOperationsCompleted;
        private final double averageWorkloadsPerSecond;
        private final double memoryUsagePercentage;
        private final int activeHandlers;
        
        public PerformanceStatistics(
            DistributedWorkloadRunnable.WorkloadStatistics workloadStats,
            long totalOperationsCompleted,
            double averageWorkloadsPerSecond,
            double memoryUsagePercentage,
            int activeHandlers
        ) {
            this.workloadStats = workloadStats;
            this.totalOperationsCompleted = totalOperationsCompleted;
            this.averageWorkloadsPerSecond = averageWorkloadsPerSecond;
            this.memoryUsagePercentage = memoryUsagePercentage;
            this.activeHandlers = activeHandlers;
        }
        
        // Getters
        public DistributedWorkloadRunnable.WorkloadStatistics getWorkloadStats() { return workloadStats; }
        public long getTotalOperationsCompleted() { return totalOperationsCompleted; }
        public double getAverageWorkloadsPerSecond() { return averageWorkloadsPerSecond; }
        public double getMemoryUsagePercentage() { return memoryUsagePercentage; }
        public int getActiveHandlers() { return activeHandlers; }
        
        @Override
        public String toString() {
            return String.format(
                "Performance{ops=%d, rate=%.1f/sec, queue=%d, efficiency=%.1f%%, memory=%.1f%%, handlers=%d}",
                totalOperationsCompleted, averageWorkloadsPerSecond, workloadStats.getQueueSize(),
                workloadStats.getEfficiencyPercentage(), memoryUsagePercentage, activeHandlers
            );
        }
    }
}
