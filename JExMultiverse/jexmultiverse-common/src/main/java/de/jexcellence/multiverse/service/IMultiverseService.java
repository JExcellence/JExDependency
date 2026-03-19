package de.jexcellence.multiverse.service;

import de.jexcellence.multiverse.database.entity.MVWorld;
import de.jexcellence.multiverse.type.MVWorldType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for multiverse operations following the IHomeService pattern.
 * <p>
 * Provides async methods for all world CRUD operations, spawn management, and edition features.
 * Implementations include FreeMultiverseService (limited) and PremiumMultiverseService (full features).
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public interface IMultiverseService {

    // ==================== Edition Detection ====================

    /**
     * Checks if this is the premium version.
     *
     * @return true if premium, false if free
     */
    boolean isPremium();

    // ==================== World Limits ====================

    /**
     * Gets the maximum number of worlds allowed.
     *
     * @return the maximum number of worlds, or -1 for unlimited
     */
    int getMaxWorlds();

    /**
     * Gets the number of available world types.
     * <p>
     * Free edition may have limited world types (e.g., DEFAULT and VOID only),
     * while premium has access to all types including PLOT.
     * </p>
     *
     * @return the number of available world types
     */
    int getMaxWorldTypes();

    /**
     * Gets the list of available world types for this edition.
     *
     * @return list of available world types
     */
    @NotNull
    List<MVWorldType> getAvailableWorldTypes();

    /**
     * Checks if a world type is available in this edition.
     *
     * @param type the world type to check
     * @return true if the type is available
     */
    boolean isWorldTypeAvailable(@NotNull MVWorldType type);

    // ==================== World CRUD Operations ====================

    /**
     * Creates a new world with the specified parameters.
     *
     * @param identifier  the unique world identifier (name)
     * @param environment the world environment (NORMAL, NETHER, THE_END)
     * @param type        the world generation type
     * @param creator     the player creating the world (for logging/permissions)
     * @return a CompletableFuture containing the created MVWorld
     */
    @NotNull
    CompletableFuture<MVWorld> createWorld(
            @NotNull String identifier,
            @NotNull World.Environment environment,
            @NotNull MVWorldType type,
            @NotNull Player creator
    );

    /**
     * Deletes a world by its identifier.
     * <p>
     * This operation will:
     * <ul>
     *   <li>Unload the world from the server</li>
     *   <li>Remove the database entry</li>
     *   <li>Optionally delete world files</li>
     * </ul>
     * </p>
     *
     * @param identifier the world identifier
     * @param deleter    the player deleting the world (for logging/permissions)
     * @return a CompletableFuture containing true if deleted successfully
     */
    @NotNull
    CompletableFuture<Boolean> deleteWorld(@NotNull String identifier, @NotNull Player deleter);

    /**
     * Gets a world by its identifier.
     *
     * @param identifier the world identifier
     * @return a CompletableFuture containing an Optional with the world if found
     */
    @NotNull
    CompletableFuture<Optional<MVWorld>> getWorld(@NotNull String identifier);

    /**
     * Gets all managed worlds.
     *
     * @return a CompletableFuture containing a list of all worlds
     */
    @NotNull
    CompletableFuture<List<MVWorld>> getAllWorlds();

    /**
     * Updates an existing world entity.
     *
     * @param world the world to update
     * @return a CompletableFuture containing the updated world
     */
    @NotNull
    CompletableFuture<MVWorld> updateWorld(@NotNull MVWorld world);

    // ==================== Spawn Management ====================

    /**
     * Sets the spawn location for a world.
     *
     * @param world    the world to update
     * @param location the new spawn location
     * @return a CompletableFuture containing true if successful
     */
    @NotNull
    CompletableFuture<Boolean> setSpawn(@NotNull MVWorld world, @NotNull Location location);

    /**
     * Sets whether a world is the global spawn location.
     * <p>
     * When setting a world as global spawn, any other world with global spawn
     * will have its flag cleared.
     * </p>
     *
     * @param world  the world to update
     * @param global true to make this the global spawn
     * @return a CompletableFuture containing true if successful
     */
    @NotNull
    CompletableFuture<Boolean> setGlobalSpawn(@NotNull MVWorld world, boolean global);

    /**
     * Gets the global spawn world if one is configured.
     *
     * @return a CompletableFuture containing an Optional with the global spawn world
     */
    @NotNull
    CompletableFuture<Optional<MVWorld>> getGlobalSpawnWorld();

    /**
     * Gets the appropriate spawn location for a player.
     * <p>
     * Priority:
     * <ol>
     *   <li>Global spawn if configured</li>
     *   <li>Current world's spawn if managed</li>
     *   <li>Default world spawn</li>
     * </ol>
     * </p>
     *
     * @param player the player to get spawn for
     * @return a CompletableFuture containing the spawn location
     */
    @NotNull
    CompletableFuture<Location> getSpawnLocation(@NotNull Player player);

    // ==================== Validation ====================

    /**
     * Checks if a world with the given identifier already exists.
     *
     * @param identifier the world identifier to check
     * @return a CompletableFuture containing true if the world exists
     */
    @NotNull
    CompletableFuture<Boolean> worldExists(@NotNull String identifier);

    /**
     * Checks if the world limit has been reached.
     *
     * @return a CompletableFuture containing true if at limit
     */
    @NotNull
    CompletableFuture<Boolean> isAtWorldLimit();

    /**
     * Gets the current world count.
     *
     * @return a CompletableFuture containing the number of managed worlds
     */
    @NotNull
    CompletableFuture<Long> getWorldCount();
}
