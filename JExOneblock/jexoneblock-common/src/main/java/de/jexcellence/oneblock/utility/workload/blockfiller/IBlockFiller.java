package de.jexcellence.oneblock.utility.workload.blockfiller;

import de.jexcellence.oneblock.database.entity.oneblock.IslandRegion;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for block filling operations within island regions.
 */
public interface IBlockFiller {
    
    /**
     * Fills the specified region with the given material.
     * 
     * @param region the region to fill
     * @param world the world containing the region
     * @param material the material to fill with
     * @return a CompletableFuture that completes when the operation is done
     */
    CompletableFuture<Void> fill(@NotNull IslandRegion region, @NotNull World world, @NotNull Material material);
    
    /**
     * Clears all blocks in the specified region (fills with air).
     * 
     * @param region the region to clear
     * @param world the world containing the region
     * @return a CompletableFuture that completes when the operation is done
     */
    default CompletableFuture<Void> clear(@NotNull IslandRegion region, @NotNull World world) {
        return fill(region, world, Material.AIR);
    }
}
