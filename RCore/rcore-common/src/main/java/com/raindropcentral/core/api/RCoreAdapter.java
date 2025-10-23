package com.raindropcentral.core.api;

import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import com.raindropcentral.core.service.RCoreService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

/**
 * Thread-safe façade that exposes {@link RCoreService} through the stable
 * {@code rcore-common} surface and delegates persistence to the active
 * {@link RCoreBackend}.
 *
 * <p>The adapter ensures all asynchronous operations leverage the backend's
 * {@link java.util.concurrent.Executor Executor} so Bukkit consumers observe
 * consistent threading semantics described in the package overview.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class RCoreAdapter implements RCoreService {

    /**
     * Shared logger used to emit lifecycle and error information for backend
     * delegation calls, supporting RDQ and RPlatform diagnostics.
     */
    private static final Logger LOGGER = CentralLogger.getLogger(RCoreAdapter.class);

    /**
     * Concrete backend supplied by the runtime implementation (Free or Premium)
     * that performs datastore access and executor provisioning.
     */
    private final RCoreBackend backend;
    /**
     * Executor sourced from the backend and reused to schedule adapter composed
     * stages, guaranteeing thread affinity with persistence operations.
     */
    private final Executor executor;

    /**
     * Creates a new adapter that proxies calls to the supplied backend while
     * caching the backend executor for all asynchronous chains initiated by the
     * adapter.
     *
     * @param backend initialized backend providing persistence and executors
     * @throws NullPointerException if backend is {@code null}
     */
    public RCoreAdapter(final @NotNull RCoreBackend backend) {
        this.backend = Objects.requireNonNull(backend, "backend cannot be null");
        this.executor = backend.getExecutor();
        LOGGER.info("RCoreAdapter initialized successfully");
    }

    /**
     * Asynchronously locates a player by unique identifier using the backend
     * executor. Failures emitted by the backend are logged with a
     * {@link java.util.logging.Level#WARNING WARNING} severity before the
     * resulting {@link CompletableFuture} completes exceptionally.
     *
     * @param uniqueId player UUID to search for
     * @return future containing the player when present
     * @throws NullPointerException if {@code uniqueId} is {@code null}
     */
    @Override
    public CompletableFuture<Optional<RPlayer>> findPlayerAsync(final @NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        return backend.findByUuidAsync(uniqueId)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LOGGER.warning("Failed to retrieve player with UUID: %s - %s"
                                .formatted(uniqueId, ex.getMessage()));
                    }
                });
    }

    /**
     * Delegates to {@link #findPlayerAsync(UUID)} with the offline player's
     * unique identifier, preserving the executor guarantees and logging
     * behaviour of the primary lookup.
     *
     * @param offlinePlayer player reference whose UUID will be used
     * @return future containing the player when present
     * @throws NullPointerException if {@code offlinePlayer} is {@code null}
     */
    @Override
    public CompletableFuture<Optional<RPlayer>> findPlayerAsync(final @NotNull OfflinePlayer offlinePlayer) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer cannot be null");
        return findPlayerAsync(offlinePlayer.getUniqueId());
    }

    /**
     * Asynchronously locates a player by last known name while reusing the
     * backend executor for completion stages. Failures are logged at
     * {@link java.util.logging.Level#WARNING WARNING} prior to propagating the
     * exceptional completion to callers.
     *
     * @param playerName case-insensitive player name
     * @return future containing the player when present
     * @throws NullPointerException if {@code playerName} is {@code null}
     */
    @Override
    public CompletableFuture<Optional<RPlayer>> findPlayerByNameAsync(final @NotNull String playerName) {
        Objects.requireNonNull(playerName, "playerName cannot be null");
        return backend.findByNameAsync(playerName)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LOGGER.warning("Failed to retrieve player with name: %s - %s"
                                .formatted(playerName, ex.getMessage()));
                    }
                });
    }

    /**
     * Resolves player existence asynchronously by chaining
     * {@link #findPlayerAsync(UUID)} on the backend executor. Any failures from
     * the lookup stage will propagate to the returned future unchanged, and no
     * additional logging occurs beyond the lookup warning emitted by
     * {@link #findPlayerAsync(UUID)}.
     *
     * @param uniqueId player identifier to check
     * @return future reporting {@code true} when the player exists
     * @throws NullPointerException if {@code uniqueId} is {@code null}
     */
    @Override
    public CompletableFuture<Boolean> playerExistsAsync(final @NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        return findPlayerAsync(uniqueId)
                .thenApplyAsync(Optional::isPresent, executor);
    }

    /**
     * Convenience existence check for {@link OfflinePlayer} instances. The
     * returned future inherits the logging and exception propagation semantics
     * of {@link #playerExistsAsync(UUID)}.
     *
     * @param offlinePlayer player reference whose UUID will be used
     * @return future reporting {@code true} when the player exists
     * @throws NullPointerException if {@code offlinePlayer} is {@code null}
     */
    @Override
    public CompletableFuture<Boolean> playerExistsAsync(final @NotNull OfflinePlayer offlinePlayer) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer cannot be null");
        return playerExistsAsync(offlinePlayer.getUniqueId());
    }

    /**
     * Creates a new player asynchronously when the identifier is not already
     * registered, ensuring statistic scaffolding is initialized prior to
     * persistence. Duplicate player attempts are logged as warnings and short
     * circuit to an empty optional. Persistence failures are logged at
     * {@link java.util.logging.Level#SEVERE SEVERE} before propagating the
     * exceptional completion.
     *
     * @param uniqueId player identifier to persist
     * @param playerName display name to associate with the player
     * @return future containing the newly created player when successful;
     *     otherwise an empty optional
     * @throws NullPointerException if {@code uniqueId} or {@code playerName} is {@code null}
     */
    @Override
    public CompletableFuture<Optional<RPlayer>> createPlayerAsync(
            final @NotNull UUID uniqueId,
            final @NotNull String playerName
    ) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        Objects.requireNonNull(playerName, "playerName cannot be null");

        return playerExistsAsync(uniqueId)
                .thenComposeAsync(exists -> {
                    if (exists) {
                        LOGGER.warning("Attempted to create player that already exists: %s".formatted(uniqueId));
                        return CompletableFuture.completedFuture(Optional.<RPlayer>empty());
                    }

                    final RPlayer newPlayer = new RPlayer(uniqueId, playerName);
                    final RPlayerStatistic statistics = new RPlayerStatistic(newPlayer);
                    newPlayer.setPlayerStatistic(statistics);

                    return backend.createAsync(newPlayer)
                            .thenApply(Optional::of);
                }, executor)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LOGGER.severe("Failed to create player: %s - %s"
                                .formatted(uniqueId, ex.getMessage()));
                    }
                });
    }

    /**
     * Persists updates for the provided player via the backend executor. A
     * failed persistence call is logged at {@link java.util.logging.Level#SEVERE
     * SEVERE} before the future completes exceptionally.
     *
     * @param player player entity with applied changes
     * @return future containing the updated player when successful
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public CompletableFuture<Optional<RPlayer>> updatePlayerAsync(final @NotNull RPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");

        return backend.updateAsync(player)
                .thenApply(Optional::of)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LOGGER.severe("Failed to update player: %s - %s"
                                .formatted(player.getUniqueId(), ex.getMessage()));
                    }
                });
    }

    /**
     * Retrieves player statistics asynchronously, mapping absent players to an
     * empty optional while executing mapping operations on the backend executor
     * to maintain thread affinity.
     *
     * @param uniqueId player identifier whose statistics should be fetched
     * @return future containing player statistics when present
     * @throws NullPointerException if {@code uniqueId} is {@code null}
     */
    @Override
    public CompletableFuture<Optional<RPlayerStatistic>> findPlayerStatisticsAsync(final @NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        return findPlayerAsync(uniqueId).thenApplyAsync(playerOpt -> playerOpt.map(RPlayer::getPlayerStatistic), executor);
    }

    /**
     * Resolves statistics for an offline player using the player's UUID while
     * preserving the executor usage and optional semantics of
     * {@link #findPlayerStatisticsAsync(UUID)}.
     *
     * @param offlinePlayer offline reference whose UUID will be used
     * @return future containing player statistics when present
     * @throws NullPointerException if {@code offlinePlayer} is {@code null}
     */
    @Override
    public CompletableFuture<Optional<RPlayerStatistic>> findPlayerStatisticsAsync(
            final @NotNull OfflinePlayer offlinePlayer
    ) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer cannot be null");
        return findPlayerStatisticsAsync(offlinePlayer.getUniqueId());
    }

    /**
     * Asynchronously locates a statistic value for the specified identifier and
     * plugin, retaining backend executor affinity. The resulting future
     * completes exceptionally if the underlying player lookup fails and no
     * additional logging is performed beyond the lookup warnings.
     *
     * @param uniqueId player identifier to inspect
     * @param identifier statistic key
     * @param plugin plugin namespace owning the statistic
     * @return future containing the statistic value when present
     * @throws NullPointerException if any argument is {@code null}
     */
    @Override
    public CompletableFuture<Optional<Object>> findStatisticValueAsync(
            final @NotNull UUID uniqueId,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");

        return findPlayerStatisticsAsync(uniqueId)
                .thenApplyAsync(statsOpt -> statsOpt
                        .flatMap(stats -> stats.getStatisticValue(identifier, plugin)), executor);
    }

    /**
     * Convenience overload that resolves the UUID from an offline player before
     * forwarding to {@link #findStatisticValueAsync(UUID, String, String)},
     * inheriting the executor usage and failure propagation described there.
     *
     * @param offlinePlayer player reference whose UUID will be used
     * @param identifier statistic key
     * @param plugin plugin namespace owning the statistic
     * @return future containing the statistic value when present
     * @throws NullPointerException if any argument is {@code null}
     */
    @Override
    public CompletableFuture<Optional<Object>> findStatisticValueAsync(
            final @NotNull OfflinePlayer offlinePlayer,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer cannot be null");
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");
        return findStatisticValueAsync(offlinePlayer.getUniqueId(), identifier, plugin);
    }

    /**
     * Determines asynchronously whether a statistic exists for the supplied
     * identifier within the player statistics snapshot. Failures while fetching
     * player data propagate to the returned future without additional logging
     * beyond the lookup warnings.
     *
     * @param uniqueId player identifier to inspect
     * @param identifier statistic key
     * @param plugin plugin namespace owning the statistic
     * @return future reporting {@code true} when the statistic exists
     * @throws NullPointerException if any argument is {@code null}
     */
    @Override
    public CompletableFuture<Boolean> hasStatisticAsync(
            final @NotNull UUID uniqueId,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");

        return findPlayerStatisticsAsync(uniqueId)
                .thenApplyAsync(statsOpt -> statsOpt
                        .map(stats -> stats.hasStatistic(identifier, plugin))
                        .orElse(false), executor);
    }

    /**
     * Convenience overload that delegates to
     * {@link #hasStatisticAsync(UUID, String, String)} using the player's UUID,
     * preserving the executor usage and failure propagation semantics of the
     * primary overload.
     *
     * @param offlinePlayer player reference whose UUID will be used
     * @param identifier statistic key
     * @param plugin plugin namespace owning the statistic
     * @return future reporting {@code true} when the statistic exists
     * @throws NullPointerException if any argument is {@code null}
     */
    @Override
    public CompletableFuture<Boolean> hasStatisticAsync(
            final @NotNull OfflinePlayer offlinePlayer,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer cannot be null");
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");
        return hasStatisticAsync(offlinePlayer.getUniqueId(), identifier, plugin);
    }

    /**
     * Removes a statistic from the player snapshot and persists the change via
     * the backend executor. When persistence fails the error is logged at
     * {@link java.util.logging.Level#SEVERE SEVERE} prior to propagating the
     * exceptional completion.
     *
     * @param uniqueId player identifier whose statistic should be removed
     * @param identifier statistic key
     * @param plugin plugin namespace owning the statistic
     * @return future reporting {@code true} when removal succeeded
     * @throws NullPointerException if any argument is {@code null}
     */
    @Override
    public CompletableFuture<Boolean> removeStatisticAsync(
            final @NotNull UUID uniqueId,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");

        return findPlayerAsync(uniqueId)
                .thenComposeAsync(playerOpt -> {
                    if (playerOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(false);
                    }

                    final RPlayer player = playerOpt.get();
                    final RPlayerStatistic stats = player.getPlayerStatistic();

                    if (stats == null || !stats.removeStatistic(identifier, plugin)) {
                        return CompletableFuture.completedFuture(false);
                    }

                    return backend.updateAsync(player).thenApply(p -> true);
                }, executor)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LOGGER.severe("Failed to persist statistic removal for player: %s - %s"
                                .formatted(uniqueId, ex.getMessage()));
                    }
                });
    }

    /**
     * Convenience overload that delegates to
     * {@link #removeStatisticAsync(UUID, String, String)} using the player's
     * UUID, inheriting the executor usage and severe error logging of the
     * primary overload.
     *
     * @param offlinePlayer player reference whose UUID will be used
     * @param identifier statistic key
     * @param plugin plugin namespace owning the statistic
     * @return future reporting {@code true} when removal succeeded
     * @throws NullPointerException if any argument is {@code null}
     */
    @Override
    public CompletableFuture<Boolean> removeStatisticAsync(
            final @NotNull OfflinePlayer offlinePlayer,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer cannot be null");
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");
        return removeStatisticAsync(offlinePlayer.getUniqueId(), identifier, plugin);
    }

    /**
     * Adds or replaces a statistic for the supplied player and persists the
     * updated aggregate through the backend executor. When the player is
     * missing a warning is emitted and a completed future of {@code false} is
     * returned. Persistence failures are logged at
     * {@link java.util.logging.Level#SEVERE SEVERE} before propagating the
     * exceptional completion.
     *
     * @param uniqueId player identifier to update
     * @param statistic statistic instance to add or replace
     * @return future reporting {@code true} when persistence succeeds
     * @throws NullPointerException if any argument is {@code null}
     */
    @Override
    public CompletableFuture<Boolean> addOrReplaceStatisticAsync(
            final @NotNull UUID uniqueId,
            final @NotNull RAbstractStatistic statistic
    ) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        Objects.requireNonNull(statistic, "statistic cannot be null");

        return findPlayerAsync(uniqueId)
                .thenComposeAsync(playerOpt -> {
                    if (playerOpt.isEmpty()) {
                        LOGGER.warning("Attempted to addOrReplaceStatistic for non-existent player: %s"
                                .formatted(uniqueId));
                        return CompletableFuture.completedFuture(false);
                    }

                    final RPlayer player = playerOpt.get();
                    RPlayerStatistic stats = player.getPlayerStatistic();

                    if (stats == null) {
                        stats = new RPlayerStatistic(player);
                        player.setPlayerStatistic(stats);
                    }

                    stats.addOrReplaceStatistic(statistic);

                    return backend.updateAsync(player).thenApply(p -> true);
                }, executor)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LOGGER.severe("Failed to persist addOrReplaceStatistic for player: %s - %s"
                                .formatted(uniqueId, ex.getMessage()));
                    }
                });
    }

    /**
     * Convenience overload that delegates to
     * {@link #addOrReplaceStatisticAsync(UUID, RAbstractStatistic)} using the
     * player's UUID, inheriting the executor usage and logging semantics of the
     * primary overload.
     *
     * @param offlinePlayer player reference whose UUID will be used
     * @param statistic statistic instance to add or replace
     * @return future reporting {@code true} when persistence succeeds
     * @throws NullPointerException if any argument is {@code null}
     */
    @Override
    public CompletableFuture<Boolean> addOrReplaceStatisticAsync(
            final @NotNull OfflinePlayer offlinePlayer,
            final @NotNull RAbstractStatistic statistic
    ) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer cannot be null");
        Objects.requireNonNull(statistic, "statistic cannot be null");
        return addOrReplaceStatisticAsync(offlinePlayer.getUniqueId(), statistic);
    }

    /**
     * Determines asynchronously how many statistics the specified plugin owns
     * for the player. Mapping occurs on the backend executor and failures in
     * the lookup stage propagate to the returned future without additional
     * logging beyond the player lookup warnings.
     *
     * @param uniqueId player identifier to inspect
     * @param plugin plugin namespace owning the statistics
     * @return future containing the count of statistics for the plugin
     * @throws NullPointerException if any argument is {@code null}
     */
    @Override
    public CompletableFuture<Long> getStatisticCountForPluginAsync(
            final @NotNull UUID uniqueId,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");

        return findPlayerStatisticsAsync(uniqueId)
                .thenApplyAsync(statsOpt -> statsOpt
                        .map(stats -> stats.getStatisticCountForPlugin(plugin))
                        .orElse(0L), executor);
    }

    /**
     * Convenience overload that delegates to
     * {@link #getStatisticCountForPluginAsync(UUID, String)} using the player's
     * UUID, inheriting the executor usage and failure propagation semantics of
     * the primary overload.
     *
     * @param offlinePlayer player reference whose UUID will be used
     * @param plugin plugin namespace owning the statistics
     * @return future containing the count of statistics for the plugin
     * @throws NullPointerException if any argument is {@code null}
     */
    @Override
    public CompletableFuture<Long> getStatisticCountForPluginAsync(
            final @NotNull OfflinePlayer offlinePlayer,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");
        return getStatisticCountForPluginAsync(offlinePlayer.getUniqueId(), plugin);
    }

    /**
     * Reports the stable API version exposed by this adapter.
     *
     * @return semantic version string advertised to consumers
     */
    @Override
    public @NotNull String getApiVersion() {
        return "2.0.0";
    }
}
