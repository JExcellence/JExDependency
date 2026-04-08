package de.jexcellence.oneblock.adapter;

import de.jexcellence.multiverse.api.IMultiverseAdapter;
import de.jexcellence.multiverse.api.IMultiverseAdapter.MVWorld;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Adapter for JExMultiverse integration following RDQ patterns.
 * Provides safe access to multiverse functionality with fallback behavior.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
public class MultiverseAdapter {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final IMultiverseAdapter multiverseService;
    /**
     * -- GETTER --
     *  Checks if JExMultiverse is available and loaded.
     *
     * @return true if multiverse functionality is available
     */
    @Getter
    private final boolean isAvailable;
    
    /**
     * Creates a new MultiverseAdapter.
     * 
     * @param multiverseService the multiverse service instance, can be null
     */
    public MultiverseAdapter(@Nullable IMultiverseAdapter multiverseService) {
        this.multiverseService = multiverseService;
        this.isAvailable = multiverseService != null;
        
        if (isAvailable) {
            LOGGER.info("JExMultiverse integration enabled");
        } else {
            LOGGER.info("JExMultiverse not available - using fallback behavior");
        }
    }

    /**
     * Gets all available worlds from JExMultiverse.
     * 
     * @return CompletableFuture with list of worlds, empty if not available
     */
    @NotNull
    public CompletableFuture<List<MVWorld>> getWorlds() {
        if (!isAvailable) {
            return CompletableFuture.completedFuture(List.of());
        }
        
        return multiverseService.getWorlds()
            .exceptionally(throwable -> {
                LOGGER.warning("Failed to get worlds from JExMultiverse: " + throwable.getMessage());
                return List.of();
            });
    }
    
    /**
     * Gets a world by name from JExMultiverse.
     * 
     * @param worldName the world name
     * @return the MVWorld or null if not found or not available
     */
    @Nullable
    public MVWorld getWorld(@NotNull String worldName) {
        if (!isAvailable) {
            return null;
        }
        
        try {
            return multiverseService.getWorld(worldName);
        } catch (Exception e) {
            LOGGER.warning("Failed to get world '" + worldName + "' from JExMultiverse: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets an MVWorld by name from JExMultiverse.
     * 
     * @param worldName the world name
     * @return the MVWorld or null if not found or not available
     */
    @Nullable
    public MVWorld getMVWorld(@NotNull String worldName) {
        if (!isAvailable) {
            return null;
        }
        
        try {
            return multiverseService.getMVWorld(worldName);
        } catch (Exception e) {
            LOGGER.warning("Failed to get MVWorld '" + worldName + "' from JExMultiverse: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets the global MVWorld from JExMultiverse.
     * 
     * @return the global MVWorld or null if not available
     */
    @Nullable
    public MVWorld getGlobalMVWorld() {
        if (!isAvailable) {
            return null;
        }
        
        try {
            return multiverseService.getGlobalMVWorld();
        } catch (Exception e) {
            LOGGER.warning("Failed to get global MVWorld from JExMultiverse: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Checks if a world is suitable for islands using JExMultiverse.
     * 
     * @param world the world to check
     * @return true if suitable, false if not suitable or not available
     */
    public boolean isSuitableForIslands(@NotNull World world) {
        if (!isAvailable) {
            // Fallback: check if it's a void/flat world
            return isVoidWorld(world);
        }
        
        try {
            return multiverseService.isSuitableForIslands(world);
        } catch (Exception e) {
            LOGGER.warning("Failed to check if world '" + world.getName() + "' is suitable for islands: " + e.getMessage());
            return isVoidWorld(world); // Fallback
        }
    }
    
    /**
     * Creates a new world using JExMultiverse.
     * 
     * @param worldName the world name
     * @param worldType the world type
     * @return CompletableFuture with the created world, or failed future if not available
     */
    @NotNull
    public CompletableFuture<MVWorld> createWorld(@NotNull String worldName, @NotNull String worldType) {
        if (!isAvailable) {
            return CompletableFuture.failedFuture(
                new UnsupportedOperationException("JExMultiverse not available - cannot create worlds")
            );
        }
        
        return multiverseService.createWorld(worldName, worldType)
            .exceptionally(throwable -> {
                LOGGER.warning("Failed to create world '" + worldName + "' with JExMultiverse: " + throwable.getMessage());
                throw new RuntimeException("World creation failed", throwable);
            });
    }
    
    /**
     * Teleports a player to a world's spawn using JExMultiverse if available.
     * 
     * @param player the player to teleport
     * @param worldName the target world name
     * @return CompletableFuture with teleport result
     */
    @NotNull
    public CompletableFuture<Boolean> teleportToWorld(@NotNull Player player, @NotNull String worldName) {
        return CompletableFuture.supplyAsync(() -> {
            MVWorld mvWorld = getMVWorld(worldName);
            if (mvWorld != null) {
                Location spawnLocation = mvWorld.getSpawnLocation();
                if (spawnLocation != null && spawnLocation.getWorld() != null) {
                    return player.teleport(spawnLocation);
                }
            }
            
            // Fallback: use Bukkit world spawn
            World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world != null) {
                return player.teleport(world.getSpawnLocation());
            }
            
            return false;
        });
    }
    
    /**
     * Gets the spawn location for a world, preferring JExMultiverse data.
     * 
     * @param worldName the world name
     * @return the spawn location or null if not found
     */
    @Nullable
    public Location getWorldSpawn(@NotNull String worldName) {
        MVWorld mvWorld = getMVWorld(worldName);
        if (mvWorld != null) {
            Location spawnLocation = mvWorld.getSpawnLocation();
            if (spawnLocation != null) {
                return spawnLocation;
            }
        }
        
        // Fallback: use Bukkit world spawn
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world != null) {
            return world.getSpawnLocation();
        }
        
        return null;
    }
    
    /**
     * Checks if JExMultiverse has a global spawn configured.
     * 
     * @return true if global spawn is available
     */
    public boolean hasGlobalSpawn() {
        return getGlobalMVWorld() != null;
    }
    
    /**
     * Teleports a player to the global spawn if available.
     * 
     * @param player the player to teleport
     * @return CompletableFuture with teleport result
     */
    @NotNull
    public CompletableFuture<Boolean> teleportToGlobalSpawn(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            MVWorld globalWorld = getGlobalMVWorld();
            if (globalWorld != null) {
                Location spawnLocation = globalWorld.getSpawnLocation();
                if (spawnLocation != null && spawnLocation.getWorld() != null) {
                    return player.teleport(spawnLocation);
                }
            }
            
            // Fallback: teleport to main world spawn
            World mainWorld = org.bukkit.Bukkit.getWorlds().get(0);
            return player.teleport(mainWorld.getSpawnLocation());
        });
    }
    
    /**
     * Gets information about the multiverse integration status.
     * 
     * @return status information string
     */
    @NotNull
    public String getStatus() {
        if (isAvailable) {
            return "JExMultiverse integration active";
        } else {
            return "JExMultiverse not available - using fallback behavior";
        }
    }
    
    /**
     * Fallback method to check if a world is void-like.
     * Used when JExMultiverse is not available.
     * 
     * @param world the world to check
     * @return true if it appears to be a void world
     */
    private boolean isVoidWorld(@NotNull World world) {
        // Check world generator
        String generator = world.getGenerator() != null ? 
            world.getGenerator().getClass().getSimpleName() : "default";
        
        return generator.toLowerCase().contains("void") || 
               generator.toLowerCase().contains("empty") ||
               world.getName().toLowerCase().contains("flat") ||
               world.getName().toLowerCase().contains("void");
    }
}