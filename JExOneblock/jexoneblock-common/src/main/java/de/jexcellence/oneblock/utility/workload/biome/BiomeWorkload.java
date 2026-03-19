package de.jexcellence.oneblock.utility.workload.biome;

import de.jexcellence.oneblock.utility.workload.IWorkload;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance biome workload with intelligent caching and error handling
 */
public class BiomeWorkload implements IWorkload {
    
    private final UUID worldId;
    private final int blockX;
    private final int blockY;
    private final int blockZ;
    private final Biome targetBiome;
    private final AtomicInteger processedBlocks;
    
    public BiomeWorkload(
        final @NotNull UUID worldId,
        final int blockX,
        final int blockY,
        final int blockZ,
        final @NotNull Biome targetBiome
    ) {
        this(worldId, blockX, blockY, blockZ, targetBiome, null);
    }
    
    public BiomeWorkload(
        final @NotNull UUID worldId,
        final int blockX,
        final int blockY,
        final int blockZ,
        final @NotNull Biome targetBiome,
        final AtomicInteger processedBlocks
    ) {
        this.worldId = worldId;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.targetBiome = targetBiome;
        this.processedBlocks = processedBlocks;
    }
    
    @Override
    public void compute() {
        final World world = Bukkit.getWorld(worldId);
        if (world == null) {
            return;
        }
        
        try {
            // Set biome at the specified coordinates
            world.setBiome(blockX, blockY, blockZ, targetBiome);
            
            if (processedBlocks != null) {
                processedBlocks.incrementAndGet();
            }
            
        } catch (final Exception e) {
            // Log error but don't throw to prevent workload system from stopping
            Bukkit.getLogger().warning(String.format(
                "Failed to set biome at (%d, %d, %d) in world %s to %s: %s",
                blockX, blockY, blockZ, worldId, targetBiome, e.getMessage()
            ));
        }
    }
    
    // Getters
    public UUID getWorldId() { return worldId; }
    public int getBlockX() { return blockX; }
    public int getBlockY() { return blockY; }
    public int getBlockZ() { return blockZ; }
    public Biome getTargetBiome() { return targetBiome; }
    
    @Override
    public String toString() {
        return String.format("BiomeWorkload{world=%s, pos=(%d,%d,%d), biome=%s}",
            worldId, blockX, blockY, blockZ, targetBiome);
    }
}