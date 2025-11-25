package com.raindropcentral.core.service;

import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public contract that exposes asynchronous player and statistic operations backed by the RCore
 * persistence layer.
 *
 * <p>Implementations execute work on the executor provided by the active {@code RCoreBackend} and
 * must avoid the Bukkit primary thread except when delivering already-computed results. The
 * service is designed for concurrent access and every {@link CompletableFuture} completes on the
 * backend executor so downstream modules can depend on stable threading semantics. Consumers such
 * as RDQ and RPlatform should expect that each asynchronous chain may complete exceptionally with
 * a {@link java.util.concurrent.CompletionException} when transport layers, persistence engines, or
 * cross-module bridges fail; callers are responsible for wiring defensive handling.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public interface RCoreService {

    /**
     * Locates a persisted player profile using its unique identifier.
     *
     * <p>The lookup is performed asynchronously on the backend executor and completes with an
     * {@link Optional#empty()} when no profile exists. See {@link #findPlayerAsync(OfflinePlayer)}
     * for resolving profiles via Bukkit handles. Downstream consumers should anticipate exceptional
     * completions mirroring persistence failures and handle them through
     * {@link CompletableFuture#exceptionally(java.util.function.Function)} or equivalent stages.</p>
     *
     * @param uniqueId the non-null unique identifier whose profile should be resolved
     * @return a future that completes with an optional player profile, never {@code null}
     * @throws NullPointerException if {@code uniqueId} is {@code null}
     */
    CompletableFuture<Optional<RPlayer>> findPlayerAsync(@NotNull UUID uniqueId);

    /**
     * Resolves a player profile via an {@link OfflinePlayer} reference.
     *
     * <p>This overload translates the Bukkit handle into the underlying {@link UUID} before
     * delegating to {@link #findPlayerAsync(UUID)}. The future completes on the backend executor and
     * yields {@link Optional#empty()} when the player is unknown. Cross-module consumers (for
     * example RDQ session sync) should be prepared for exceptional completions triggered by
     * transient lookup failures.</p>
     *
     * @param offlinePlayer the non-null Bukkit handle referencing the player
     * @return a future containing an optional profile, never {@code null}
     * @throws NullPointerException if {@code offlinePlayer} is {@code null}
     */
    CompletableFuture<Optional<RPlayer>> findPlayerAsync(@NotNull OfflinePlayer offlinePlayer);

    /**
     * Searches for a persisted player profile by the last known player name.
     *
     * <p>The operation runs asynchronously on the backend executor and completes with
     * {@link Optional#empty()} if no profile currently uses the provided name. RDQ name resolution
     * flows typically fan out from this method, so callers must harden for
     * {@link CompletableFuture} exceptional completions before composing platform events.</p>
     *
     * @param playerName the non-null, case-insensitive profile name to resolve
     * @return a future that yields an optional player profile, never {@code null}
     * @throws NullPointerException if {@code playerName} is {@code null}
     */
    CompletableFuture<Optional<RPlayer>> findPlayerByNameAsync(@NotNull String playerName);

    /**
     * Determines whether a player profile exists for the supplied unique identifier.
     *
     * <p>The existence check executes asynchronously on the backend executor. See
     * {@link #playerExistsAsync(OfflinePlayer)} for resolving via Bukkit handles. Futures may
     * complete exceptionally when backing repositories are unavailable; callers should fold in
     * fallback logic for delayed RDQ or RPlatform flows.</p>
     *
     * @param uniqueId the non-null unique identifier to check
     * @return a future that completes with {@code true} if the profile exists, never {@code null}
     * @throws NullPointerException if {@code uniqueId} is {@code null}
     */
    CompletableFuture<Boolean> playerExistsAsync(@NotNull UUID uniqueId);

    /**
     * Determines whether a player profile exists for the supplied {@link OfflinePlayer}.
     *
     * <p>This overload resolves the underlying {@link UUID} and delegates to
     * {@link #playerExistsAsync(UUID)}. The future completes on the backend executor and may end
     * exceptionally when profile stores are momentarily unreachable, signalling callers to retry or
     * reschedule dependent tasks.</p>
     *
     * @param offlinePlayer the non-null Bukkit handle to inspect
     * @return a future that resolves to {@code true} if the profile exists, never {@code null}
     * @throws NullPointerException if {@code offlinePlayer} is {@code null}
     */
    CompletableFuture<Boolean> playerExistsAsync(@NotNull OfflinePlayer offlinePlayer);

    /**
     * Creates a new player profile when none exists for the supplied identifier and name.
     *
     * <p>The creation runs asynchronously on the backend executor. The future contains
     * {@link Optional#empty()} when a conflicting profile already exists or persistence fails. RDQ
     * onboarding flows should react to exceptional completions by queueing compensating actions or
     * surfacing diagnostic information.</p>
     *
     * @param uniqueId   the non-null unique identifier for the player
     * @param playerName the non-null player name to persist
     * @return a future with the created profile if successful, otherwise {@link Optional#empty()}
     * @throws NullPointerException if {@code uniqueId} or {@code playerName} is {@code null}
     */
    CompletableFuture<Optional<RPlayer>> createPlayerAsync(@NotNull UUID uniqueId, @NotNull String playerName);

    /**
     * Persists the supplied player aggregate and returns the stored state.
     *
     * <p>Implementations persist asynchronously on the backend executor. The future completes with
     * {@link Optional#empty()} when the update fails or the target player is missing. Callers should
     * watch for exceptionally completed futures signalling upstream validation or transport
     * failures.</p>
     *
     * @param player the non-null aggregate to persist
     * @return a future with the updated aggregate or {@link Optional#empty()} on failure
     * @throws NullPointerException if {@code player} is {@code null}
     */
    CompletableFuture<Optional<RPlayer>> updatePlayerAsync(@NotNull RPlayer player);

    /**
     * Retrieves the aggregated statistics bundle for the supplied unique identifier.
     *
     * <p>The lookup executes asynchronously on the backend executor and yields
     * {@link Optional#empty()} when the player or statistics record is absent. For Bukkit handles
     * see {@link #findPlayerStatisticsAsync(OfflinePlayer)}. Consumers such as RDQ scoreboard
     * loaders must treat exceptional completions as fatal fetch attempts and gate UI updates
     * accordingly.</p>
     *
     * @param uniqueId the non-null player identifier to inspect
     * @return a future with the optional statistics bundle, never {@code null}
     * @throws NullPointerException if {@code uniqueId} is {@code null}
     */
    CompletableFuture<Optional<RPlayerStatistic>> findPlayerStatisticsAsync(@NotNull UUID uniqueId);

    /**
     * Retrieves aggregated statistics via an {@link OfflinePlayer} reference.
     *
     * <p>This overload resolves the {@link UUID} and delegates to
     * {@link #findPlayerStatisticsAsync(UUID)} while preserving asynchronous execution on the
     * backend executor. Cross-module integrations should expect exceptional completions for
     * transient datastore outages and defer dependent scoreboard updates.</p>
     *
     * @param offlinePlayer the non-null Bukkit handle for the player
     * @return a future yielding the optional statistics bundle, never {@code null}
     * @throws NullPointerException if {@code offlinePlayer} is {@code null}
     */
    CompletableFuture<Optional<RPlayerStatistic>> findPlayerStatisticsAsync(@NotNull OfflinePlayer offlinePlayer);

    /**
     * Locates a single statistic value for a player identified by {@link UUID}.
     *
     * <p>The result is delivered asynchronously on the backend executor and contains
     * {@link Optional#empty()} when the statistic is missing. Futures may complete exceptionally
     * when the persistence layer signals unexpected issues; RDQ milestone processing should capture
     * and log such failures for operational visibility.</p>
     *
     * @param uniqueId   the non-null identifier whose statistic should be fetched
     * @param identifier the non-null statistic key
     * @param plugin     the non-null plugin namespace that owns the statistic
     * @return a future containing an optional statistic payload, never {@code null}
     * @throws NullPointerException if any parameter is {@code null}
     */
    CompletableFuture<Optional<Object>> findStatisticValueAsync(
        @NotNull UUID uniqueId,
        @NotNull String identifier,
        @NotNull String plugin
    );

    /**
     * Locates a single statistic value for a player referenced by {@link OfflinePlayer}.
     *
     * <p>This overload delegates to {@link #findStatisticValueAsync(UUID, String, String)} after
     * resolving the player identifier and completes on the backend executor. Callers should expect
     * exceptional completions when backing services timeout, ensuring cross-module workflows remain
     * resilient.</p>
     *
     * @param offlinePlayer the non-null Bukkit handle for the player
     * @param identifier    the non-null statistic key
     * @param plugin        the non-null plugin namespace that owns the statistic
     * @return a future containing an optional statistic payload, never {@code null}
     * @throws NullPointerException if any parameter is {@code null}
     */
    CompletableFuture<Optional<Object>> findStatisticValueAsync(
        @NotNull OfflinePlayer offlinePlayer,
        @NotNull String identifier,
        @NotNull String plugin
    );

    /**
     * Determines whether the supplied player possesses the requested statistic entry.
     *
     * <p>The check runs asynchronously on the backend executor. See
     * {@link #hasStatisticAsync(OfflinePlayer, String, String)} for the Bukkit overload. Exceptional
     * completions bubble out of repository-level failures and should be handled to prevent RDQ
     * achievements from deadlocking.</p>
     *
     * @param uniqueId   the non-null player identifier to inspect
     * @param identifier the non-null statistic key
     * @param plugin     the non-null plugin namespace that owns the statistic
     * @return a future resolving to {@code true} if the statistic exists, never {@code null}
     * @throws NullPointerException if any parameter is {@code null}
     */
    CompletableFuture<Boolean> hasStatisticAsync(
        @NotNull UUID uniqueId,
        @NotNull String identifier,
        @NotNull String plugin
    );

    /**
     * Determines whether the supplied player possesses the requested statistic entry.
     *
     * <p>This overload resolves the player's identifier and delegates to
     * {@link #hasStatisticAsync(UUID, String, String)} while maintaining asynchronous execution on
     * the backend executor. Failure signals propagate via exceptional completions; integrate retry
     * policies where appropriate.</p>
     *
     * @param offlinePlayer the non-null Bukkit handle for the player
     * @param identifier    the non-null statistic key
     * @param plugin        the non-null plugin namespace that owns the statistic
     * @return a future resolving to {@code true} if the statistic exists, never {@code null}
     * @throws NullPointerException if any parameter is {@code null}
     */
    CompletableFuture<Boolean> hasStatisticAsync(
        @NotNull OfflinePlayer offlinePlayer,
        @NotNull String identifier,
        @NotNull String plugin
    );

    /**
     * Removes the specified statistic for the player identified by {@link UUID}.
     *
     * <p>The removal executes asynchronously on the backend executor and completes with
     * {@code true} when the statistic was deleted. Exceptional completions indicate that the delete
     * request could not be fulfilled due to backend issues and should be surfaced to monitoring.</p>
     *
     * @param uniqueId   the non-null player identifier
     * @param identifier the non-null statistic key
     * @param plugin     the non-null plugin namespace that owns the statistic
     * @return a future resolving to {@code true} if the statistic was removed, never {@code null}
     * @throws NullPointerException if any parameter is {@code null}
     */
    CompletableFuture<Boolean> removeStatisticAsync(
        @NotNull UUID uniqueId,
        @NotNull String identifier,
        @NotNull String plugin
    );

    /**
     * Removes the specified statistic for the player referenced by {@link OfflinePlayer}.
     *
     * <p>The overload resolves the {@link UUID} and delegates to
     * {@link #removeStatisticAsync(UUID, String, String)} while preserving the asynchronous
     * execution context. Transient failures bubble out as exceptional completions and should be
     * reconciled before acknowledging deletions to dependent modules.</p>
     *
     * @param offlinePlayer the non-null Bukkit handle for the player
     * @param identifier    the non-null statistic key
     * @param plugin        the non-null plugin namespace that owns the statistic
     * @return a future resolving to {@code true} if the statistic was removed, never {@code null}
     * @throws NullPointerException if any parameter is {@code null}
     */
    CompletableFuture<Boolean> removeStatisticAsync(
        @NotNull OfflinePlayer offlinePlayer,
        @NotNull String identifier,
        @NotNull String plugin
    );

    /**
     * Adds or replaces the supplied statistic for the player identified by {@link UUID}.
     *
     * <p>The update runs asynchronously on the backend executor and reports {@code true} when the
     * statistic was successfully stored. When repositories fail, the future completes
     * exceptionally, signalling orchestrators to pause progression of dependent achievements.</p>
     *
     * @param uniqueId  the non-null player identifier whose statistic should be updated
     * @param statistic the non-null statistic payload to store
     * @return a future resolving to {@code true} if the statistic was persisted, never {@code null}
     * @throws NullPointerException if any parameter is {@code null}
     */
    CompletableFuture<Boolean> addOrReplaceStatisticAsync(
        @NotNull UUID uniqueId,
        @NotNull RAbstractStatistic statistic
    );

    /**
     * Adds or replaces the supplied statistic for the player referenced by {@link OfflinePlayer}.
     *
     * <p>This overload resolves the player's identifier and delegates to
     * {@link #addOrReplaceStatisticAsync(UUID, RAbstractStatistic)} while maintaining asynchronous
     * execution on the backend executor. Exceptional completions echo upstream storage failures and
     * should be captured by platform telemetry.</p>
     *
     * @param offlinePlayer the non-null Bukkit handle for the player
     * @param statistic     the non-null statistic payload to store
     * @return a future resolving to {@code true} if the statistic was persisted, never {@code null}
     * @throws NullPointerException if any parameter is {@code null}
     */
    CompletableFuture<Boolean> addOrReplaceStatisticAsync(
        @NotNull OfflinePlayer offlinePlayer,
        @NotNull RAbstractStatistic statistic
    );

    /**
     * Counts the number of statistics belonging to the supplied plugin namespace for the player
     * identified by {@link UUID}.
     *
     * <p>The count executes asynchronously on the backend executor. If datastore queries fail, the
     * resulting future completes exceptionally and RDQ dashboards should treat the outcome as a
     * temporary service interruption.</p>
     *
     * @param uniqueId the non-null player identifier
     * @param plugin   the non-null plugin namespace that owns the statistics
     * @return a future resolving to the number of statistics, never {@code null}
     * @throws NullPointerException if {@code uniqueId} or {@code plugin} is {@code null}
     */
    CompletableFuture<Long> getStatisticCountForPluginAsync(@NotNull UUID uniqueId, @NotNull String plugin);

    /**
     * Counts the number of statistics belonging to the supplied plugin namespace for the player
     * referenced by {@link OfflinePlayer}.
     *
     * <p>This overload resolves the player identifier and delegates to
     * {@link #getStatisticCountForPluginAsync(UUID, String)} while ensuring the asynchronous work
     * stays on the backend executor. Exceptional completions communicate unavailable storage layers
     * and should prompt deferred analytics aggregation.</p>
     *
     * @param offlinePlayer the non-null Bukkit handle for the player
     * @param plugin        the non-null plugin namespace that owns the statistics
     * @return a future resolving to the number of statistics, never {@code null}
     * @throws NullPointerException if {@code offlinePlayer} or {@code plugin} is {@code null}
     */
    CompletableFuture<Long> getStatisticCountForPluginAsync(@NotNull OfflinePlayer offlinePlayer, @NotNull String plugin);

    /**
     * Reports the semantic API version implemented by the service.
     *
     * <p>Consumers can feature-detect additions by comparing this string to documented version
     * milestones. The value is used across RDQ and RPlatform compatibility checks, so callers should
     * still consider exceptional completions when obtaining related metadata from other service
     * endpoints.</p>
     *
     * @return a non-null string representing the semantic API version
     */
    @NotNull String getApiVersion();
}
