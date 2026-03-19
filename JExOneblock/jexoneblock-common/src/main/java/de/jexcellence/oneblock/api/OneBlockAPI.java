package de.jexcellence.oneblock.api;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * API for accessing OneBlock player data.
 * This is a placeholder - implement with actual OneBlock logic.
 */
public final class OneBlockAPI {
    
    private OneBlockAPI() {}
    
    /**
     * Gets the evolution level for a player's island.
     *
     * @param player the player
     * @param islandId the island ID
     * @return the evolution level
     */
    public static int getEvolutionLevel(@NotNull Player player, @NotNull String islandId) {
        // TODO: Implement with actual OneBlock logic
        return 0;
    }
    
    /**
     * Gets the total number of blocks broken by a player on an island.
     *
     * @param player the player
     * @param islandId the island ID
     * @return the total blocks broken
     */
    public static long getTotalBlocksBroken(@NotNull Player player, @NotNull String islandId) {
        // TODO: Implement with actual OneBlock logic
        return 0;
    }
    
    /**
     * Gets the number of specific blocks broken by a player on an island.
     *
     * @param player the player
     * @param islandId the island ID
     * @param blockType the block type
     * @return the number of blocks broken
     */
    public static long getBlocksBroken(@NotNull Player player, @NotNull String islandId, @NotNull Material blockType) {
        // TODO: Implement with actual OneBlock logic
        return 0;
    }
    
    /**
     * Gets the prestige level for a player.
     *
     * @param player the player
     * @return the prestige level
     */
    public static int getPrestigeLevel(@NotNull Player player) {
        // TODO: Implement with actual OneBlock logic
        return 0;
    }
}
