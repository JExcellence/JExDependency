package de.jexcellence.multiverse.generator.void_world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Chunk generator for void worlds that generates completely empty chunks.
 * <p>
 * This generator creates an empty world with no terrain, structures, or features.
 * The spawn location is fixed at y=96 to provide a safe spawn point in the void.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class VoidChunkGenerator extends ChunkGenerator {

    /**
     * The fixed spawn Y coordinate for void worlds.
     */
    public static final int SPAWN_Y = 96;

    private final VoidBiomeProvider biomeProvider;

    /**
     * Creates a new void chunk generator.
     */
    public VoidChunkGenerator() {
        this.biomeProvider = new VoidBiomeProvider();
    }

    /**
     * Generates chunk data. For void worlds, this does nothing as chunks should be empty.
     *
     * @param worldInfo the world info
     * @param random    the random generator
     * @param chunkX    the chunk X coordinate
     * @param chunkZ    the chunk Z coordinate
     * @param chunkData the chunk data to populate
     */
    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random,
                              int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        // Empty implementation - void world has no terrain
    }

    /**
     * Returns the fixed spawn location for void worlds at y=96.
     *
     * @param world  the world
     * @param random the random generator
     * @return the spawn location at (0, 96, 0)
     */
    @Override
    public @Nullable Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        return new Location(world, 0.5, SPAWN_Y, 0.5);
    }

    /**
     * Returns the void biome provider.
     *
     * @param worldInfo the world info
     * @return the void biome provider
     */
    @Override
    public @Nullable BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return biomeProvider;
    }

    /**
     * Disables cave generation.
     *
     * @return false
     */
    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    /**
     * Disables decoration generation.
     *
     * @return false
     */
    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    /**
     * Disables mob spawning.
     *
     * @return false
     */
    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    /**
     * Disables structure generation.
     *
     * @return false
     */
    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    /**
     * Disables surface generation.
     *
     * @return false
     */
    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    /**
     * Disables bedrock generation.
     *
     * @return false
     */
    @Override
    public boolean shouldGenerateBedrock() {
        return false;
    }

    /**
     * Disables noise generation.
     *
     * @return false
     */
    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }
}
