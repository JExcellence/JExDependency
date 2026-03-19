package de.jexcellence.oneblock.utility.workload.level;

import de.jexcellence.oneblock.database.entity.oneblock.IslandRegion;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for island level calculation operations.
 */
public interface ILevelCalculator {
    
    /**
     * Calculates the level for the specified region.
     * 
     * @param region the region to calculate level for
     * @return a CompletableFuture containing the calculated level
     */
    CompletableFuture<Long> calculateLevel(@NotNull IslandRegion region);
    
    /**
     * Gets the block counts for the specified region.
     * 
     * @param region the region to count blocks in
     * @return a CompletableFuture containing a map of materials to their counts
     */
    CompletableFuture<Map<Material, Long>> getBlockCounts(@NotNull IslandRegion region);
}
