package com.raindropcentral.core.api;

import com.raindropcentral.core.database.entity.player.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Cross-module abstraction implemented by Free and Premium runtime variants to
 * decouple {@link RCoreAdapter} from storage or executor specifics.
 *
 * <p>Implementations must provide a dedicated {@link java.util.concurrent.ExecutorService}
 * owned by the backend to satisfy the threading guarantees promised in this package's overview.
 * The executor must not be {@code null} and must remain stable for the lifetime of the backend so
 * that asynchronous pipelines composed by the adapter execute predictably.</p>
 *
 * <p>All public entry points in this contract expect upstream callers to guard against {@code null}
 * input. Implementations should eagerly apply {@link java.util.Objects#requireNonNull(Object)} to
 * enforce that invariant before scheduling asynchronous work.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public interface RCoreBackend {

    /**
     * Supplies the executor used for every asynchronous stage composed by the
     * adapter, typically tied to the backend's persistence infrastructure.
     * Implementations own the lifecycle of this executor and must ensure it is
     * threadsafe, non-blocking for critical server operations, and non-null.
     *
     * @return executor for adapter and backend asynchronous work
     */
    @NotNull ExecutorService getExecutor();

    /**
     * Asynchronously resolves a player using the provided unique identifier.
     * The lookup is scheduled on {@link #getExecutor()} and must handle null
     * identifiers defensively prior to dispatching asynchronous tasks.
     *
     * @param uniqueId player UUID to search for
     * @return future containing the player when present, completed on the backend executor
     * @throws NullPointerException if {@code uniqueId} is {@code null}
     */
    @NotNull CompletableFuture<Optional<RPlayer>> findByUuidAsync(@NotNull UUID uniqueId);

    /**
     * Asynchronously resolves a player by last known name. The query must be
     * dispatched using {@link #getExecutor()} and implementations should validate
     * the name before scheduling the asynchronous computation.
     *
     * @param playerName case-insensitive player name
     * @return future containing the player when present, completed on the backend executor
     * @throws NullPointerException if {@code playerName} is {@code null}
     */
    @NotNull CompletableFuture<Optional<RPlayer>> findByNameAsync(@NotNull String playerName);

    /**
     * Persists a new player entity and returns the saved instance. The creation
     * process is executed asynchronously using {@link #getExecutor()} and relies
     * on null checks before deferring work to the persistence layer.
     *
     * @param player player aggregate to create
     * @return future containing the persisted player entity, completed on the backend executor
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @NotNull CompletableFuture<RPlayer> createAsync(@NotNull RPlayer player);

    /**
     * Persists updates for an existing player and returns the refreshed entity.
     * The update operation runs asynchronously on {@link #getExecutor()} with
     * null guards executed prior to dispatching persistence logic.
     *
     * @param player player aggregate to update
     * @return future containing the updated player entity, completed on the backend executor
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @NotNull CompletableFuture<RPlayer> updateAsync(@NotNull RPlayer player);
}