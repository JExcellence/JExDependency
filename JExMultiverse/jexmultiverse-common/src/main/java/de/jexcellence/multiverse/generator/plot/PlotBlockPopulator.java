package de.jexcellence.multiverse.generator.plot;

import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Block populator for plot worlds.
 * <p>
 * This populator is intentionally empty as plot worlds should not have
 * any additional features like trees, ores, or structures generated
 * after the initial chunk generation.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PlotBlockPopulator extends BlockPopulator {

    /**
     * Does nothing - plot worlds should not have additional population.
     *
     * @param worldInfo     the world info
     * @param random        the random generator
     * @param chunkX        the chunk X coordinate
     * @param chunkZ        the chunk Z coordinate
     * @param limitedRegion the limited region for block placement
     */
    @Override
    public void populate(@NotNull WorldInfo worldInfo, @NotNull Random random,
                         int chunkX, int chunkZ, @NotNull LimitedRegion limitedRegion) {
        // Empty implementation - plot worlds have no additional population
    }
}
