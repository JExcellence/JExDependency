package de.jexcellence.oneblock.utility.workload.biome;

import de.jexcellence.oneblock.database.entity.oneblock.IslandRegion;
import de.jexcellence.oneblock.utility.workload.DistributedWorkloadRunnable;
import de.jexcellence.oneblock.utility.workload.IWorkload;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * High-performance distributed biome changer with intelligent sampling and adaptive processing
 */
public class DistributedBiomeChanger implements IBiomeChanger {
    
    private final DistributedWorkloadRunnable workloadRunnable;
    private final int biomeStepSize;
    private final boolean optimizeForPerformance;
    
    // Performance constants
    private static final int DEFAULT_STEP_SIZE = 4;
    private static final int BATCH_SIZE = 1000;
    private static final int PROGRESS_UPDATE_INTERVAL = 200;
    
    // Performance tracking
    private final AtomicLong totalBiomeChanges;
    private final AtomicLong totalProcessingTime;
    
    public DistributedBiomeChanger(final @NotNull DistributedWorkloadRunnable workloadRunnable) {
        this(workloadRunnable, DEFAULT_STEP_SIZE, true);
    }
    
    public DistributedBiomeChanger(
        final @NotNull DistributedWorkloadRunnable workloadRunnable,
        final int biomeStepSize,
        final boolean optimizeForPerformance
    ) {
        this.workloadRunnable = workloadRunnable;
        this.biomeStepSize = Math.max(1, Math.min(16, biomeStepSize)); // Clamp between 1-16
        this.optimizeForPerformance = optimizeForPerformance;
        this.totalBiomeChanges = new AtomicLong(0);
        this.totalProcessingTime = new AtomicLong(0);
    }
    
    @Override
    public void change(
        final @NotNull IslandRegion islandRegion,
        final @NotNull World world,
        final @NotNull Biome biome
    ) {
        change(islandRegion, world, biome, null, null);
    }
    
    /**
     * Advanced biome changing with progress tracking and intelligent optimization
     */
    public CompletableFuture<BiomeChangeResult> change(
        final @NotNull IslandRegion islandRegion,
        final @NotNull World world,
        final @NotNull Biome biome,
        final Consumer<Double> progressCallback,
        final Runnable completionCallback
    ) {
        return CompletableFuture.supplyAsync(() -> {
            final long startTime = System.currentTimeMillis();
            final AtomicInteger processedBlocks = new AtomicInteger(0);
            final int totalBlocks = calculateTotalBlocks(islandRegion);
            
            setupCallbacks(processedBlocks, totalBlocks, progressCallback, completionCallback);
            
            if (totalBlocks > BATCH_SIZE) {
                generateBatchedBiomeWorkloads(islandRegion, world, biome, processedBlocks);
            } else {
                generateBiomeWorkloads(islandRegion, world, biome, processedBlocks);
            }
            
            final long endTime = System.currentTimeMillis();
            totalProcessingTime.addAndGet(endTime - startTime);
            totalBiomeChanges.addAndGet(processedBlocks.get());
            
            return new BiomeChangeResult(
                processedBlocks.get(),
                totalBlocks,
                endTime - startTime,
                biome,
                biomeStepSize
            );
        });
    }
    
    /**
     * Generates batched biome workloads for better performance
     */
    private void generateBatchedBiomeWorkloads(
        final @NotNull IslandRegion islandRegion,
        final @NotNull World world,
        final @NotNull Biome biome,
        final AtomicInteger processedBlocks
    ) {
        final int minX = islandRegion.getMinX();
        final int maxX = islandRegion.getMaxX();
        final int minY = islandRegion.getMinY();
        final int maxY = islandRegion.getMaxY();
        final int minZ = islandRegion.getMinZ();
        final int maxZ = islandRegion.getMaxZ();
        
        // Process in larger batches for better performance
        final int batchSize = biomeStepSize * 8;
        
        for (int batchY = minY; batchY <= maxY; batchY += batchSize) {
            final int endY = Math.min(batchY + batchSize - 1, maxY);
            
            final BatchBiomeWorkload batchWorkload = new BatchBiomeWorkload(
                world.getUID(),
                minX, maxX,
                batchY, endY,
                minZ, maxZ,
                biome,
                biomeStepSize,
                optimizeForPerformance,
                processedBlocks
            );
            
            workloadRunnable.addWorkload(batchWorkload);
        }
    }
    
    /**
     * Generates individual biome workloads
     */
    private void generateBiomeWorkloads(
        final @NotNull IslandRegion islandRegion,
        final @NotNull World world,
        final @NotNull Biome biome,
        final AtomicInteger processedBlocks
    ) {
        final int minX = islandRegion.getMinX();
        final int maxX = islandRegion.getMaxX();
        final int minY = islandRegion.getMinY();
        final int maxY = islandRegion.getMaxY();
        final int minZ = islandRegion.getMinZ();
        final int maxZ = islandRegion.getMaxZ();
        
        for (int y = minY; y <= maxY; y += biomeStepSize) {
            for (int x = minX; x <= maxX; x += biomeStepSize) {
                for (int z = minZ; z <= maxZ; z += biomeStepSize) {
                    
                    // Performance optimization: check if biome change is needed
                    if (optimizeForPerformance) {
                        try {
                            final Biome currentBiome = world.getBiome(x, y, z);
                            if (currentBiome.equals(biome)) {
                                processedBlocks.incrementAndGet();
                                continue;
                            }
                        } catch (final Exception e) {
                            // If we can't check the biome, proceed with the change
                        }
                    }
                    
                    final BiomeWorkload workload = new BiomeWorkload(
                        world.getUID(),
                        x, y, z,
                        biome,
                        processedBlocks
                    );
                    
                    workloadRunnable.addWorkload(workload);
                }
            }
        }
    }
    
    /**
     * Sets up progress and completion callbacks
     */
    private void setupCallbacks(
        final AtomicInteger processedBlocks,
        final int totalBlocks,
        final Consumer<Double> progressCallback,
        final Runnable completionCallback
    ) {
        if (progressCallback != null) {
            workloadRunnable.setOnWorkloadComplete(workload -> {
                if (workload instanceof BiomeWorkload || workload instanceof BatchBiomeWorkload) {
                    final double progress = (double) processedBlocks.get() / totalBlocks;
                    progressCallback.accept(Math.min(1.0, progress));
                }
            });
        }
        
        if (completionCallback != null) {
            workloadRunnable.setOnQueueEmpty(() -> {
                if (processedBlocks.get() >= totalBlocks) {
                    completionCallback.run();
                }
            });
        }
    }
    
    private int calculateTotalBlocks(final @NotNull IslandRegion islandRegion) {
        final int xBlocks = ((islandRegion.getMaxX() - islandRegion.getMinX()) / biomeStepSize) + 1;
        final int yBlocks = ((islandRegion.getMaxY() - islandRegion.getMinY()) / biomeStepSize) + 1;
        final int zBlocks = ((islandRegion.getMaxZ() - islandRegion.getMinZ()) / biomeStepSize) + 1;
        return xBlocks * yBlocks * zBlocks;
    }
    
    // Performance monitoring methods
    public int getQueueSize() {
        return workloadRunnable.getQueueSize();
    }
    
    public DistributedWorkloadRunnable.WorkloadStatistics getStatistics() {
        return workloadRunnable.getStatistics();
    }
    
    public boolean isProcessing() {
        return workloadRunnable.isProcessing() || !workloadRunnable.isEmpty();
    }
    
    public void clearPendingOperations() {
        workloadRunnable.clearQueue();
    }
    
    public long getTotalBiomeChanges() {
        return totalBiomeChanges.get();
    }
    
    public double getAverageProcessingTimePerChange() {
        long totalChanges = totalBiomeChanges.get();
        return totalChanges > 0 ? (double) totalProcessingTime.get() / totalChanges : 0.0;
    }
    
    // Getters
    public DistributedWorkloadRunnable getWorkloadRunnable() { return workloadRunnable; }
    public int getBiomeStepSize() { return biomeStepSize; }
    public boolean isOptimizeForPerformance() { return optimizeForPerformance; }
    
    /**
     * Result object for biome change operations
     */
    public static class BiomeChangeResult {
        private final int blocksProcessed;
        private final int totalBlocks;
        private final long processingTimeMillis;
        private final Biome biome;
        private final int stepSize;
        private final long timestamp;
        
        public BiomeChangeResult(int blocksProcessed, int totalBlocks, long processingTimeMillis, 
                               Biome biome, int stepSize) {
            this.blocksProcessed = blocksProcessed;
            this.totalBlocks = totalBlocks;
            this.processingTimeMillis = processingTimeMillis;
            this.biome = biome;
            this.stepSize = stepSize;
            this.timestamp = System.currentTimeMillis();
        }
        
        public int getBlocksProcessed() { return blocksProcessed; }
        public int getTotalBlocks() { return totalBlocks; }
        public long getProcessingTimeMillis() { return processingTimeMillis; }
        public Biome getBiome() { return biome; }
        public int getStepSize() { return stepSize; }
        public long getTimestamp() { return timestamp; }
        
        public double getCompletionPercentage() {
            return totalBlocks > 0 ? (double) blocksProcessed / totalBlocks * 100.0 : 0.0;
        }
        
        public double getBlocksPerSecond() {
            return processingTimeMillis > 0 ? (blocksProcessed * 1000.0) / processingTimeMillis : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "BiomeChangeResult{processed=%d/%d (%.1f%%), biome=%s, step=%d, time=%dms, rate=%.1f blocks/sec}",
                blocksProcessed, totalBlocks, getCompletionPercentage(), 
                biome, stepSize, processingTimeMillis, getBlocksPerSecond()
            );
        }
    }
}