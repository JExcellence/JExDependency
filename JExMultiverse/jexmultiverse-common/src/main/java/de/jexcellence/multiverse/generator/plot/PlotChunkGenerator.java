package de.jexcellence.multiverse.generator.plot;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

/**
 * Chunk generator for plot worlds that generates a grid-based layout.
 * <p>
 * This generator creates a world with:
 * <ul>
 *   <li>Configurable plot sizes</li>
 *   <li>Roads between plots</li>
 *   <li>Borders around plots</li>
 *   <li>Layered floor construction</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PlotChunkGenerator extends ChunkGenerator {

    /**
     * Default plot size (width/length in blocks).
     */
    public static final int DEFAULT_PLOT_SIZE = 42;

    /**
     * Default road width in blocks.
     */
    public static final int DEFAULT_ROAD_WIDTH = 7;

    /**
     * Default floor Y level.
     */
    public static final int DEFAULT_FLOOR_Y = 64;

    /**
     * Default spawn Y level (one above floor).
     */
    public static final int DEFAULT_SPAWN_Y = 65;

    private final int plotSize;
    private final int roadWidth;
    private final int floorY;
    private final int totalSize;

    private final Material roadMaterial;
    private final Material borderMaterial;
    private final Material floorMaterial;
    private final List<PlotLayer> layers;

    private final PlotBiomeProvider biomeProvider;
    private final PlotBlockPopulator blockPopulator;

    /**
     * Creates a new plot chunk generator with default settings.
     */
    public PlotChunkGenerator() {
        this(DEFAULT_PLOT_SIZE, DEFAULT_ROAD_WIDTH, DEFAULT_FLOOR_Y,
             Material.STONE, Material.STONE_BRICKS, Material.GRASS_BLOCK);
    }


    /**
     * Creates a new plot chunk generator with custom settings.
     *
     * @param plotSize       the size of each plot (width/length)
     * @param roadWidth      the width of roads between plots
     * @param floorY         the Y level of the floor
     * @param roadMaterial   the material for roads
     * @param borderMaterial the material for plot borders
     * @param floorMaterial  the material for plot floors
     */
    public PlotChunkGenerator(int plotSize, int roadWidth, int floorY,
                              Material roadMaterial, Material borderMaterial, Material floorMaterial) {
        this.plotSize = plotSize;
        this.roadWidth = roadWidth;
        this.floorY = floorY;
        this.totalSize = plotSize + roadWidth;
        this.roadMaterial = roadMaterial;
        this.borderMaterial = borderMaterial;
        this.floorMaterial = floorMaterial;

        this.layers = List.of(
            PlotLayer.single(Material.BEDROCK, -64),
            PlotLayer.range(Material.STONE, -63, floorY - 2),
            PlotLayer.single(Material.DIRT, floorY - 1)
        );

        this.biomeProvider = new PlotBiomeProvider();
        this.blockPopulator = new PlotBlockPopulator();
    }

    /**
     * Generates the terrain for a chunk in the plot world.
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
        int startX = chunkX << 4;
        int startZ = chunkZ << 4;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;

                generateColumn(chunkData, x, z, worldX, worldZ);
            }
        }
    }

    /**
     * Generates a single column of blocks at the given coordinates.
     */
    private void generateColumn(ChunkData chunkData, int localX, int localZ, int worldX, int worldZ) {
        // Generate base layers
        for (PlotLayer layer : layers) {
            for (int y = layer.minY(); y <= layer.maxY(); y++) {
                chunkData.setBlock(localX, y, localZ, layer.material());
            }
        }

        // Determine what type of block this column should have at floor level
        Material surfaceMaterial = getSurfaceMaterial(worldX, worldZ);
        chunkData.setBlock(localX, floorY, localZ, surfaceMaterial);
    }

    /**
     * Determines the surface material based on world coordinates.
     * This creates the grid pattern of plots, roads, and borders.
     */
    private Material getSurfaceMaterial(int worldX, int worldZ) {
        // Normalize coordinates to handle negative values correctly
        int normalizedX = Math.floorMod(worldX, totalSize);
        int normalizedZ = Math.floorMod(worldZ, totalSize);

        // Check if we're in the road area
        boolean inRoadX = normalizedX >= plotSize;
        boolean inRoadZ = normalizedZ >= plotSize;

        if (inRoadX || inRoadZ) {
            return roadMaterial;
        }

        // Check if we're on the border (first or last block of plot)
        boolean onBorderX = normalizedX == 0 || normalizedX == plotSize - 1;
        boolean onBorderZ = normalizedZ == 0 || normalizedZ == plotSize - 1;

        if (onBorderX || onBorderZ) {
            return borderMaterial;
        }

        // Inside the plot
        return floorMaterial;
    }

    /**
     * Returns the fixed spawn location for plot worlds.
     *
     * @param world  the world
     * @param random the random generator
     * @return the spawn location at the center of the first plot
     */
    @Override
    public @Nullable Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        // Spawn at the center of the first plot
        int centerOffset = plotSize / 2;
        return new Location(world, centerOffset + 0.5, floorY + 1, centerOffset + 0.5);
    }

    /**
     * Returns the plot biome provider.
     *
     * @param worldInfo the world info
     * @return the plot biome provider
     */
    @Override
    public @Nullable BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return biomeProvider;
    }

    /**
     * Returns the block populators for this generator.
     *
     * @param worldInfo the world info
     * @return list containing the plot block populator
     */
    @Override
    public @NotNull List<BlockPopulator> getDefaultPopulators(@NotNull World worldInfo) {
        return List.of(blockPopulator);
    }

    /**
     * Disables cave generation.
     */
    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    /**
     * Disables decoration generation.
     */
    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    /**
     * Disables mob spawning.
     */
    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    /**
     * Disables structure generation.
     */
    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    // Getters for configuration values

    public int getPlotSize() {
        return plotSize;
    }

    public int getRoadWidth() {
        return roadWidth;
    }

    public int getFloorY() {
        return floorY;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public Material getRoadMaterial() {
        return roadMaterial;
    }

    public Material getBorderMaterial() {
        return borderMaterial;
    }

    public Material getFloorMaterial() {
        return floorMaterial;
    }
}
