package de.jexcellence.oneblock.utility.workload.biome;

import de.jexcellence.oneblock.database.entity.oneblock.IslandRegion;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for biome changing operations within island regions.
 */
public interface IBiomeChanger {
    
    /**
     * Changes the biome of all blocks within the specified region.
     * 
     * @param islandRegion the region to change biomes in
     * @param world the world containing the region
     * @param biome the target biome to set
     */
    void change(@NotNull IslandRegion islandRegion, @NotNull World world, @NotNull Biome biome);
}
