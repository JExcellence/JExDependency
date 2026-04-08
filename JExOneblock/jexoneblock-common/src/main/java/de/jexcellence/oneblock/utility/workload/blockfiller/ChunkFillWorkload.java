package de.jexcellence.oneblock.utility.workload.blockfiller;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Workload for filling blocks within a chunk section.
 */
public class ChunkFillWorkload extends BlockFillerWorkload {
    
    public ChunkFillWorkload(
            @NotNull UUID worldUUID,
            int startX, int endX,
            int startY, int endY,
            int startZ, int endZ,
            @NotNull Material material,
            boolean applyPhysics,
            boolean optimizeForPerformance,
            @NotNull AtomicInteger processedBlocks
    ) {
        super(worldUUID, startX, endX, startY, endY, startZ, endZ, 
              material, applyPhysics, optimizeForPerformance, processedBlocks);
    }
    
    @Override
    public int getPriority() {
        return 1; // Higher priority than batch fill
    }
}
