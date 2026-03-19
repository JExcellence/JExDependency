package de.jexcellence.multiverse.type;

/**
 * Defines the available world generation types for JExMultiverse.
 * <p>
 * Each type corresponds to a different world generation strategy:
 * <ul>
 *   <li>{@link #DEFAULT} - Vanilla Minecraft world generation</li>
 *   <li>{@link #VOID} - Empty void world with no terrain</li>
 *   <li>{@link #PLOT} - Grid-based plot world with configurable parameters</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public enum MVWorldType {

    /**
     * Vanilla Minecraft world generation.
     * Uses the default terrain generator for the specified environment.
     */
    DEFAULT,

    /**
     * Empty void world with no terrain.
     * Generates a completely empty world with THE_VOID biome.
     */
    VOID,

    /**
     * Grid-based plot world.
     * Generates a world with configurable plot sizes, roads, and borders.
     */
    PLOT
}
