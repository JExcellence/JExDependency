package de.jexcellence.oneblock.utility.workload.biome;

import de.jexcellence.oneblock.utility.workload.IWorkload;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Batch biome workload for processing multiple biome changes efficiently
 */
public class BatchBiomeWorkload implements IWorkload {
    
    private final UUID worldId;
    private final int minX, maxX;
    private final int minY, maxY;
    private final int minZ, maxZ;
    private final Biome targetBiome;
    private final int stepSize;
    private final boolean optimizeForPerformance;
    private final AtomicInteger processedBlocks;
    
    public BatchBiomeWorkload(
        final @NotNull UUID worldId,
        final int minX, final int maxX,
        final int minY, final int maxY,
        final int minZ, final int maxZ,
        final @NotNull Biome targetBiome,
        final int stepSize,
        final boolean optimizeForPerformance,
        final AtomicInteger processedBlocks
    ) {
        this.worldId = worldId;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.targetBiome = targetBiome;
        this.stepSize = stepSize;
        this.optimizeForPerformance = optimizeForPerformance;
        this.processedBlocks = processedBlocks;
    }
    
    @Override
    public void compute() {
        final World world = Bukkit.getWorld(worldId);
        if (world == null) {
            return;
        }
        
        int biomesChanged = 0;
        
        try {
            for (int y = minY; y <= maxY; y += stepSize) {
                for (int x = minX; x <= maxX; x += stepSize) {
                    for (int z = minZ; z <= maxZ; z += stepSize) {
                        try {
                            // Performance optimization: skip if biome is already correct
                            if (optimizeForPerformance) {
                                final Biome currentBiome = world.getBiome(x, y, z);
                                if (currentBiome.equals(targetBiome)) {
                                    biomesChanged++;
                                    continue;
                                }
                            }
                            
                            world.setBiome(x, y, z, targetBiome);
                            biomesChanged++;
                            
                        } catch (final Exception e) {
                            // Skip problematic blocks but continue processing
                            biomesChanged++; // Count as processed even if failed
                        }
                    }
                }
            }
            
        } catch (final Exception e) {
            Bukkit.getLogger().warning(String.format(
                "Error in batch biome changing for region (%d,%d,%d) to (%d,%d,%d): %s",
                minX, minY, minZ, maxX, maxY, maxZ, e.getMessage()
            ));
        } finally {
            if (processedBlocks != null) {
                processedBlocks.addAndGet(biomesChanged);
            }
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "BatchBiomeWorkload{world=%s, region=(%d,%d,%d)-(%d,%d,%d), biome=%s, step=%d}",
            worldId, minX, minY, minZ, maxX, maxY, maxZ, targetBiome, stepSize
        );
    }
}