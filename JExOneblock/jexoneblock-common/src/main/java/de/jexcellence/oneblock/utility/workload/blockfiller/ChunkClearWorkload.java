package de.jexcellence.oneblock.utility.workload.blockfiller;

import de.jexcellence.oneblock.database.entity.oneblock.IslandRegion;
import de.jexcellence.oneblock.utility.workload.IWorkload;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Workload for clearing chunks (filling with air) in a region.
 */
public class ChunkClearWorkload implements IWorkload {
    
    private final UUID worldUUID;
    private final int startChunkX, endChunkX;
    private final int startChunkZ, endChunkZ;
    private final IslandRegion region;
    private final AtomicInteger processedBlocks;
    private final AtomicBoolean complete = new AtomicBoolean(false);
    
    public ChunkClearWorkload(
            @NotNull UUID worldUUID,
            int startChunkX, int endChunkX,
            int startChunkZ, int endChunkZ,
            @NotNull IslandRegion region,
            @NotNull AtomicInteger processedBlocks
    ) {
        this.worldUUID = worldUUID;
        this.startChunkX = startChunkX;
        this.endChunkX = endChunkX;
        this.startChunkZ = startChunkZ;
        this.endChunkZ = endChunkZ;
        this.region = region;
        this.processedBlocks = processedBlocks;
    }
    
    @Override
    public void compute() {
        World world = Bukkit.getWorld(worldUUID);
        if (world == null) {
            complete.set(true);
            return;
        }
        
        int count = 0;
        for (int chunkX = startChunkX; chunkX <= endChunkX; chunkX++) {
            for (int chunkZ = startChunkZ; chunkZ <= endChunkZ; chunkZ++) {
                count += clearChunk(world, chunkX, chunkZ);
            }
        }
        
        processedBlocks.addAndGet(count);
        complete.set(true);
    }
    
    private int clearChunk(World world, int chunkX, int chunkZ) {
        int count = 0;
        int chunkWorldX = chunkX << 4;
        int chunkWorldZ = chunkZ << 4;
        
        int startX = Math.max(0, region.getMinX() - chunkWorldX);
        int endX = Math.min(15, region.getMaxX() - chunkWorldX);
        int startZ = Math.max(0, region.getMinZ() - chunkWorldZ);
        int endZ = Math.min(15, region.getMaxZ() - chunkWorldZ);
        int startY = Math.max(world.getMinHeight(), region.getMinY());
        int endY = Math.min(world.getMaxHeight() - 1, region.getMaxY());
        
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                for (int z = startZ; z <= endZ; z++) {
                    Block block = world.getBlockAt(chunkWorldX + x, y, chunkWorldZ + z);
                    if (block.getType() != Material.AIR) {
                        block.setType(Material.AIR, false);
                        count++;
                    }
                }
            }
        }
        
        return count;
    }
    
    @Override
    public boolean isComplete() {
        return complete.get();
    }
    
    @Override
    public int getPriority() {
        return 2; // Highest priority for clearing
    }
}
