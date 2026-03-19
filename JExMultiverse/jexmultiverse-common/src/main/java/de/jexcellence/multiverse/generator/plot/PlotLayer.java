package de.jexcellence.multiverse.generator.plot;

import org.bukkit.Material;

/**
 * Represents a layer in the plot world generation.
 * <p>
 * Each layer defines a material and the Y level range it occupies.
 * Layers are used to build up the plot world floor from bedrock to surface.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public record PlotLayer(Material material, int minY, int maxY) {

    /**
     * Creates a new plot layer.
     *
     * @param material the material for this layer
     * @param minY     the minimum Y level (inclusive)
     * @param maxY     the maximum Y level (inclusive)
     */
    public PlotLayer {
        if (minY > maxY) {
            throw new IllegalArgumentException("minY cannot be greater than maxY");
        }
        if (material == null) {
            throw new IllegalArgumentException("material cannot be null");
        }
    }

    /**
     * Creates a single-block layer at the specified Y level.
     *
     * @param material the material for this layer
     * @param y        the Y level
     * @return a new PlotLayer
     */
    public static PlotLayer single(Material material, int y) {
        return new PlotLayer(material, y, y);
    }

    /**
     * Creates a layer spanning multiple Y levels.
     *
     * @param material the material for this layer
     * @param minY     the minimum Y level (inclusive)
     * @param maxY     the maximum Y level (inclusive)
     * @return a new PlotLayer
     */
    public static PlotLayer range(Material material, int minY, int maxY) {
        return new PlotLayer(material, minY, maxY);
    }

    /**
     * Checks if the given Y level is within this layer's range.
     *
     * @param y the Y level to check
     * @return true if y is within [minY, maxY]
     */
    public boolean contains(int y) {
        return y >= minY && y <= maxY;
    }

    /**
     * Gets the height of this layer.
     *
     * @return the number of blocks in this layer
     */
    public int height() {
        return maxY - minY + 1;
    }
}
