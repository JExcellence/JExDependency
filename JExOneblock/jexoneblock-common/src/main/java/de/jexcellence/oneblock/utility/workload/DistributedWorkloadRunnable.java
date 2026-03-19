package de.jexcellence.oneblock.utility.workload;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * High-performance distributed workload processor with adaptive timing and smart batching
 */
public class DistributedWorkloadRunnable extends BukkitRunnable {
    
    private static final double DEFAULT_MAX_MILLIS_PER_TICK = 2.5;
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final int ADAPTIVE_BATCH_MIN = 1;
    private static final int ADAPTIVE_BATCH_MAX = 50;
    
    private final ConcurrentLinkedQueue<IWorkload> workloadQueue;
    private final double maxMillisPerTick;
    private final long maxNanosPerTick;
    private final Logger logger;
    private final AtomicBoolean isRunning;
    private final AtomicLong totalWorkloadsProcessed;
    private final AtomicLong totalProcessingTime;
    private final AtomicInteger currentQueueSize;
    private final AtomicLong lastExecutionTime;
    
    // Adaptive performance tracking
    private final AtomicInteger adaptiveBatchSize;
    private final AtomicLong lastTickTime;
    private final AtomicInteger consecutiveSlowTicks;
    
    // Callbacks
    private Consumer<IWorkload> onWorkloadComplete;
    private Consumer<Exception> onWorkloadError;
    private Runnable onQueueEmpty;
    
    public DistributedWorkloadRunnable(final @NotNull Plugin plugin) {
        this(plugin, DEFAULT_MAX_MILLIS_PER_TICK);
    }
    
    public DistributedWorkloadRunnable(final @NotNull Plugin plugin, final double maxMillisPerTick) {
        this.workloadQueue = new ConcurrentLinkedQueue<>();
        this.maxMillisPerTick = maxMillisPerTick;
        this.maxNanosPerTick = (long) (maxMillisPerTick * NANOS_PER_MILLI);
        this.logger = plugin.getLogger();
        this.isRunning = new AtomicBoolean(false);
        this.totalWorkloadsProcessed = new AtomicLong(0);
        this.totalProcessingTime = new AtomicLong(0);
        this.currentQueueSize = new AtomicInteger(0);
        this.lastExecutionTime = new AtomicLong(0);
        
        // Adaptive performance tracking
        this.adaptiveBatchSize = new AtomicInteger(10);
        this.lastTickTime = new AtomicLong(0);
        this.consecutiveSlowTicks = new AtomicInteger(0);
    }
    
    /**
     * Adds a workload to the queue with priority support
     */
    public void addWorkload(final @NotNull IWorkload workload) {
        workloadQueue.offer(workload);
        currentQueueSize.incrementAndGet();
    }
    
    /**
     * Adds multiple workloads efficiently
     */
    public void addWorkloads(final @NotNull IWorkload... workloads) {
        for (final IWorkload workload : workloads) {
            workloadQueue.offer(workload);
        }
        currentQueueSize.addAndGet(workloads.length);
    }
    
    @Override
    public void run() {
        if (!isRunning.compareAndSet(false, true)) {
            return; // Already running
        }
        
        final long startTime = System.nanoTime();
        final long stopTime = startTime + maxNanosPerTick;
        int processedCount = 0;
        
        try {
            // Adaptive batch processing
            final int currentBatch = adaptiveBatchSize.get();
            
            while (System.nanoTime() <= stopTime && !workloadQueue.isEmpty() && processedCount < currentBatch) {
                final IWorkload workload = workloadQueue.poll();
                
                if (workload == null) {
                    break;
                }
                
                currentQueueSize.decrementAndGet();
                
                try {
                    final long workloadStartTime = System.nanoTime();
                    workload.compute();
                    final long workloadEndTime = System.nanoTime();
                    
                    totalProcessingTime.addAndGet(workloadEndTime - workloadStartTime);
                    processedCount++;
                    
                    if (onWorkloadComplete != null) {
                        onWorkloadComplete.accept(workload);
                    }
                    
                } catch (final Exception e) {
                    logger.log(Level.WARNING, "Error processing workload: " + workload.getClass().getSimpleName(), e);
                    
                    if (onWorkloadError != null) {
                        onWorkloadError.accept(e);
                    }
                }
            }
            
            // Adaptive batch size adjustment
            adjustBatchSize(startTime, processedCount);
            
            // Check if queue is empty and notify
            if (workloadQueue.isEmpty() && onQueueEmpty != null) {
                onQueueEmpty.run();
            }
            
        } finally {
            totalWorkloadsProcessed.addAndGet(processedCount);
            lastExecutionTime.set(System.nanoTime() - startTime);
            isRunning.set(false);
        }
    }
    
    /**
     * Dynamically adjusts batch size based on performance
     */
    private void adjustBatchSize(final long startTime, final int processedCount) {
        final long executionTime = System.nanoTime() - startTime;
        final long currentTick = System.currentTimeMillis() / 50; // Minecraft tick
        
        if (lastTickTime.get() != currentTick) {
            lastTickTime.set(currentTick);
            
            if (executionTime > maxNanosPerTick * 0.8) { // Using 80% of available time
                // Slow tick - reduce batch size
                consecutiveSlowTicks.incrementAndGet();
                if (consecutiveSlowTicks.get() > 3) {
                    adaptiveBatchSize.updateAndGet(current -> Math.max(ADAPTIVE_BATCH_MIN, current - 2));
                    consecutiveSlowTicks.set(0);
                }
            } else if (executionTime < maxNanosPerTick * 0.5 && processedCount > 0) {
                // Fast tick - increase batch size
                consecutiveSlowTicks.set(0);
                adaptiveBatchSize.updateAndGet(current -> Math.min(ADAPTIVE_BATCH_MAX, current + 1));
            }
        }
    }
    
    // ... rest of the methods remain the same but with improved performance tracking
    
    public int getQueueSize() {
        return currentQueueSize.get();
    }
    
    public boolean isEmpty() {
        return workloadQueue.isEmpty();
    }
    
    public boolean isProcessing() {
        return isRunning.get();
    }
    
    public long getTotalWorkloadsProcessed() {
        return totalWorkloadsProcessed.get();
    }
    
    public double getAverageProcessingTime() {
        final long total = totalWorkloadsProcessed.get();
        return total > 0 ? (double) totalProcessingTime.get() / total : 0.0;
    }
    
    public long getLastExecutionTime() {
        return lastExecutionTime.get();
    }
    
    public int getCurrentBatchSize() {
        return adaptiveBatchSize.get();
    }
    
    public @NotNull WorkloadStatistics getStatistics() {
        return new WorkloadStatistics(
            totalWorkloadsProcessed.get(),
            currentQueueSize.get(),
            getAverageProcessingTime(),
            lastExecutionTime.get(),
            maxMillisPerTick,
            adaptiveBatchSize.get()
        );
    }
    
    public void clearQueue() {
        workloadQueue.clear();
        currentQueueSize.set(0);
    }
    
    public void setOnWorkloadComplete(final @Nullable Consumer<IWorkload> callback) {
        this.onWorkloadComplete = callback;
    }
    
    public void setOnWorkloadError(final @Nullable Consumer<Exception> callback) {
        this.onWorkloadError = callback;
    }
    
    public void setOnQueueEmpty(final @Nullable Runnable callback) {
        this.onQueueEmpty = callback;
    }
    
    /**
     * Enhanced performance statistics with adaptive metrics
     */
    public static class WorkloadStatistics {
        private final long totalProcessed;
        private final int queueSize;
        private final double averageProcessingTime;
        private final long lastExecutionTime;
        private final double maxMillisPerTick;
        private final int currentBatchSize;
        
        public WorkloadStatistics(
            final long totalProcessed,
            final int queueSize,
            final double averageProcessingTime,
            final long lastExecutionTime,
            final double maxMillisPerTick,
            final int currentBatchSize
        ) {
            this.totalProcessed = totalProcessed;
            this.queueSize = queueSize;
            this.averageProcessingTime = averageProcessingTime;
            this.lastExecutionTime = lastExecutionTime;
            this.maxMillisPerTick = maxMillisPerTick;
            this.currentBatchSize = currentBatchSize;
        }
        
        public long getTotalProcessed() { return totalProcessed; }
        public int getQueueSize() { return queueSize; }
        public double getAverageProcessingTime() { return averageProcessingTime; }
        public long getLastExecutionTime() { return lastExecutionTime; }
        public double getMaxMillisPerTick() { return maxMillisPerTick; }
        public int getCurrentBatchSize() { return currentBatchSize; }
        
        public double getAverageProcessingTimeMillis() {
            return averageProcessingTime / NANOS_PER_MILLI;
        }
        
        public double getLastExecutionTimeMillis() {
            return (double) lastExecutionTime / NANOS_PER_MILLI;
        }
        
        public double getEfficiencyPercentage() {
            return Math.min(100.0, (getLastExecutionTimeMillis() / maxMillisPerTick) * 100.0);
        }
        
        @Override
        public String toString() {
            return String.format(
                "WorkloadStats{processed=%d, queue=%d, avgTime=%.2fms, lastExec=%.2fms, efficiency=%.1f%%, batch=%d}",
                totalProcessed, queueSize, getAverageProcessingTimeMillis(),
                getLastExecutionTimeMillis(), getEfficiencyPercentage(), currentBatchSize
            );
        }
    }
}