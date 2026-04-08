package de.jexcellence.home.service;

import de.jexcellence.home.database.entity.Home;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for home operations following the IBountyService pattern from RDQ.
 * <p>
 * Provides async methods for all home CRUD operations, validation, and premium features.
 * Implementations include FreeHomeService (limited) and PremiumHomeService (full features).
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public interface IHomeService {

    // ==================== Core CRUD Operations ====================

    /**
     * Creates a new home at the specified location.
     *
     * @param playerId the UUID of the player
     * @param name     the name of the home
     * @param location the location of the home
     * @return a CompletableFuture containing the created home
     */
    @NotNull
    CompletableFuture<Home> createHome(@NotNull UUID playerId, @NotNull String name, @NotNull Location location);

    /**
     * Deletes a home by player UUID and name.
     *
     * @param playerId the UUID of the player
     * @param name     the name of the home to delete
     * @return a CompletableFuture containing true if deleted, false if not found
     */
    @NotNull
    CompletableFuture<Boolean> deleteHome(@NotNull UUID playerId, @NotNull String name);

    /**
     * Finds a home by player UUID and name.
     *
     * @param playerId the UUID of the player
     * @param name     the name of the home
     * @return a CompletableFuture containing an Optional with the home if found
     */
    @NotNull
    CompletableFuture<Optional<Home>> findHome(@NotNull UUID playerId, @NotNull String name);

    /**
     * Gets all homes belonging to a player.
     *
     * @param playerId the UUID of the player
     * @return a CompletableFuture containing the list of homes
     */
    @NotNull
    CompletableFuture<List<Home>> getPlayerHomes(@NotNull UUID playerId);

    /**
     * Updates an existing home.
     *
     * @param home the home to update
     * @return a CompletableFuture containing the updated home
     */
    @NotNull
    CompletableFuture<Home> updateHome(@NotNull Home home);

    // ==================== Teleportation ====================

    /**
     * Teleports a player to a named home.
     *
     * @param player   the player to teleport
     * @param homeName the name of the home
     * @return a CompletableFuture containing true if teleported successfully
     */
    @NotNull
    CompletableFuture<Boolean> teleportToHome(@NotNull Player player, @NotNull String homeName);

    // ==================== Validation & Limits ====================

    /**
     * Checks if a player can create a new home.
     *
     * @param player the player to check
     * @return true if the player can create a home
     */
    boolean canCreateHome(@NotNull Player player);

    /**
     * Gets the maximum number of homes allowed for a player.
     *
     * @param player the player to check
     * @return the maximum number of homes, or -1 for unlimited
     */
    int getMaxHomesForPlayer(@NotNull Player player);

    /**
     * Gets the current home count for a player.
     *
     * @param playerId the UUID of the player
     * @return a CompletableFuture containing the home count
     */
    @NotNull
    CompletableFuture<Long> getHomeCount(@NotNull UUID playerId);

    // ==================== Edition Detection ====================

    /**
     * Checks if this is the premium version.
     *
     * @return true if premium, false if free
     */
    boolean isPremium();

    // ==================== Premium Features ====================

    /**
     * Gets homes by category (premium feature).
     *
     * @param playerId the UUID of the player
     * @param category the category to filter by
     * @return a CompletableFuture containing homes in the category
     */
    @NotNull
    CompletableFuture<List<Home>> getHomesByCategory(@NotNull UUID playerId, @NotNull String category);

    /**
     * Gets favorite homes (premium feature).
     *
     * @param playerId the UUID of the player
     * @return a CompletableFuture containing favorite homes
     */
    @NotNull
    CompletableFuture<List<Home>> getFavoriteHomes(@NotNull UUID playerId);

    /**
     * Sets the category of a home (premium feature).
     *
     * @param playerId the UUID of the player
     * @param homeName the name of the home
     * @param category the new category
     * @return a CompletableFuture containing true if successful
     */
    @NotNull
    CompletableFuture<Boolean> setHomeCategory(@NotNull UUID playerId, @NotNull String homeName, @NotNull String category);

    /**
     * Toggles the favorite status of a home (premium feature).
     *
     * @param playerId the UUID of the player
     * @param homeName the name of the home
     * @return a CompletableFuture containing true if successful
     */
    @NotNull
    CompletableFuture<Boolean> toggleFavorite(@NotNull UUID playerId, @NotNull String homeName);
}
