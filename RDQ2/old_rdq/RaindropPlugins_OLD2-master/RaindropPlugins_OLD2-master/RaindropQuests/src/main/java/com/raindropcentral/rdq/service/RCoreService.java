package com.raindropcentral.rdq.service;

import com.raindropcentral.rcore.api.RCoreAdapter;
import com.raindropcentral.rcore.database.entity.RPlayer;
import com.raindropcentral.rcore.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.rcore.database.entity.statistic.RPlayerStatistic;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logger.CentralLogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class for managing RCore API operations, such as player data retrieval,
 * statistics management, and player existence checks.
 * <p>
 * Provides utility methods for:
 * <ul>
 *     <li>Fetching RPlayer entities by UUID, OfflinePlayer, or name</li>
 *     <li>Checking player existence in the RCore system</li>
 *     <li>Retrieving player statistics</li>
 *     <li>Creating and updating player records</li>
 *     <li>Asynchronous operations for non-blocking data access</li>
 * </ul>
 * <p>
 * This service wraps the {@link RCoreAdapter} to provide a consistent API
 * similar to other platform services like {@link com.raindropcentral.rplatform.api.luckperms.LuckPermsService}.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public class RCoreService {

    private static final Logger LOGGER = CentralLogger.getLogger(RCoreService.class.getName());

    private final RPlatform platform;
    private final RCoreAdapter rCoreAdapter;

    /**
     * Constructs a new RCoreService.
     *
     * @param platform     The RPlatform instance for accessing platform services
     * @param rCoreAdapter The RCoreAdapter instance for accessing RCore data
     */
    public RCoreService(
            final @NotNull RPlatform platform,
            final @NotNull RCoreAdapter rCoreAdapter
    ) {
        this.platform = platform;
        this.rCoreAdapter = rCoreAdapter;
        LOGGER.info("RCoreService initialized successfully");
    }

    /**
     * Retrieves the RPlayer for the given UUID asynchronously.
     *
     * @param uuid The UUID of the player
     * @return A CompletableFuture containing the RPlayer, or null if not found
     */
    @NotNull
    public CompletableFuture<@Nullable RPlayer> getPlayerAsync(final @NotNull UUID uuid) {
        return this.rCoreAdapter.getPlayer(uuid)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to retrieve player with UUID: " + uuid, throwable);
                    return null;
                });
    }

    /**
     * Retrieves the RPlayer for the given OfflinePlayer asynchronously.
     *
     * @param offlinePlayer The OfflinePlayer
     * @return A CompletableFuture containing the RPlayer, or null if not found
     */
    @NotNull
    public CompletableFuture<@Nullable RPlayer> getPlayerAsync(final @NotNull OfflinePlayer offlinePlayer) {
        return this.rCoreAdapter.getPlayer(offlinePlayer)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING,
                            "Failed to retrieve player with UUID: " + offlinePlayer.getUniqueId(), throwable);
                    return null;
                });
    }

    /**
     * Retrieves the RPlayer for the given online Player asynchronously.
     *
     * @param player The online Player
     * @return A CompletableFuture containing the RPlayer, or null if not found
     */
    @NotNull
    public CompletableFuture<@Nullable RPlayer> getPlayerAsync(final @NotNull Player player) {
        return getPlayerAsync(player.getUniqueId());
    }

    /**
     * Retrieves the RPlayer for the given player name asynchronously.
     *
     * @param playerName The name of the player
     * @return A CompletableFuture containing the RPlayer, or null if not found
     */
    @NotNull
    public CompletableFuture<@Nullable RPlayer> getPlayerByNameAsync(final @NotNull String playerName) {
        return this.rCoreAdapter.getPlayerByName(playerName)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to retrieve player with name: " + playerName, throwable);
                    return null;
                });
    }

    /**
     * Checks if a player exists in the RCore system asynchronously.
     *
     * @param uuid The UUID of the player
     * @return A CompletableFuture containing true if the player exists, false otherwise
     */
    @NotNull
    public CompletableFuture<Boolean> hasPlayerAsync(final @NotNull UUID uuid) {
        return this.rCoreAdapter.hasPlayer(uuid)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to check player existence for UUID: " + uuid, throwable);
                    return false;
                });
    }

    /**
     * Checks if a player exists in the RCore system asynchronously.
     *
     * @param offlinePlayer The OfflinePlayer
     * @return A CompletableFuture containing true if the player exists, false otherwise
     */
    @NotNull
    public CompletableFuture<Boolean> hasPlayerAsync(final @NotNull OfflinePlayer offlinePlayer) {
        return this.rCoreAdapter.hasPlayer(offlinePlayer)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING,
                            "Failed to check player existence for UUID: " + offlinePlayer.getUniqueId(), throwable);
                    return false;
                });
    }

    /**
     * Retrieves the statistics for a player asynchronously.
     *
     * @param uuid The UUID of the player
     * @return A CompletableFuture containing the RPlayerStatistic, or null if not found
     */
    @NotNull
    public CompletableFuture<@Nullable RPlayerStatistic> getPlayerStatisticsAsync(final @NotNull UUID uuid) {
        return this.rCoreAdapter.getPlayerStatistics(uuid)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING,
                            "Failed to retrieve player statistics for UUID: " + uuid, throwable);
                    return null;
                });
    }

    /**
     * Retrieves the statistics for a player asynchronously.
     *
     * @param offlinePlayer The OfflinePlayer
     * @return A CompletableFuture containing the RPlayerStatistic, or null if not found
     */
    @NotNull
    public CompletableFuture<@Nullable RPlayerStatistic> getPlayerStatisticsAsync(
            final @NotNull OfflinePlayer offlinePlayer
    ) {
        return this.rCoreAdapter.getPlayerStatistics(offlinePlayer)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING,
                            "Failed to retrieve player statistics for UUID: " + offlinePlayer.getUniqueId(), throwable);
                    return null;
                });
    }

    /**
     * Retrieves the statistics for an online player asynchronously.
     *
     * @param player The online Player
     * @return A CompletableFuture containing the RPlayerStatistic, or null if not found
     */
    @NotNull
    public CompletableFuture<@Nullable RPlayerStatistic> getPlayerStatisticsAsync(final @NotNull Player player) {
        return getPlayerStatisticsAsync(player.getUniqueId());
    }

    /**
     * Creates a new player in the RCore system asynchronously.
     * <p>
     * <strong>Warning:</strong> This should typically not be called directly as RCore's
     * OnJoin listener handles player creation automatically. Use this only for special cases.
     * </p>
     *
     * @param uuid       The UUID of the player
     * @param playerName The name of the player
     * @return A CompletableFuture containing the created RPlayer, or null if creation failed
     */
    @NotNull
    public CompletableFuture<@Nullable RPlayer> createPlayerAsync(
            final @NotNull UUID uuid,
            final @NotNull String playerName
    ) {
        return this.rCoreAdapter.createPlayer(uuid, playerName)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE,
                            "Failed to create player with UUID: " + uuid + ", name: " + playerName, throwable);
                    return null;
                });
    }

    /**
     * Updates an existing player in the RCore system asynchronously.
     *
     * @param rPlayer The RPlayer to update
     * @return A CompletableFuture containing the updated RPlayer, or null if update failed
     */
    @NotNull
    public CompletableFuture<@Nullable RPlayer> updatePlayerAsync(final @NotNull RPlayer rPlayer) {
        return this.rCoreAdapter.updatePlayer(rPlayer)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE,
                            "Failed to update player with UUID: " + rPlayer.getUniqueId(), throwable);
                    return null;
                });
    }

    // ============================
    // Statistic helpers (identifier + plugin) - Async wrappers
    // ============================

    /**
     * Gets a specific statistic value for a player by identifier and plugin asynchronously.
     *
     * @param uuid       The player's UUID
     * @param identifier The statistic identifier
     * @param plugin     The plugin name that owns the statistic
     * @return A future completing with the statistic value, or null if not found
     */
    @NotNull
    public CompletableFuture<@Nullable Object> getStatisticValueAsync(
            final @NotNull UUID uuid,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        return this.rCoreAdapter.getStatisticValue(uuid, identifier, plugin)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING,
                            "Failed to get statistic value for UUID: " + uuid +
                                    ", identifier: " + identifier + ", plugin: " + plugin, throwable);
                    return null;
                });
    }

    /**
     * Convenience overload for OfflinePlayer.
     */
    @NotNull
    public CompletableFuture<@Nullable Object> getStatisticValueAsync(
            final @NotNull OfflinePlayer offlinePlayer,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        return this.rCoreAdapter.getStatisticValue(offlinePlayer, identifier, plugin)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING,
                            "Failed to get statistic value for UUID: " + offlinePlayer.getUniqueId() +
                                    ", identifier: " + identifier + ", plugin: " + plugin, throwable);
                    return null;
                });
    }

    /**
     * Convenience overload for Player.
     */
    @NotNull
    public CompletableFuture<@Nullable Object> getStatisticValueAsync(
            final @NotNull Player player,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        return getStatisticValueAsync(player.getUniqueId(), identifier, plugin);
    }

    /**
     * Checks if a player has a statistic by identifier and plugin asynchronously.
     *
     * @param uuid       The player's UUID
     * @param identifier The statistic identifier
     * @param plugin     The plugin name that owns the statistic
     * @return A future completing with true if present, false otherwise
     */
    @NotNull
    public CompletableFuture<Boolean> hasStatisticAsync(
            final @NotNull UUID uuid,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        return this.rCoreAdapter.hasStatistic(uuid, identifier, plugin)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING,
                            "Failed to check statistic presence for UUID: " + uuid +
                                    ", identifier: " + identifier + ", plugin: " + plugin, throwable);
                    return false;
                });
    }

    /**
     * Convenience overload for OfflinePlayer.
     */
    @NotNull
    public CompletableFuture<Boolean> hasStatisticAsync(
            final @NotNull OfflinePlayer offlinePlayer,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        return this.rCoreAdapter.hasStatistic(offlinePlayer, identifier, plugin)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING,
                            "Failed to check statistic presence for UUID: " + offlinePlayer.getUniqueId() +
                                    ", identifier: " + identifier + ", plugin: " + plugin, throwable);
                    return false;
                });
    }

    /**
     * Convenience overload for Player.
     */
    @NotNull
    public CompletableFuture<Boolean> hasStatisticAsync(
            final @NotNull Player player,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        return hasStatisticAsync(player.getUniqueId(), identifier, plugin);
    }

    /**
     * Removes a statistic by identifier and plugin and persists the change asynchronously.
     *
     * @param uuid       The player's UUID
     * @param identifier The statistic identifier
     * @param plugin     The plugin name that owns the statistic
     * @return A future completing with true if a statistic was removed and saved, false otherwise
     */
    @NotNull
    public CompletableFuture<Boolean> removeStatisticAsync(
            final @NotNull UUID uuid,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        return this.rCoreAdapter.removeStatistic(uuid, identifier, plugin)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE,
                            "Failed to remove statistic for UUID: " + uuid +
                                    ", identifier: " + identifier + ", plugin: " + plugin, throwable);
                    return false;
                });
    }

    /**
     * Convenience overload for OfflinePlayer.
     */
    @NotNull
    public CompletableFuture<Boolean> removeStatisticAsync(
            final @NotNull OfflinePlayer offlinePlayer,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        return this.rCoreAdapter.removeStatistic(offlinePlayer, identifier, plugin)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE,
                            "Failed to remove statistic for UUID: " + offlinePlayer.getUniqueId() +
                                    ", identifier: " + identifier + ", plugin: " + plugin, throwable);
                    return false;
                });
    }

    /**
     * Convenience overload for Player.
     */
    @NotNull
    public CompletableFuture<Boolean> removeStatisticAsync(
            final @NotNull Player player,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        return removeStatisticAsync(player.getUniqueId(), identifier, plugin);
    }

    /**
     * Adds or replaces a statistic for a player and persists the change asynchronously.
     *
     * @param uuid      The player's UUID
     * @param statistic The statistic instance to add or replace
     * @return A future completing with true if persisted successfully, false otherwise
     */
    @NotNull
    public CompletableFuture<Boolean> addOrReplaceStatisticAsync(
            final @NotNull UUID uuid,
            final @NotNull RAbstractStatistic statistic
    ) {
        return this.rCoreAdapter.addOrReplaceStatistic(uuid, statistic)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE,
                            "Failed to addOrReplace statistic for UUID: " + uuid +
                                    ", identifier: " + statistic.getIdentifier() +
                                    ", plugin: " + statistic.getPlugin(), throwable);
                    return false;
                });
    }

    /**
     * Convenience overload for OfflinePlayer.
     */
    @NotNull
    public CompletableFuture<Boolean> addOrReplaceStatisticAsync(
            final @NotNull OfflinePlayer offlinePlayer,
            final @NotNull RAbstractStatistic statistic
    ) {
        return this.rCoreAdapter.addOrReplaceStatistic(offlinePlayer, statistic)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE,
                            "Failed to addOrReplace statistic for UUID: " + offlinePlayer.getUniqueId() +
                                    ", identifier: " + statistic.getIdentifier() +
                                    ", plugin: " + statistic.getPlugin(), throwable);
                    return false;
                });
    }

    /**
     * Convenience overload for Player.
     */
    @NotNull
    public CompletableFuture<Boolean> addOrReplaceStatisticAsync(
            final @NotNull Player player,
            final @NotNull RAbstractStatistic statistic
    ) {
        return addOrReplaceStatisticAsync(player.getUniqueId(), statistic);
    }

    /**
     * Gets the number of statistics a player has for a given plugin asynchronously.
     *
     * @param uuid   The player's UUID
     * @param plugin The plugin namespace
     * @return A future completing with the count, or 0 if not found
     */
    @NotNull
    public CompletableFuture<Long> getStatisticCountForPluginAsync(
            final @NotNull UUID uuid,
            final @NotNull String plugin
    ) {
        return this.rCoreAdapter.getStatisticCountForPlugin(uuid, plugin)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING,
                            "Failed to get statistic count for UUID: " + uuid + ", plugin: " + plugin, throwable);
                    return 0L;
                });
    }

    /**
     * Convenience overload for OfflinePlayer.
     */
    @NotNull
    public CompletableFuture<Long> getStatisticCountForPluginAsync(
            final @NotNull OfflinePlayer offlinePlayer,
            final @NotNull String plugin
    ) {
        return this.rCoreAdapter.getStatisticCountForPlugin(offlinePlayer, plugin)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING,
                            "Failed to get statistic count for UUID: " + offlinePlayer.getUniqueId() +
                                    ", plugin: " + plugin, throwable);
                    return 0L;
                });
    }

    /**
     * Convenience overload for Player.
     */
    @NotNull
    public CompletableFuture<Long> getStatisticCountForPluginAsync(
            final @NotNull Player player,
            final @NotNull String plugin
    ) {
        return getStatisticCountForPluginAsync(player.getUniqueId(), plugin);
    }

    /**
     * Gets the underlying RCoreAdapter instance.
     * <p>
     * Use this for advanced operations not covered by the service methods.
     * </p>
     *
     * @return The RCoreAdapter instance
     */
    @NotNull
    public RCoreAdapter getAdapter() {
        return this.rCoreAdapter;
    }

    /**
     * Gets the API version of the RCore adapter.
     *
     * @return The API version string
     */
    @NotNull
    public String getApiVersion() {
        return this.rCoreAdapter.getApiVersion();
    }

    /**
     * Gets the RPlatform instance.
     *
     * @return The RPlatform instance
     */
    @NotNull
    public RPlatform getPlatform() {
        return this.platform;
    }
}