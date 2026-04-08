package de.jexcellence.multiverse.generator.void_world;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Biome provider for void worlds that returns THE_VOID biome for all locations.
 * <p>
 * This provider ensures that the entire void world uses the THE_VOID biome,
 * creating a consistent empty world experience.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class VoidBiomeProvider extends BiomeProvider {

    /**
     * Returns THE_VOID biome for all coordinates.
     *
     * @param worldInfo the world info
     * @param x         the x coordinate
     * @param y         the y coordinate
     * @param z         the z coordinate
     * @return THE_VOID biome
     */
    @Override
    public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        return Biome.THE_VOID;
    }

    /**
     * Returns a list containing only THE_VOID biome.
     *
     * @param worldInfo the world info
     * @return list containing THE_VOID biome
     */
    @Override
    public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        return List.of(Biome.THE_VOID);
    }
}
