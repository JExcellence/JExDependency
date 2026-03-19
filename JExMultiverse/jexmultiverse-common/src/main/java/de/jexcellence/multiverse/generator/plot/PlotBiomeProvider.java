package de.jexcellence.multiverse.generator.plot;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Biome provider for plot worlds that returns PLAINS biome for all locations.
 * <p>
 * This provider ensures that the entire plot world uses the PLAINS biome,
 * creating a consistent flat world experience suitable for building.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PlotBiomeProvider extends BiomeProvider {

    /**
     * Returns PLAINS biome for all coordinates.
     *
     * @param worldInfo the world info
     * @param x         the x coordinate
     * @param y         the y coordinate
     * @param z         the z coordinate
     * @return PLAINS biome
     */
    @Override
    public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        return Biome.PLAINS;
    }

    /**
     * Returns a list containing only PLAINS biome.
     *
     * @param worldInfo the world info
     * @return list containing PLAINS biome
     */
    @Override
    public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        return List.of(Biome.PLAINS);
    }
}
