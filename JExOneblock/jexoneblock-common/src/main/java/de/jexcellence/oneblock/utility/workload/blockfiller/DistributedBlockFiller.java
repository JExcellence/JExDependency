package de.jexcellence.oneblock.utility.workload.blockfiller;

import de.jexcellence.oneblock.database.entity.oneblock.IslandRegion;
import de.jexcellence.oneblock.utility.workload.DistributedWorkloadRunnable;
import de.jexcellence.oneblock.utility.workload.IWorkload;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * High-performance distributed block filler with intelligent chunk handling and adaptive batching
 */
public class DistributedBlockFiller implements IBlockFiller {
    
    private final DistributedWorkloadRunnable workloadRunnable;
    private final boolean optimizeForPerformance;
    private final boolean applyPhysics;
    private final Logger logger;
    
    // Performance constants
    private static final int CHUNK_BATCH_SIZE = 4;
    private static final int AIR_OPTIMIZATION_THRESHOLD = 10000;
    
    public DistributedBlockFiller(final @NotNull DistributedWorkloadRunnable workloadRunnable) {
        this(workloadRunnable, true, false);
    }
    
    public DistributedBlockFiller(
        final @NotNull DistributedWorkloadRunnable workloadRunnable,
        final boolean optimizeForPerformance,
        final boolean applyPhysics
    ) {
        this.workloadRunnable = workloadRunnable;
        this.optimizeForPerformance = optimizeForPerformance;
        this.applyPhysics = applyPhysics;
        this.logger = Logger.getLogger(DistributedBlockFiller.class.getName());
    }
    
    @Override
    public CompletableFuture<Void> fill(
        final @NotNull IslandRegion region,
        final @NotNull World world,
        final @NotNull Material material
    ) {
        return fill(region, world, material, null, null);
    }
    
    /**
     * Advanced fill with progress tracking and intelligent optimization
     */
    public CompletableFuture<Void> fill(
        final @NotNull IslandRegion region,
        final @NotNull World world,
        final @NotNull Material material,
        final Consumer<Double> progressCallback,
        final Runnable completionCallback
    ) {
        return CompletableFuture.runAsync(() -> {
            final int totalBlocks = calculateTotalBlocks(region);
            final AtomicInteger processedBlocks = new AtomicInteger(0);
            
            setupCallbacks(processedBlocks, totalBlocks, progressCallback, completionCallback);
            
            if (material == Material.AIR && totalBlocks > AIR_OPTIMIZATION_THRESHOLD) {
                fillWithAirOptimized(region, world, processedBlocks);
            } else if (shouldUseChunkOptimization(region)) {
                fillByChunks(region, world, material, processedBlocks);
            } else {
                fillSequentially(region, world, material, processedBlocks);
            }
        });
    }
    
    /**
     * Optimized air filling using chunk-based clearing
     */
    private void fillWithAirOptimized(
        final @NotNull IslandRegion region,
        final @NotNull World world,
        final AtomicInteger processedBlocks
    ) {
        final int chunkXStart = region.getMinX() >> 4;
        final int chunkXEnd = region.getMaxX() >> 4;
        final int chunkZStart = region.getMinZ() >> 4;
        final int chunkZEnd = region.getMaxZ() >> 4;
        
        // Process chunks in batches for better performance
        for (int chunkX = chunkXStart; chunkX <= chunkXEnd; chunkX += CHUNK_BATCH_SIZE) {
            for (int chunkZ = chunkZStart; chunkZ <= chunkZEnd; chunkZ += CHUNK_BATCH_SIZE) {
                final int endChunkX = Math.min(chunkX + CHUNK_BATCH_SIZE - 1, chunkXEnd);
                final int endChunkZ = Math.min(chunkZ + CHUNK_BATCH_SIZE - 1, chunkZEnd);
                
                final ChunkClearWorkload workload = new ChunkClearWorkload(
                    world.getUID(),
                    chunkX, endChunkX,
                    chunkZ, endChunkZ,
                    region,
                    processedBlocks
                );
                
                workloadRunnable.addWorkload(workload);
            }
        }
    }
    
    /**
     * Chunk-based filling for better performance
     */
    private void fillByChunks(
        final @NotNull IslandRegion region,
        final @NotNull World world,
        final @NotNull Material material,
        final AtomicInteger processedBlocks
    ) {
        final int chunkXStart = region.getMinX() >> 4;
        final int chunkXEnd = region.getMaxX() >> 4;
        final int chunkZStart = region.getMinZ() >> 4;
        final int chunkZEnd = region.getMaxZ() >> 4;
        
        for (int chunkX = chunkXStart; chunkX <= chunkXEnd; chunkX++) {
            for (int chunkZ = chunkZStart; chunkZ <= chunkZEnd; chunkZ++) {
                generateChunkWorkloads(world, chunkX, chunkZ, region, material, processedBlocks);
            }
        }
    }
    
    /**
     * Generate workloads for a specific chunk
     */
    private void generateChunkWorkloads(
        final @NotNull World world,
        final int chunkX,
        final int chunkZ,
        final @NotNull IslandRegion region,
        final @NotNull Material material,
        final AtomicInteger processedBlocks
    ) {
        try {
            final int chunkWorldX = chunkX << 4;
            final int chunkWorldZ = chunkZ << 4;
            
            // Calculate intersection of chunk with region
            final int startX = Math.max(0, region.getMinX() - chunkWorldX);
            final int endX = Math.min(15, region.getMaxX() - chunkWorldX);
            final int startZ = Math.max(0, region.getMinZ() - chunkWorldZ);
            final int endZ = Math.min(15, region.getMaxZ() - chunkWorldZ);
            final int startY = Math.max(world.getMinHeight(), region.getMinY());
            final int endY = Math.min(world.getMaxHeight() - 1, region.getMaxY());
            
            // Create batch workload for this chunk section
            final ChunkFillWorkload workload = new ChunkFillWorkload(
                world.getUID(),
                chunkWorldX + startX, chunkWorldX + endX,
                startY, endY,
                chunkWorldZ + startZ, chunkWorldZ + endZ,
                material,
                applyPhysics,
                optimizeForPerformance,
                processedBlocks
            );
            
            workloadRunnable.addWorkload(workload);
            
        } catch (final Exception e) {
            logger.warning(String.format(
                "Failed to generate workloads for chunk (%d, %d): %s",
                chunkX, chunkZ, e.getMessage()
            ));
        }
    }
    
    /**
     * Sequential filling for smaller regions
     */
    private void fillSequentially(
        final @NotNull IslandRegion region,
        final @NotNull World world,
        final @NotNull Material material,
        final AtomicInteger processedBlocks
    ) {
        final int batchSize = calculateOptimalBatchSize(region);
        final int minX = region.getMinX();
        final int maxX = region.getMaxX();
        final int minY = region.getMinY();
        final int maxY = region.getMaxY();
        final int minZ = region.getMinZ();
        final int maxZ = region.getMaxZ();
        
        // Process in batches for better performance
        for (int y = minY; y <= maxY; y += batchSize) {
            for (int x = minX; x <= maxX; x += batchSize) {
                for (int z = minZ; z <= maxZ; z += batchSize) {
                    final int endY = Math.min(y + batchSize - 1, maxY);
                    final int endX = Math.min(x + batchSize - 1, maxX);
                    final int endZ = Math.min(z + batchSize - 1, maxZ);
                    
                    final BatchFillWorkload workload = new BatchFillWorkload(
                        world.getUID(),
                        x, endX, y, endY, z, endZ,
                        material,
                        applyPhysics,
                        optimizeForPerformance,
                        processedBlocks
                    );
                    
                    workloadRunnable.addWorkload(workload);
                }
            }
        }
    }
    
    /**
     * Calculate optimal batch size based on region size
     */
    private int calculateOptimalBatchSize(final @NotNull IslandRegion region) {
        final int volume = calculateTotalBlocks(region);
        
        if (volume > 100000) return 8;
        if (volume > 50000) return 6;
        if (volume > 10000) return 4;
        return 2;
    }
    
    /**
     * Determine if chunk optimization should be used
     */
    private boolean shouldUseChunkOptimization(final @NotNull IslandRegion region) {
        final int chunkCount = ((region.getMaxX() >> 4) - (region.getMinX() >> 4) + 1) *
                              ((region.getMaxZ() >> 4) - (region.getMinZ() >> 4) + 1);
        return chunkCount >= 4; // Use chunk optimization for 4+ chunks
    }
    
    private void setupCallbacks(
        final AtomicInteger processedBlocks,
        final int totalBlocks,
        final Consumer<Double> progressCallback,
        final Runnable completionCallback
    ) {
        if (progressCallback != null) {
            workloadRunnable.setOnWorkloadComplete(workload -> {
                if (workload instanceof BlockFillerWorkload || 
                    workload instanceof ChunkFillWorkload || 
                    workload instanceof BatchFillWorkload) {
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
    
    private int calculateTotalBlocks(final @NotNull IslandRegion region) {
        final int xBlocks = (region.getMaxX() - region.getMinX()) + 1;
        final int yBlocks = (region.getMaxY() - region.getMinY()) + 1;
        final int zBlocks = (region.getMaxZ() - region.getMinZ()) + 1;
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
    
    // Getters
    public DistributedWorkloadRunnable getWorkloadRunnable() { return workloadRunnable; }
    public boolean isOptimizeForPerformance() { return optimizeForPerformance; }
    public boolean isApplyPhysics() { return applyPhysics; }
}