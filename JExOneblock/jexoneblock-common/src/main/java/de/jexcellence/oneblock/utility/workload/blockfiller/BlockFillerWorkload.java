package de.jexcellence.oneblock.utility.workload.blockfiller;

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
 * Base workload for block filling operations.
 */
public class BlockFillerWorkload implements IWorkload {
    
    protected final UUID worldUUID;
    protected final int startX, endX;
    protected final int startY, endY;
    protected final int startZ, endZ;
    protected final Material material;
    protected final boolean applyPhysics;
    protected final boolean optimizeForPerformance;
    protected final AtomicInteger processedBlocks;
    protected final AtomicBoolean complete = new AtomicBoolean(false);
    
    public BlockFillerWorkload(
            @NotNull UUID worldUUID,
            int startX, int endX,
            int startY, int endY,
            int startZ, int endZ,
            @NotNull Material material,
            boolean applyPhysics,
            boolean optimizeForPerformance,
            @NotNull AtomicInteger processedBlocks
    ) {
        this.worldUUID = worldUUID;
        this.startX = startX;
        this.endX = endX;
        this.startY = startY;
        this.endY = endY;
        this.startZ = startZ;
        this.endZ = endZ;
        this.material = material;
        this.applyPhysics = applyPhysics;
        this.optimizeForPerformance = optimizeForPerformance;
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
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                for (int z = startZ; z <= endZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (optimizeForPerformance && block.getType() == material) {
                        continue;
                    }
                    block.setType(material, applyPhysics);
                    count++;
                }
            }
        }
        
        processedBlocks.addAndGet(count);
        complete.set(true);
    }
    
    @Override
    public boolean isComplete() {
        return complete.get();
    }
}
