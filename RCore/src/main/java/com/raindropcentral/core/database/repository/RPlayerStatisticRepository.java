package com.raindropcentral.core.database.repository;

import com.raindropcentral.core.database.entity.central.RCentralServer;
import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository handling {@link RPlayerStatistic} aggregates. Provides cached access keyed by the
 * surrogate identifier to support efficient statistic loading on dedicated executors.
 * <p>
 * The statistic table relies on a surrogate identifier instead of the natural UUID from the
 * player record to avoid repetitive joins when statistics are queried or updated in bulk. The
 * repository therefore exposes the surrogate key as the cache identifier so repeated lookups by
 * primary key remain hot and avoid unnecessary round-trips to the database layer.
 * </p>
 * <p>
 * Aggregated statistics are materialized lazily using the cached lookup before being rehydrated
 * into the calling {@code RPlayer} aggregate. By keeping the statistics cached with their
 * surrogate identifiers, load operations perform in constant time even as the number of tracked
 * statistic rows grows across sessions.
 * </p>
 * <p>
 * All asynchronous work is scheduled on the shared {@link ExecutorService}
 * provided by the {@link GenericCachedRepository} base class. This guarantees that database and
 * hydration tasks operate on the same bounded executor configured for the core services, avoiding
 * accidental execution on the common fork-join pool and ensuring predictable threading semantics
 * during batch statistic updates.
 * <p>
 * Callers should log cache misses, aggregate initializations, and mutation workflows with
 * {@link com.raindropcentral.rplatform.logging.CentralLogger CentralLogger}. Log at debug level
 * when a lookup cannot be served from cache (empty result) and at info level before applying
 * create/update/delete operations so statistic changes can be audited. Any asynchronous failure or
 * validation rejection must be escalated through error logs that include the statistic identifier
 * and owning player keys.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class RPlayerStatisticRepository extends GenericCachedRepository<RPlayerStatistic, Long, Long> {

    /**
     * Configures the repository with the shared executor and entity manager factory.
     *
     * @param executor             asynchronous executor shared across database operations
     * @param entityManagerFactory JPA factory responsible for creating entity managers
     */
    public RPlayerStatisticRepository(
        @NotNull ExecutorService executor,
        @NotNull EntityManagerFactory entityManagerFactory,
        @NotNull Class<RPlayerStatistic> entityClass,
        @NotNull Function<RPlayerStatistic, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds all statistics for a specific player and server.
     * <p>
     * Returns only statistics that match both the player and server criteria,
     * enabling per-server statistics tracking.
     * </p>
     *
     * @param player the player to find statistics for
     * @param server the server to filter by
     * @return CompletableFuture containing list of matching statistics
     * @throws NullPointerException if any parameter is null
     */
    public CompletableFuture<List<RPlayerStatistic>> findByPlayerAndServer(
            final @NotNull RPlayer player,
            final @NotNull RCentralServer server
    ) {
        return findListByAttributesAsync(Map.of(
                "player", player,
                "rCentralServer", server
        ));
    }

    /**
     * Finds all statistics for a specific server across all players.
     * <p>
     * Useful for server-wide statistics aggregation and analysis.
     * </p>
     *
     * @param server the server to find statistics for
     * @return CompletableFuture containing list of all statistics for the server
     * @throws NullPointerException if server is null
     */
    public CompletableFuture<List<RPlayerStatistic>> findByServer(final @NotNull RCentralServer server) {
        return findListByAttributesAsync(Map.of("rCentralServer", server));
    }

    /**
     * Finds all statistics for a specific player across all servers.
     * <p>
     * Useful for cross-server player statistics aggregation.
     * </p>
     *
     * @param player the player to find statistics for
     * @return CompletableFuture containing list of all statistics for the player
     * @throws NullPointerException if player is null
     */
    public CompletableFuture<List<RPlayerStatistic>> findByPlayer(final @NotNull RPlayer player) {
        return findListByAttributesAsync(Map.of("player", player));
    }

    /**
     * Finds statistics for a player on a specific server, or creates a new one if none exists.
     * <p>
     * This is a convenience method for ensuring a player has statistics on a server.
     * </p>
     *
     * @param player the player to find or create statistics for
     * @param server the server to associate with the statistics
     * @return CompletableFuture containing the existing or newly created statistics
     * @throws NullPointerException if any parameter is null
     */
    public CompletableFuture<RPlayerStatistic> findOrCreateByPlayerAndServer(
            final @NotNull RPlayer player,
            final @NotNull RCentralServer server
    ) {
        return findByPlayerAndServer(player, server)
                .thenApply(statistics -> {
                    if (statistics.isEmpty()) {
                        // Create new statistics for this player-server combination
                        final RPlayerStatistic newStatistic = new RPlayerStatistic(player);
                        newStatistic.setRCentralServer(server);
                        return newStatistic;
                    }
                    return statistics.get(0);
                });
    }

    /**
     * Finds a specific statistic value for a player.
     *
     * @param player the player whose statistic to retrieve
     * @param identifier the statistic identifier
     * @param plugin the plugin namespace
     * @return CompletableFuture containing optional statistic value
     * @throws NullPointerException if any parameter is null
     */
    public CompletableFuture<java.util.Optional<Object>> findStatisticValueAsync(
            final @NotNull RPlayer player,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        return findByPlayer(player)
                .thenApply(statistics -> {
                    if (statistics.isEmpty()) {
                        return java.util.Optional.empty();
                    }
                    return statistics.get(0).getStatisticValue(identifier, plugin);
                });
    }

    /**
     * Checks if a player has a specific statistic.
     *
     * @param player the player to check
     * @param identifier the statistic identifier
     * @param plugin the plugin namespace
     * @return CompletableFuture resolving to true if statistic exists
     * @throws NullPointerException if any parameter is null
     */
    public CompletableFuture<Boolean> hasStatisticAsync(
            final @NotNull RPlayer player,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        return findByPlayer(player)
                .thenApply(statistics -> {
                    if (statistics.isEmpty()) {
                        return false;
                    }
                    return statistics.get(0).hasStatistic(identifier, plugin);
                });
    }

    /**
     * Gets the count of statistics for a specific plugin.
     *
     * @param player the player whose statistics to count
     * @param plugin the plugin namespace
     * @return CompletableFuture containing the count
     * @throws NullPointerException if any parameter is null
     */
    public CompletableFuture<Long> getStatisticCountForPluginAsync(
            final @NotNull RPlayer player,
            final @NotNull String plugin
    ) {
        return findByPlayer(player)
                .thenApply(statistics -> {
                    if (statistics.isEmpty()) {
                        return 0L;
                    }
                    return statistics.get(0).getStatisticCountForPlugin(plugin);
                });
    }
}
