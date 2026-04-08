package de.jexcellence.multiverse.api;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Stub interface for JExMultiverse integration when the plugin is not available.
 * This allows the OneBlock plugin to compile without requiring JExMultiverse as a dependency.
 */
public interface IMultiverseAdapter {
    
    /**
     * Gets all available worlds.
     * @return list of worlds
     */
    CompletableFuture<List<MVWorld>> getWorlds();
    
    /**
     * Gets a world by name.
     * @param worldName the world name
     * @return the world or null if not found
     */
    @Nullable
    MVWorld getWorld(@NotNull String worldName);
    
    /**
     * Checks if a world is suitable for islands.
     * @param world the world to check
     * @return true if suitable
     */
    boolean isSuitableForIslands(@NotNull World world);
    
    /**
     * Creates a new world.
     * @param worldName the world name
     * @param worldType the world type
     * @return the created world
     */
    CompletableFuture<MVWorld> createWorld(@NotNull String worldName, @NotNull String worldType);
    
    /**
     * Gets an MVWorld by name.
     * @param worldName the world name
     * @return the MVWorld or null if not found
     */
    @Nullable
    MVWorld getMVWorld(@NotNull String worldName);
    
    /**
     * Gets the global MVWorld.
     * @return the global MVWorld
     */
    @Nullable
    MVWorld getGlobalMVWorld();

    /**
     * Stub implementation of MVWorld for when JExMultiverse is not available.
     */
    interface MVWorld {
        String getIdentifier();
        Location getSpawnLocation();
        boolean isGlobalSpawn();
    }
}